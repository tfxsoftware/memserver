package com.tfxsoftware.memserver.modules.dashboard.dto;

import java.util.List;

public record DashboardResponseDto(
    UserProfileDto profile,
    List<RosterVitalsDto> rosters,
    List<PlayerPedestalDto> players,
    UpcomingMatchDto nextMatch
) {}
