package com.tfxsoftware.memserver.modules.rosters.dto;

import com.tfxsoftware.memserver.modules.players.dto.PlayerResponse;
import com.tfxsoftware.memserver.modules.users.User.Region;
import com.tfxsoftware.memserver.modules.rosters.Roster.RosterActivity;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RosterResponse {
    private UUID id;
    private String name;
    private Region region;
    private RosterActivity activity;
    private BigDecimal cohesion;
    private BigDecimal morale;
    private Integer energy;
    private Double strength;
    private List<PlayerResponse> players;
}
