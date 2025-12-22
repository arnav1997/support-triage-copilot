package com.infotrode.support_triage.ticket.dto;

import com.infotrode.support_triage.ticket.TicketPriority;
import com.infotrode.support_triage.ticket.TicketStatus;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UpdateTicketRequest {

    @Size(max = 255)
    private String subject;

    private TicketStatus status;
    private TicketPriority priority;

    @Size(max = 64)
    private String category;

    private List<@Size(max = 64) String> tags;

    @Size(max = 20000)
    private String body;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
