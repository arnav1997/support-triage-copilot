package com.infotrode.support_triage.ai;

import com.infotrode.support_triage.ai.dto.AiSummaryRequest;
import com.infotrode.support_triage.ai.dto.AiSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiSummaryController {

    private final AiSummaryService service;

    public AiSummaryController(AiSummaryService service) {
        this.service = service;
    }

    @PostMapping("/summary")
    public AiSummaryResponse summary(@Valid @RequestBody AiSummaryRequest req) {
        boolean save = Boolean.TRUE.equals(req.saveAsNote());
        return service.summarize(req.ticketId(), save);
    }
}
