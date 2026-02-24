package com.tfxsoftware.memserver.modules.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
    Optional<Event> findByName(String name);
    List<Event> findAllByStatusAndOpensAtBefore(Event.EventStatus status, LocalDateTime dateTime);
    List<Event> findAllByStatusAndStartsAtBefore(Event.EventStatus status, LocalDateTime dateTime);
    List<Event> findAllByStatusAndFinishesAtBefore(Event.EventStatus status, LocalDateTime dateTime);
}