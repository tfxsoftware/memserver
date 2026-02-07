package com.tfxsoftware.memserver.modules.events.dto;

import com.tfxsoftware.memserver.modules.events.Event.EventType;
import com.tfxsoftware.memserver.modules.events.Event.Tier;
import com.tfxsoftware.memserver.modules.users.User.Region;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set; // New import

/**
 * DTO for creating an event.
 * Converted to a class to maintain consistency with the rest of the project.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEventDto {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    
    private String imageUrl;
    
    @NotEmpty(message = "At least one region is required") // Changed from NotNull
    private Set<Region> regions; // Changed field to Set

    @NotNull(message = "Event type is required")
    private EventType type;

    @NotNull(message = "Tier is required")
    private Tier tier;

    @NotNull(message = "Entry fee is required")
    @PositiveOrZero
    private BigDecimal entryFee;

    @NotNull(message = "Total prize pool is required")
    @PositiveOrZero
    private BigDecimal totalPrizePool;

    private Map<Integer, BigDecimal> rankPrizes;

    @NotNull(message = "Opening date (opensAt) is required")
    private LocalDateTime opensAt;

    private LocalDateTime startsAt;

    // Pacing
    @NotNull(message = "Games per block is required")
    @PositiveOrZero 
    private Integer gamesPerBlock;

    @NotNull(message = "Minutes between games is required")
    @PositiveOrZero 
    private Integer minutesBetweenGames;

    @NotNull(message = "Minutes between blocks is required")
    @PositiveOrZero 
    private Integer minutesBetweenBlocks;

    @NotNull(message = "Max players is required")
    @Positive(message = "Max players must be a positive number")
    private Integer maxPlayers;

    // League specific (Optional based on type)
    private Integer roundRobinCount;
}