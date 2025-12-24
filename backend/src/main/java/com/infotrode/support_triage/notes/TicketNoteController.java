package com.infotrode.support_triage.notes;

import com.infotrode.support_triage.notes.dto.CreateNoteRequest;
import com.infotrode.support_triage.notes.dto.TicketNoteDto;
import com.infotrode.support_triage.ticket.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/notes")
public class TicketNoteController {

    private final TicketNoteRepository noteRepo;
    private final TicketRepository ticketRepo;

    public TicketNoteController(TicketNoteRepository noteRepo, TicketRepository ticketRepo) {
        this.noteRepo = noteRepo;
        this.ticketRepo = ticketRepo;
    }

    @GetMapping
    public List<TicketNoteDto> list(@PathVariable Long ticketId) {
        ensureTicketExists(ticketId);
        return noteRepo.findByTicketIdOrderByCreatedAtDesc(ticketId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketNoteDto create(@PathVariable Long ticketId, @RequestBody CreateNoteRequest req) {
        ensureTicketExists(ticketId);

        if (req == null || req.body == null || req.body.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }

        String type = (req.type == null || req.type.isBlank()) ? "note" : req.type.trim().toLowerCase();
        if (type.length() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be <= 32 chars");
        }


        TicketNote note = new TicketNote();
        note.setTicketId(ticketId);
        note.setType(type);
        note.setBody(req.body.trim());

        return toDto(noteRepo.save(note));
    }

    private TicketNoteDto toDto(TicketNote n) {
        TicketNoteDto dto = new TicketNoteDto();
        dto.id = n.getId();
        dto.ticketId = n.getTicketId();
        dto.type = n.getType();
        dto.body = n.getBody();
        dto.createdAt = n.getCreatedAt();
        return dto;
    }

    private void ensureTicketExists(Long ticketId) {
        if (!ticketRepo.existsById(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ticket not found");
        }
    }
}
