package com.infotrode.support_triage.ticket;

import com.infotrode.support_triage.ticket.dto.CreateTicketRequest;
import com.infotrode.support_triage.ticket.dto.UpdateTicketRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository repo;

    public TicketService(TicketRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Ticket> list() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Ticket get(long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + id));
    }

    @Transactional
    public Ticket create(CreateTicketRequest req) {
        Ticket t = new Ticket();
        t.setSubject(req.getSubject());
        t.setRequesterEmail(req.getRequesterEmail());
        t.setBody(req.getBody());
        t.setStatus(req.getStatus());     // can be null; entity defaults handle it
        t.setPriority(req.getPriority()); // can be null
        t.setCategory(req.getCategory());
        t.setTags(req.getTags());
        return repo.save(t);
    }

    @Transactional
    public Ticket update(long id, UpdateTicketRequest req) {
        Ticket t = get(id);

        if (req.getSubject() != null) t.setSubject(req.getSubject());
        if (req.getStatus() != null) t.setStatus(req.getStatus());
        if (req.getPriority() != null) t.setPriority(req.getPriority());
        if (req.getCategory() != null) t.setCategory(req.getCategory());
        if (req.getTags() != null) t.setTags(req.getTags());
        if (req.getBody() != null) t.setBody(req.getBody());

        return repo.save(t);
    }
}
