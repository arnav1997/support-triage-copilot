package com.infotrode.support_triage.ticket.dto;

import com.infotrode.support_triage.ticket.TicketPriority;
import com.infotrode.support_triage.ticket.TicketStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateTicketRequest {

    @NotBlank
    @Size(max = 255)
    private String subject;

    @NotBlank
    @Email
    @Size(max = 320)
    private String requesterEmail;

    @NotBlank
    private String body;

    // optional; defaults in entity
    private TicketStatus status;
    private TicketPriority priority;

    @Size(max = 64)
    private String category;

    private List<@Size(max = 64) String> tags;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
