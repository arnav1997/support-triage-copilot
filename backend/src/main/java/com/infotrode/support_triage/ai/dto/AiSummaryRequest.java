package com.infotrode.support_triage.ai.dto;

import jakarta.validation.constraints.NotNull;

public record AiSummaryRequest(
        @NotNull Long ticketId,
        Boolean saveAsNote
) {}
