package com.tfxsoftware.memserver.modules.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {
    Optional<EventRegistration> findByRosterIdAndEventId(UUID rosterId, UUID eventId);
    List<EventRegistration> findAllByEventId(UUID eventId);
}