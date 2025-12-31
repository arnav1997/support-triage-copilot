package com.infotrode.support_triage.ai.dto;

import java.util.List;

public record AiSummaryResponse(
        Long ticketId,
        String summary,
        List<String> keyPoints,
        Long savedNoteId
) {}
