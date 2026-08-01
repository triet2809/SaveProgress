package vn.edu.fpt.seal.modules.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.seal.modules.report.dto.AnonymizedDatasetResponse;
import vn.edu.fpt.seal.modules.report.dto.JudgeVarianceResponse;
import vn.edu.fpt.seal.modules.report.dto.VarianceAnalysisResponse;
import vn.edu.fpt.seal.modules.report.dto.VarianceChatRequest;
import vn.edu.fpt.seal.modules.report.dto.VarianceChatResponse;
import vn.edu.fpt.seal.modules.report.service.AiVarianceAnalysisService;
import vn.edu.fpt.seal.modules.report.service.ReportService;

import java.util.List;
import java.util.UUID;

/**
 * Coordinator-facing reporting/analytics over a round's scores:
 *  - #11 ranking CSV export
 *  - #12 anonymized dataset export
 *  - #13 inter-judge variance dashboard
 *
 * All endpoints are read-only and coordinator-only (they expose cross-team
 * data that participants must not see).
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Ranking export, anonymized dataset, and judge-variance analytics")
public class ReportController {

    private final ReportService reportService;
    private final AiVarianceAnalysisService aiVarianceAnalysisService;

    @GetMapping("/rounds/{roundId}/judge-variance")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Inter-judge variance per team/criterion for a round (#13)")
    public ResponseEntity<List<JudgeVarianceResponse>> judgeVariance(@PathVariable UUID roundId,
            @RequestParam UUID eventId, @RequestParam(required=false) UUID trackId) {
        return ResponseEntity.ok(reportService.judgeVariance(eventId, roundId, trackId));
    }

    @PostMapping("/rounds/{roundId}/variance-analysis")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "AI-assisted interpretation of inter-judge variance for a round. "
            + "Statistics are computed in code; the LLM only narrates. Data sent to the "
            + "external LLM is anonymized. Falls back to stats-only if AI is unavailable.")
    public ResponseEntity<VarianceAnalysisResponse> varianceAnalysis(@PathVariable UUID roundId,
            @RequestParam UUID eventId, @RequestParam(required=false) UUID trackId,
            @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(aiVarianceAnalysisService.analyze(eventId, roundId, trackId, refresh));
    }

    @PostMapping("/rounds/{roundId}/variance-chat")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Chat with the AI about variance patterns in the round")
    public ResponseEntity<VarianceChatResponse> varianceChat(@PathVariable UUID roundId,
            @RequestParam UUID eventId, @RequestParam(required=false) UUID trackId,
            @RequestBody(required = false) VarianceChatRequest request) {
        return ResponseEntity.ok(aiVarianceAnalysisService.chat(eventId, roundId, trackId, request));
    }

    @GetMapping("/rounds/{roundId}/anonymized-dataset")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Anonymized scoring dataset for a round (#12)")
    public ResponseEntity<AnonymizedDatasetResponse> anonymizedDataset(@PathVariable UUID roundId) {
        return ResponseEntity.ok(reportService.anonymizedDataset(roundId));
    }

    @GetMapping(value = "/rounds/{roundId}/ranking.csv", produces = "text/csv")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Export round rankings as CSV (#11)")
    public ResponseEntity<String> rankingCsv(@PathVariable UUID roundId) {
        String csv = reportService.rankingCsv(roundId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"round-" + roundId + "-rankings.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
