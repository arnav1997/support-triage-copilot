package com.infotrode.support_triage.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
      select t from Ticket t
      where (:status is null or t.status = :status)
        and (
          :q is null or :q = '' or
          lower(t.subject) like lower(concat('%', :q, '%')) or
          lower(t.body) like lower(concat('%', :q, '%')) or
          lower(t.requesterEmail) like lower(concat('%', :q, '%')) or
          (t.category is not null and lower(t.category) like lower(concat('%', :q, '%')))
        )
      order by t.updatedAt desc
    """)
    List<Ticket> search(@Param("status") TicketStatus status, @Param("q") String q);
}
