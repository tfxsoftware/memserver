package com.tfxsoftware.memserver.modules.events.dto;

import com.tfxsoftware.memserver.modules.events.Event;
import com.tfxsoftware.memserver.modules.users.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
    private String location;
    private User.Region region;
    private LocalDateTime opensAt;
    private LocalDateTime startsAt;
    private LocalDateTime finishesAt;
    private Event.EventType type;
    private Event.EventStatus status;
    private Event.Tier tier;
    private BigDecimal entryFee;
    private BigDecimal totalPrizePool;
    private Map<Integer, BigDecimal> rankPrizes;
    private Integer gamesPerBlock;
    private Integer minutesBetweenGames;
    private Integer minutesBetweenBlocks;
}
