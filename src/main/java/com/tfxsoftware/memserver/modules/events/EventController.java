package com.tfxsoftware.memserver.modules.events;

import java.util.List;
import java.util.UUID;

import com.tfxsoftware.memserver.modules.events.dto.CreateEventDto;
import com.tfxsoftware.memserver.modules.events.dto.EventRegisteredRosterDto;
import com.tfxsoftware.memserver.modules.events.dto.EventRegistrationResponse;
import com.tfxsoftware.memserver.modules.events.dto.EventResponse;
import com.tfxsoftware.memserver.modules.users.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEvents(
            Pageable pageable,
            @RequestParam(required = false) User.Region region,
            @RequestParam(required = false) Event.EventStatus status,
            @RequestParam(required = false) Event.Tier tier,
            @RequestParam(required = false) Event.EventType type
    ) {
        return ResponseEntity.ok(eventService.getEvents(pageable, region, status, tier, type));
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventDto createEventDto) {
        return new ResponseEntity<>(eventService.createEvent(createEventDto), HttpStatus.CREATED);
    }

    @GetMapping("/{eventId}/rosters")
    public ResponseEntity<List<EventRegisteredRosterDto>> getEventRosters(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getRegisteredRostersForEvent(eventId));
    }

    @PostMapping("/{eventId}/register/roster/{rosterId}")
    public ResponseEntity<EventRegistrationResponse> registerForEvent(
            @PathVariable UUID eventId,
            @PathVariable UUID rosterId,
            @AuthenticationPrincipal User currentUser) {
        
        return new ResponseEntity<>(eventService.registerForEvent(eventId, rosterId, currentUser), HttpStatus.CREATED);
    }
}
