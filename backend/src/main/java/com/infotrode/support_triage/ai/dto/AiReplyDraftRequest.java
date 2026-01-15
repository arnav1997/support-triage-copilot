package com.infotrode.support_triage.ai.dto;

import com.infotrode.support_triage.ai.ReplyTone;
import jakarta.validation.constraints.NotNull;

public record AiReplyDraftRequest(
        @NotNull Long ticketId,
        @NotNull ReplyTone tone
) {}
