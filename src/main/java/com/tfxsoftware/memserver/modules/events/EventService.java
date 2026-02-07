package com.tfxsoftware.memserver.modules.events;

import com.tfxsoftware.memserver.modules.users.User;
import com.tfxsoftware.memserver.modules.users.UserRepository;
import com.tfxsoftware.memserver.modules.rosters.Roster; // New import
import com.tfxsoftware.memserver.modules.rosters.RosterRepository; // New import
import org.springframework.web.server.ResponseStatusException;
import com.tfxsoftware.memserver.modules.events.dto.CreateEventDto;
import com.tfxsoftware.memserver.modules.events.dto.EventRegistrationResponse;
import com.tfxsoftware.memserver.modules.events.dto.EventResponse;
import com.tfxsoftware.memserver.modules.events.league.League;
import com.tfxsoftware.memserver.modules.events.league.LeagueStanding;
import com.tfxsoftware.memserver.modules.events.league.LeagueStandingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final RosterRepository rosterRepository; // New injection
    private final LeagueStandingRepository leagueStandingRepository;

    /**
     * Creates a new Event with strict validation on financial and chronological data.
     * All events start in the CLOSED status.
     */
    @Transactional
    public EventResponse createEvent(CreateEventDto dto) {
        // 1. Uniqueness check
        if (eventRepository.findByName(dto.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An event with the name '" + dto.getName() + "' already exists.");
        }

        // 2. Financial Validation: Sum of rank prizes must equal total prize pool
        validatePrizeDistribution(dto);

        // 3. Chronological Validation: opensAt must be before startsAt
        if (dto.getStartsAt() != null && dto.getOpensAt().isAfter(dto.getStartsAt())) {
            throw new IllegalArgumentException("Event opening time (opensAt) must be before the starting time (startsAt).");
        }

        // 4. Build the Base Event
        Event event = Event.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .regions(dto.getRegions()) // Changed from region
                .type(dto.getType())
                .status(Event.EventStatus.CLOSED)
                .tier(dto.getTier())
                .entryFee(dto.getEntryFee())
                .totalPrizePool(dto.getTotalPrizePool())
                .rankPrizes(dto.getRankPrizes())
                .opensAt(dto.getOpensAt())
                .startsAt(dto.getStartsAt())
                .gamesPerBlock(dto.getGamesPerBlock())
                .minutesBetweenGames(dto.getMinutesBetweenGames())
                .minutesBetweenBlocks(dto.getMinutesBetweenBlocks())
                .maxPlayers(dto.getMaxPlayers())
                .build();

        // 5. Handle League-specific initialization
        if (dto.getType() == Event.EventType.LEAGUE) {
            if (dto.getRoundRobinCount() == null || dto.getRoundRobinCount() < 1) {
                throw new IllegalArgumentException("League type events must specify a roundRobinCount of at least 1.");
            }

            League league = League.builder()
                    .event(event)
                    .roundRobinCount(dto.getRoundRobinCount())
                    .build();
            
            event.setLeague(league);
        }

        log.info("Successfully created event: {} (Type: {}, Tier: {})", event.getName(), event.getType(), event.getTier());
        Event savedEvent = eventRepository.save(event);
        return mapToEventResponse(savedEvent);
    }

    /**
     * Handles a roster registering for an event, deducting balance from the roster owner.
     * @param eventId The ID of the event to register for.
     * @param rosterId The ID of the roster registering.
     * @param currentUser The authenticated user attempting the registration.
     * @return The created EventRegistration.
     */
    @Transactional
    public EventRegistrationResponse registerForEvent(UUID eventId, UUID rosterId, User currentUser) {
        // 1. Fetch Event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found with ID: " + eventId));

        // 2. Fetch Roster
        Roster roster = rosterRepository.findById(rosterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster not found with ID: " + rosterId));

        // 2.1. Region Validation
        if (!event.getRegions().contains(roster.getRegion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                "Roster region (%s) is not allowed for this event's regions (%s).",
                roster.getRegion(), event.getRegions()
            ));
        }

        // 2.2. Check Roster Status
        if (roster.getActivity() != Roster.RosterActivity.IDLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roster is not IDLE and cannot register for the event. Current status: " + roster.getActivity());
        }

        // 3. Verify Roster Ownership
        if (!roster.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Roster with ID " + rosterId + " does not belong to the current user.");
        }

        // 4. Get the Roster's Owner (User)
        // Re-fetching the user ensures we have a managed entity for transactional operations
        User owner = userRepository.findById(roster.getOwner().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster owner not found."));


        // 5. Check Event Status
        if (event.getStatus() != Event.EventStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not open for registration.");
        }

        // 6. Check if Roster is already registered
        if (eventRegistrationRepository.findByRosterIdAndEventId(rosterId, eventId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Roster is already registered for this event.");
        }

        // NEW VALIDATION: Check if the event is full
        if (event.getMaxPlayers() != null && event.getRegistrations().size() >= event.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is full. Maximum players reached.");
        }

        // 7. Check Balance of Roster Owner
        if (owner.getBalance().compareTo(event.getEntryFee()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance for roster owner to register for this event. Required: " + event.getEntryFee());
        }

        // 8. Deduct Entry Fee from Roster Owner
        owner.setBalance(owner.getBalance().subtract(event.getEntryFee()));
        userRepository.save(owner); // Save updated user balance

        // 9. Create EventRegistration
        EventRegistration registration = EventRegistration.builder()
                .event(event)
                .roster(roster) // Link to Roster
                .registrationDate(LocalDateTime.now())
                .build();

        // 10. Update Roster Status
        roster.setActivity(Roster.RosterActivity.IN_EVENT);
        rosterRepository.save(roster);

        log.info("Roster {} (Owner: {}) registered for event {}. Deducted {}", rosterId, owner.getId(), eventId, event.getEntryFee());
        EventRegistration savedRegistration = eventRegistrationRepository.save(registration);
        return mapToRegistrationResponse(savedRegistration);
    }

    /**
     * Finishes an event, sets rosters to IDLE, and distributes prizes.
     */
    @Transactional
    public void finishEvent(Event event) {
        event.setStatus(Event.EventStatus.FINISHED);
        
        // 1. Reset Roster Activities
        List<Roster> participants = event.getRegistrations().stream()
                .map(EventRegistration::getRoster)
                .toList();
        
        for (Roster roster : participants) {
            roster.setActivity(Roster.RosterActivity.IDLE);
        }
        rosterRepository.saveAll(participants);

        // 2. Distribute Prizes
        if (event.getType() == Event.EventType.LEAGUE && event.getLeague() != null) {
            distributeLeaguePrizes(event);
        }

        eventRepository.save(event);
        log.info("Event {} finished and prizes distributed.", event.getName());
    }

    private void distributeLeaguePrizes(Event event) {
        List<LeagueStanding> standings = leagueStandingRepository.findAllByLeagueEventIdOrderByWinsDesc(event.getLeague().getEventId());
        Map<Integer, BigDecimal> prizes = event.getRankPrizes();

        if (prizes == null || prizes.isEmpty()) {
            return;
        }

        for (int i = 0; i < standings.size(); i++) {
            int rank = i + 1;
            BigDecimal prize = prizes.get(rank);
            
            if (prize != null && prize.compareTo(BigDecimal.ZERO) > 0) {
                LeagueStanding standing = standings.get(i);
                Roster roster = standing.getRoster();
                User owner = roster.getOwner();

                owner.setBalance(owner.getBalance().add(prize));
                userRepository.save(owner);
                
                log.info("Prize of {} awarded to {} (Owner: {}) for Rank {} in Event {}", 
                        prize, roster.getName(), owner.getUsername(), rank, event.getName());
            }
        }
    }

    /**
     * Verifies that the mathematical sum of the rank-based rewards 
     * exactly matches the declared total prize pool.
     */
    private void validatePrizeDistribution(CreateEventDto dto) {
        if (dto.getRankPrizes() == null || dto.getRankPrizes().isEmpty()) {
            if (dto.getTotalPrizePool().compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("A total prize pool was defined, but no rank-based prize distribution was provided.");
            }
            return;
        }

        BigDecimal sumOfPrizes = dto.getRankPrizes().values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumOfPrizes.compareTo(dto.getTotalPrizePool()) != 0) {
            throw new IllegalArgumentException(String.format(
                "Prize distribution mismatch: The sum of individual rank prizes (%s) must exactly match the total prize pool (%s).",
                sumOfPrizes.toPlainString(),
                dto.getTotalPrizePool().toPlainString()
            ));
        }
    }

    private EventResponse mapToEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getImageUrl(),
                null,
                event.getRegions().isEmpty() ? null : event.getRegions().iterator().next(),
                event.getOpensAt(),
                event.getStartsAt(),
                event.getFinishesAt(),
                event.getType(),
                event.getStatus(),
                event.getTier(),
                event.getEntryFee(),
                event.getTotalPrizePool(),
                event.getRankPrizes(),
                event.getGamesPerBlock(),
                event.getMinutesBetweenGames(),
                event.getMinutesBetweenBlocks()
        );
    }

    private EventRegistrationResponse mapToRegistrationResponse(EventRegistration registration) {
        return EventRegistrationResponse.builder()
                .id(registration.getId())
                .rosterId(registration.getRoster().getId())
                .rosterName(registration.getRoster().getName())
                .eventId(registration.getEvent().getId())
                .eventName(registration.getEvent().getName())
                .registrationDate(registration.getRegistrationDate())
                .build();
    }
}