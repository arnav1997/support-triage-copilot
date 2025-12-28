package com.infotrode.support_triage.ai.dto;

import com.infotrode.support_triage.ticket.TicketPriority;

import java.util.List;

public record AiTriageSuggestion(
        String category,
        TicketPriority priority,
        List<String> tags,
        String rationale,
        Entities entities,
        Long aiRunId
) {
    public record Entities(
            String requesterEmail,
            String orderId,
            String product,
            String errorCode
    ) {}
}
