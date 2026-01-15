package com.infotrode.support_triage.ai;

import com.infotrode.support_triage.ai.dto.AiReplyDraftRequest;
import com.infotrode.support_triage.ai.dto.AiReplyDraftResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiReplyDraftController {

    private final AiReplyDraftService service;

    public AiReplyDraftController(AiReplyDraftService service) {
        this.service = service;
    }

    @PostMapping("/reply-draft")
    public AiReplyDraftResponse replyDraft(@Valid @RequestBody AiReplyDraftRequest req) {
        return service.draftReply(req.ticketId(), req.tone());
    }
}
