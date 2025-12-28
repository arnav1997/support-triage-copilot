package com.infotrode.support_triage.ai;

import com.infotrode.support_triage.ai.dto.AiTriageRequest;
import com.infotrode.support_triage.ai.dto.AiTriageSuggestion;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiTriageController {

    private final AiTriageService service;

    public AiTriageController(AiTriageService service) {
        this.service = service;
    }

    @PostMapping("/triage")
    public AiTriageSuggestion triage(@Valid @RequestBody AiTriageRequest req) {
        return service.triage(req.ticketId());
    }
}
