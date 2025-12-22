package com.infotrode.support_triage.ticket.dto;

import com.infotrode.support_triage.ticket.Ticket;
import com.infotrode.support_triage.ticket.TicketPriority;
import com.infotrode.support_triage.ticket.TicketStatus;
import java.time.Instant;
import java.util.List;

public class TicketResponse {
    private Long id;
    private String subject;
    private String requesterEmail;
    private String body;
    private TicketStatus status;
    private TicketPriority priority;
    private String category;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;

    public static TicketResponse from(Ticket t) {
        TicketResponse r = new TicketResponse();
        r.id = t.getId();
        r.subject = t.getSubject();
        r.requesterEmail = t.getRequesterEmail();
        r.body = t.getBody();
        r.status = t.getStatus();
        r.priority = t.getPriority();
        r.category = t.getCategory();
        r.tags = t.getTags();
        r.createdAt = t.getCreatedAt();
        r.updatedAt = t.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public String getRequesterEmail() { return requesterEmail; }
    public String getBody() { return body; }
    public TicketStatus getStatus() { return status; }
    public TicketPriority getPriority() { return priority; }
    public String getCategory() { return category; }
    public List<String> getTags() { return tags; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
