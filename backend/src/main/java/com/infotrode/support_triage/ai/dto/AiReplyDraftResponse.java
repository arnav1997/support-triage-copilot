package com.infotrode.support_triage.ai.dto;

import com.infotrode.support_triage.ai.ReplyTone;

public record AiReplyDraftResponse(
        Long ticketId,
        ReplyTone tone,
        String draft,
        Long aiRunId
) {}
