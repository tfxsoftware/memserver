package com.tfxsoftware.memserver.modules.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfxsoftware.memserver.modules.events.league.LeagueGenerator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The EventOrchestrator is the background pulse of the competition system.
 * It manages transitions between event states (CLOSED -> OPEN -> ONGOING).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventOrchestrator {
    
    private final EventRepository eventRepository;
    private final LeagueGenerator leagueGenerator;
    private final EventService eventService;

    /**
     * Runs every minute to check for lifecycle transitions.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processLifecycleTicks() {
        LocalDateTime now = LocalDateTime.now();
        
        openEligibleEvents(now);
        startEligibleEvents(now);
        finishEligibleEvents(now);
    }

    /**
     * Moves events from ONGOING to FINISHED and distributes prizes.
     */
    private void finishEligibleEvents(LocalDateTime now) {
        List<Event> toFinish = eventRepository.findAllByStatusAndFinishesAtBefore(Event.EventStatus.ONGOING, now);
        
        for (Event event : toFinish) {
            log.info("Finishing Event: {}. Type: {}", event.getName(), event.getType());
            eventService.finishEvent(event);
        }
    }

    /**
     * Moves events from CLOSED to OPEN so users can see them and register.
     */
    private void openEligibleEvents(LocalDateTime now) {
        List<Event> toOpen = eventRepository.findAllByStatusAndOpensAtBefore(Event.EventStatus.CLOSED, now);
        log.debug("Found {} events to open.", toOpen.size());
        for (Event event : toOpen) {
            log.info("Opening registration for Event: {}", event.getName());
            event.setStatus(Event.EventStatus.OPEN);
        }
        
        if (!toOpen.isEmpty()) eventRepository.saveAll(toOpen);
    }

    /**
     * Moves events from OPEN to ONGOING and generates the competition schedule.
     */
    private void startEligibleEvents(LocalDateTime now) {
        List<Event> toStart = eventRepository.findAllByStatusAndStartsAtBefore(Event.EventStatus.OPEN, now);

        for (Event event : toStart) {
            log.info("Starting Event: {}. Type: {}", event.getName(), event.getType());

            if (event.getRegistrations().size() < 2) {
                log.warn("Event {} has insufficient registrations ({}). Cancelling.", event.getName(), event.getRegistrations().size());
                event.setStatus(Event.EventStatus.CANCELLED);
                continue;
            }

            // Transition state
            event.setStatus(Event.EventStatus.ONGOING);

            // Logic hand-off based on type
            if (event.getType() == Event.EventType.LEAGUE) {
                leagueGenerator.generateFullSeason(event);
            }
            
            // Note: Tournament logic would go here in Phase 2
        }

        if (!toStart.isEmpty()) eventRepository.saveAll(toStart);
    }
}