package com.infotrode.support_triage.ticket;

import com.infotrode.support_triage.ticket.dto.CreateTicketRequest;
import com.infotrode.support_triage.ticket.dto.UpdateTicketRequest;
import com.infotrode.support_triage.ticket.dto.TicketResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public List<TicketResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        TicketStatus parsedStatus = parseStatus(status);
        return service.list(parsedStatus, q).stream().map(TicketResponse::from).toList();
    }

    private TicketStatus parseStatus(String status) {
        if (status == null) return null;

        String s = status.trim();
        if (s.isEmpty()) return null;

        try {
            return TicketStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid status: " + status + ". Allowed: " + java.util.Arrays.toString(TicketStatus.values())
            );
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest req) {
        return TicketResponse.from(service.create(req));
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable long id) {
        return TicketResponse.from(service.get(id));
    }

    @PatchMapping("/{id}")
    public TicketResponse patch(@PathVariable long id, @Valid @RequestBody UpdateTicketRequest req) {
        return TicketResponse.from(service.update(id, req));
    }
}
