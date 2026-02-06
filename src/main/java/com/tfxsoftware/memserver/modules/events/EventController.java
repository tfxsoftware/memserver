package com.tfxsoftware.memserver.modules.events;

import java.util.UUID;

import com.tfxsoftware.memserver.modules.events.dto.CreateEventDto;
import com.tfxsoftware.memserver.modules.events.dto.EventRegistrationResponse;
import com.tfxsoftware.memserver.modules.events.dto.EventResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfxsoftware.memserver.modules.users.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventDto createEventDto) {
        return new ResponseEntity<>(eventService.createEvent(createEventDto), HttpStatus.CREATED);
    }

    @PostMapping("/{eventId}/register/roster/{rosterId}")
    public ResponseEntity<EventRegistrationResponse> registerForEvent(
            @PathVariable UUID eventId,
            @PathVariable UUID rosterId,
            @AuthenticationPrincipal User currentUser) {
        
        return new ResponseEntity<>(eventService.registerForEvent(eventId, rosterId, currentUser), HttpStatus.CREATED);
    }
}
