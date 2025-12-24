package com.infotrode.support_triage.notes.dto;

import java.time.OffsetDateTime;

public class TicketNoteDto {
    public Long id;
    public Long ticketId;
    public String type;
    public String body;
    public OffsetDateTime createdAt;
}
