package com.tfxsoftware.memserver.modules.matches.dto;

import com.tfxsoftware.memserver.modules.matches.Match.MatchStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    UUID homeRosterId,
    UUID awayRosterId,
    MatchStatus status,
    LocalDateTime scheduledTime,
    LocalDateTime playedAt,
    UUID eventId
) {}
