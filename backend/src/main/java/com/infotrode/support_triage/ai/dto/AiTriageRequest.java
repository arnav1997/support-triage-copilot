package com.infotrode.support_triage.ai.dto;

import jakarta.validation.constraints.NotNull;

public record AiTriageRequest(@NotNull Long ticketId) {}
