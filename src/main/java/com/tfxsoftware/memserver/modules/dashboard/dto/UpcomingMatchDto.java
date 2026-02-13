package com.tfxsoftware.memserver.modules.dashboard.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpcomingMatchDto(
    UUID matchId,
    String opponentName,
    LocalDateTime scheduledTime,
    String eventName
) {}
