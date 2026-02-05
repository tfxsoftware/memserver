package com.tfxsoftware.memserver.modules.events;

import org.springframework.web.server.ResponseStatusException;
import com.tfxsoftware.memserver.modules.events.dto.CreateEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;

    /**
     * Creates a new Event with strict validation on financial and chronological data.
     * All events start in the CLOSED status.
     */
    @Transactional
    public Event createEvent(CreateEventDto dto) {
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
                .region(dto.getRegion())
                .type(dto.getType())
                .status(Event.EventStatus.CLOSED)
                .tier(dto.getTier())
                .entryFee(dto.getEntryFee())
                .totalPrizePool(dto.getTotalPrizePool())
                .rankPrizes(dto.getRankPrizes())
                .opensAt(dto.getOpensAt())
                .startsAt(dto.getStartsAt())
                .finishesAt(dto.getFinishesAt())
                .gamesPerBlock(dto.getGamesPerBlock())
                .minutesBetweenGames(dto.getMinutesBetweenGames())
                .minutesBetweenBlocks(dto.getMinutesBetweenBlocks())
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
        return eventRepository.save(event);
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
}