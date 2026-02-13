package com.tfxsoftware.memserver.modules.dashboard.dto;

import com.tfxsoftware.memserver.modules.rosters.Roster;
import java.math.BigDecimal;
import java.util.UUID;

public record RosterVitalsDto(
    UUID id,
    String name,
    Integer energy,
    BigDecimal morale,
    BigDecimal cohesion,
    Roster.RosterActivity activity
) {}
