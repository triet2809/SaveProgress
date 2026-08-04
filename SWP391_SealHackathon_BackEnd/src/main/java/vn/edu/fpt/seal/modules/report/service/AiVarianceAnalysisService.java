package vn.edu.fpt.seal.modules.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.seal.config.AiProperties;
import vn.edu.fpt.seal.modules.report.ai.LlmClient;
import vn.edu.fpt.seal.modules.report.dto.VarianceAnalysisResponse;
import vn.edu.fpt.seal.modules.report.dto.VarianceAnalysisResponse.Ai;
import vn.edu.fpt.seal.modules.report.dto.VarianceAnalysisResponse.Hotspot;
import vn.edu.fpt.seal.modules.report.dto.VarianceAnalysisResponse.JudgeBias;
import vn.edu.fpt.seal.modules.report.dto.VarianceAnalysisResponse.Stats;
import vn.edu.fpt.seal.modules.report.dto.VarianceChatRequest;
import vn.edu.fpt.seal.modules.report.dto.VarianceChatResponse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the AI layer of the variance analysis.
 * <p>
 * Pipeline:
 * 1. get code-computed {@link Stats} (never sent raw names to the LLM)
 * 2. ANONYMIZE: replace real team/judge names with stable aliases
 * ("Team 1", "Judge A") before building the prompt
 * 3. call the {@link LlmClient} (Gemini) for a JSON narrative
 * 4. DE-ANONYMIZE: map aliases in the narrative back to real names
 * 5. cache the result in-memory keyed by roundId + hash(stats)
 * <p>
 * Degradation: if AI is disabled/unavailable or the call fails, the response
 * still carries the full {@link Stats} with {@code aiAvailable=false} and an
 * {@code aiError} note. The endpoint never fails just because the LLM did.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiVarianceAnalysisService {

    private final VarianceStatsService statsService;
    private final LlmClient llmClient;
    private final AiProperties props;
    private final ObjectMapper mapper;

    /**
     * cacheKey -> (statsHash, cached response). One entry per round+track.
     */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(int statsHash, VarianceAnalysisResponse response) {
    }

    public VarianceAnalysisResponse analyze(UUID eventId, UUID roundId, UUID trackId, boolean forceRefresh) {
        Stats stats = statsService.compute(eventId, roundId, trackId);
        int statsHash = Objects.hash(stats.toString());
        String cacheKey = roundId + "|" + (trackId == null ? "all" : trackId);

        if (!forceRefresh) {
            CacheEntry cached = cache.get(cacheKey);
            if (cached != null && cached.statsHash() == statsHash) {
                return cached.response();
            }
        }

        VarianceAnalysisResponse response = build(roundId, stats);
        // Only cache successful AI results; a failed call should retry next time.
        if (response.aiAvailable()) {
            cache.put(cacheKey, new CacheEntry(statsHash, response));
        }
        return response;
    }

    private VarianceAnalysisResponse build(UUID roundId, Stats stats) {
        if (!llmClient.isAvailable()) {
            return statsOnly(roundId, stats, "AI disabled or not configured");
        }
        if (stats.groupCount() == 0) {
            return statsOnly(roundId, stats, "No scoring data to analyze");
        }

        try {
            Anonymizer anon = new Anonymizer(stats);
            String userPrompt = anon.buildPrompt();
            String raw = llmClient.complete(systemPrompt(), userPrompt);
            Ai ai = parseAndDeAnonymize(raw, anon);
            return VarianceAnalysisResponse.builder()
                    .roundId(roundId)
                    .stats(stats)
                    .ai(ai)
                    .aiAvailable(true)
                    .aiError(null)
                    .build();
        } catch (Exception e) {
            log.warn("Variance AI analysis failed for round {}: {}", roundId, e.getMessage());
            return statsOnly(roundId, stats, "AI analysis unavailable: " + e.getMessage());
        }
    }

    private VarianceAnalysisResponse statsOnly(UUID roundId, Stats stats, String reason) {
        return VarianceAnalysisResponse.builder()
                .roundId(roundId)
                .stats(stats)
                .ai(null)
                .aiAvailable(false)
                .aiError(reason)
                .build();
    }

    public VarianceChatResponse chat(UUID eventId, UUID roundId, UUID trackId, VarianceChatRequest request) {
        Stats stats = statsService.compute(eventId, roundId, trackId);
        if (!llmClient.isAvailable()) {
            return VarianceChatResponse.builder()
                    .roundId(roundId)
                    .aiAvailable(false)
                    .aiError("AI disabled or not configured")
                    .reply(fallbackReply(stats, request))
                    .stats(stats)
                    .suggestions(defaultSuggestions())
                    .build();
        }
        try {
            Anonymizer anon = new Anonymizer(stats);
            String raw = llmClient.complete(chatSystemPrompt(), anon.buildChatPrompt(request == null ? null : request.messages()));
            return VarianceChatResponse.builder()
                    .roundId(roundId)
                    .aiAvailable(true)
                    .aiError(null)
                    .reply(anon.deAnonymize(stripCodeFence(raw)))
                    .stats(stats)
                    .suggestions(defaultSuggestions())
                    .build();
        } catch (Exception e) {
            log.warn("Variance AI chat failed for round {}: {}", roundId, e.getMessage());
            return VarianceChatResponse.builder()
                    .roundId(roundId)
                    .aiAvailable(false)
                    .aiError("AI chat unavailable: " + e.getMessage())
                    .reply(fallbackReply(stats, request))
                    .stats(stats)
                    .suggestions(defaultSuggestions())
                    .build();
        }
    }

    // ---- prompt -----------------------------------------------------------

    private String systemPrompt() {
        String lang = "en".equalsIgnoreCase(props.getLanguage()) ? "English" : "Vietnamese";
        return """
                You are a hackathon judging-analytics assistant. You are given \
                PRE-COMPUTED statistics about inter-judge scoring disagreement for \
                one round. Do NOT recompute any numbers; trust the values given. \
                Your job is to interpret them for a coordinator.
                
                Explain, in %s, what the disagreement patterns mean and what the \
                coordinator should do. Be concrete and concise. Distinguish a \
                single outlier judge from a genuine two-way split from a general \
                spread. Point out judges who are systematically lenient/harsh or \
                inconsistent across the round.
                
                Refer to teams and judges ONLY by the aliases given (e.g. \
                "Team 1", "Judge A"). Never invent names.
                
                Return ONLY valid JSON, no markdown, with this exact shape:
                {
                  "summary": "2-4 sentence overview of the round's consensus",
                  "recommendations": ["actionable item", "..."],
                  "judgeNotes": ["note about a specific judge's tendency", "..."]
                }
                """.formatted(lang);
    }

    private String chatSystemPrompt() {
        String lang = "en".equalsIgnoreCase(props.getLanguage()) ? "English" : "Vietnamese";
        return """
                You are a hackathon judging-analytics chatbot.
                Answer in %s.
                Use only provided round variance stats and chat history.
                Do not invent scores, judges, teams, or facts.
                Give direct practical answers for coordinator.
                Refer to teams and judges ONLY by aliases provided in prompt.
                Return plain text only, no markdown fence.
                """.formatted(lang);
    }

    private String fallbackReply(Stats stats, VarianceChatRequest request) {
        return "AI tạm không khả dụng. Hiện có " + stats.highVarianceCount()
                + " nhóm phương sai cao trên tổng " + stats.groupCount()
                + " nhóm. Câu hỏi gần nhất: " + lastUserMessage(request);
    }

    private String lastUserMessage(VarianceChatRequest request) {
        if (request == null || request.messages() == null) return "(không có)";
        for (int i = request.messages().size() - 1; i >= 0; i--) {
            VarianceChatRequest.Message m = request.messages().get(i);
            if (m != null && "user".equalsIgnoreCase(m.role()) && m.content() != null && !m.content().isBlank()) {
                return m.content();
            }
        }
        return "(không có)";
    }

    private List<String> defaultSuggestions() {
        return List.of(
                "Vì sao round này có phương sai cao?",
                "Giám khảo nào đang chấm dễ hoặc chấm khó?",
                "Coordinator nên kiểm tra gì trước?"
        );
    }

    // ---- parse + de-anonymize --------------------------------------------

    private Ai parseAndDeAnonymize(String raw, Anonymizer anon) {
        String json = stripCodeFence(raw);
        try {
            JsonNode root = mapper.readTree(json);
            String summary = anon.deAnonymize(root.path("summary").asText(""));
            List<String> recs = readArray(root.path("recommendations"), anon);
            List<String> notes = readArray(root.path("judgeNotes"), anon);
            return Ai.builder()
                    .summary(summary)
                    .recommendations(recs)
                    .judgeNotes(notes)
                    .build();
        } catch (Exception e) {
            throw new LlmClient.LlmException("Failed to parse AI JSON: " + e.getMessage(), e);
        }
    }

    private List<String> readArray(JsonNode node, Anonymizer anon) {
        List<String> out = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode n : node) {
                String s = anon.deAnonymize(n.asText(""));
                if (!s.isBlank()) out.add(s);
            }
        }
        return out;
    }

    /**
     * Some models wrap JSON in ```json fences despite instructions.
     */
    private static String stripCodeFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl >= 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    // ---- anonymization ----------------------------------------------------

    /**
     * Builds stable real-name <-> alias maps from the stats, renders the LLM
     * prompt using aliases only, and maps aliases back to real names in the
     * AI narrative. The real names never leave this process.
     */
    private final class Anonymizer {
        private final Stats stats;
        private final Map<String, String> teamToAlias = new LinkedHashMap<>();
        private final Map<String, String> judgeToAlias = new LinkedHashMap<>();
        private final Map<String, String> aliasToReal = new HashMap<>();

        Anonymizer(Stats stats) {
            this.stats = stats;
            for (Hotspot h : stats.hotspots()) {
                aliasTeam(h.teamName());
                if (h.outlierJudge() != null) aliasJudge(h.outlierJudge());
            }
            for (JudgeBias b : stats.judgeBiases()) {
                aliasJudge(b.judgeName());
            }
        }

        private String aliasTeam(String name) {
            if (name == null) return "Team ?";
            return teamToAlias.computeIfAbsent(name, k -> {
                String alias = "Team " + (teamToAlias.size() + 1);
                aliasToReal.put(alias, name);
                return alias;
            });
        }

        private String aliasJudge(String name) {
            if (name == null) return "Judge ?";
            return judgeToAlias.computeIfAbsent(name, k -> {
                String alias = "Judge " + alpha(judgeToAlias.size());
                aliasToReal.put(alias, name);
                return alias;
            });
        }

        String buildPrompt() {
            StringBuilder sb = new StringBuilder();
            sb.append("ROUND SUMMARY:\n");
            sb.append("- groups (team x criterion): ").append(stats.groupCount()).append('\n');
            sb.append("- high-variance groups: ").append(stats.highVarianceCount()).append('\n');
            sb.append("- average variance: ").append(stats.avgVariance()).append('\n');

            sb.append("\nHOTSPOTS (highest disagreement first):\n");
            for (Hotspot h : stats.hotspots()) {
                sb.append("- ").append(aliasTeam(h.teamName()))
                        .append(" / criterion \"").append(h.criterionName()).append("\": ")
                        .append("judges=").append(h.judgeCount())
                        .append(", mean=").append(h.meanScore())
                        .append(", variance=").append(h.variance())
                        .append(", min=").append(h.minScore())
                        .append(", max=").append(h.maxScore())
                        .append(", spread=").append(h.spread())
                        .append(", pattern=").append(h.pattern());
                if (h.outlierJudge() != null) {
                    sb.append(", outlier=").append(aliasJudge(h.outlierJudge()));
                }
                sb.append('\n');
            }

            sb.append("\nJUDGE TENDENCIES (across the round):\n");
            for (JudgeBias b : stats.judgeBiases()) {
                sb.append("- ").append(aliasJudge(b.judgeName()))
                        .append(": groupsScored=").append(b.groupsScored())
                        .append(", avgDeviationFromPeers=").append(b.avgDeviation())
                        .append(", tendency=").append(b.tendency())
                        .append(", timesOutlier=").append(b.outlierCount())
                        .append('\n');
            }
            return sb.toString();
        }

        String buildChatPrompt(List<VarianceChatRequest.Message> messages) {
            StringBuilder sb = new StringBuilder();
            sb.append(buildPrompt());
            sb.append("\nCONVERSATION:\n");
            if (messages != null) {
                for (VarianceChatRequest.Message m : messages) {
                    if (m == null || m.content() == null || m.content().isBlank()) continue;
                    sb.append(m.role() == null ? "user" : m.role())
                            .append(": ")
                            .append(m.content())
                            .append('\n');
                }
            }
            sb.append("assistant: Reply to the latest user message using the round stats above.\n");
            return sb.toString();
        }

        /**
         * Replace every alias occurrence in AI text with the real name.
         */
        String deAnonymize(String text) {
            if (text == null || text.isEmpty()) return text;
            String result = text;
            // Longest aliases first so "Judge AA" is handled before "Judge A".
            List<String> aliases = new ArrayList<>(aliasToReal.keySet());
            aliases.sort(Comparator.comparingInt(String::length).reversed());
            for (String alias : aliases) {
                result = result.replace(alias, aliasToReal.get(alias));
            }
            return result;
        }

        private String alpha(int idx) {
            StringBuilder sb = new StringBuilder();
            int n = idx;
            do {
                sb.insert(0, (char) ('A' + (n % 26)));
                n = n / 26 - 1;
            } while (n >= 0);
            return sb.toString();
        }
    }
}
