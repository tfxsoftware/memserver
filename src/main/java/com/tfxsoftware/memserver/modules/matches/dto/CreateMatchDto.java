package com.tfxsoftware.memserver.modules.matches.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateMatchDto(
    @NotNull UUID homeRosterId,
    @NotNull UUID awayRosterId,
    @NotNull LocalDateTime scheduledTime,
    UUID eventId
) {}
