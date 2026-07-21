package vn.edu.fpt.seal.modules.timeline;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class TimelineDomainHookContractTest {
    @Test
    void everyRequiredDomainGroupUsesCentralTimelineService() throws Exception {
        assertHooks("round/service/RoundService.java","RESULT_PUBLISHED","ROUND_ADVANCED","COMPETITION_RESUMED");
        assertHooks("submission/service/SubmissionService.java","SUBMISSION_CREATED","SUBMISSION_UPDATED");
        assertHooks("appeal/service/AppealService.java","APPEAL_SUBMITTED","ROUND_PAUSED_FOR_APPEAL","RESULT_RECALCULATION_REQUIRED");
        assertHooks("incident/service/IncidentService.java","INCIDENT_SUBMITTED","INCIDENT_RESOLVED");
        assertHooks("support/service/SupportTicketService.java","SUPPORT_CREATED","SUPPORT_RESOLVED");
        assertHooks("seeding/service/SeedingService.java","TRACK_RESULTS_FINALIZED","SEED_CONFIRMED","SEED_REMOVED");
        assertHooks("prize/service/PrizeService.java","PRIZE_CONFIGURED");
        assertHooks("recognition/service/TeamRecognitionService.java","RECOGNITION_AWARDED","RECOGNITION_REVOKED","RECOGNITION_RESTORED");
    }

    @Test
    void noDomainServiceWritesTimelineRepositoryDirectly() throws Exception {
        Path modules=Path.of("src/main/java/vn/edu/fpt/seal/modules");
        try(var files=Files.walk(modules)){
            files.filter(p->p.toString().endsWith(".java"))
                    .filter(p->!p.toString().contains("\\timeline\\")&&!p.toString().contains("/timeline/"))
                    .forEach(p->assertDoesNotThrow(()->assertFalse(Files.readString(p).contains("TimelineEventRepository"),
                            "Direct timeline repository use: "+p)));
        }
    }

    private void assertHooks(String relative,String...hooks)throws Exception{
        String source=Files.readString(Path.of("src/main/java/vn/edu/fpt/seal/modules").resolve(relative));
        assertTrue(source.contains("TimelineService"),relative+" must use TimelineService");
        for(String hook:hooks)assertTrue(source.contains(hook),relative+" missing "+hook);
    }
}
