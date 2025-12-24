package com.infotrode.support_triage.notes;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketNoteRepository extends JpaRepository<TicketNote, Long> {
    List<TicketNote> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
