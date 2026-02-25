package com.tfxsoftware.memserver.modules.bootcamps.dto;

import com.tfxsoftware.memserver.modules.rosters.Roster.RosterActivity;
import com.tfxsoftware.memserver.modules.users.User.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveBootcampResponseDto {
    private UUID rosterId;
    private String rosterName;
    private Region rosterRegion;
    private RosterActivity rosterActivity;
    private BigDecimal cohesion;
    private BigDecimal morale;
    private Integer energy;
    private Double strength;
    private LocalDateTime startedAt;
    private LocalDateTime lastTickAt;
    private List<PlayerTrainingConfigResponseDto> configs;
}
