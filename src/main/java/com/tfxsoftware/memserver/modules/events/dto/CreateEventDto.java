package com.tfxsoftware.memserver.modules.events.dto;

import com.tfxsoftware.memserver.modules.events.Event;
import com.tfxsoftware.memserver.modules.users.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventDto {
    @NotBlank(message = "Event name cannot be blank")
    private String name;
    @NotNull(message = "Event type is required")
    private Event.EventType type;
    @NotNull(message = "Event tier is required")
    private Event.Tier tier;
    @NotNull(message = "Event region is required")
    private User.Region region;

    private String description;

    @NotNull(message = "Entry fee is required")
    private BigDecimal entryFee;

    @NotNull(message = "Total prize pool is required")
    private BigDecimal totalPrizePool;

    private Map<Integer, BigDecimal> rankPrizes;

    @NotNull(message = "Games per block is required")
    private Integer gamesPerBlock;

    @NotNull(message = "Minutes between games is required")
    private Integer minutesBetweenGames;

    @NotNull(message = "Minutes between blocks is required")
    private Integer minutesBetweenBlocks;

    // Lifecycle variables
    @NotNull(message = "opensAt field is required")
    private LocalDateTime opensAt; // Conditional validation to be handled in service
}