package com.tfxsoftware.memserver.modules.bootcamps.dto;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTrainingConfigResponseDto {
    private UUID playerId;
    private HeroRole targetRole;
    private UUID primaryHeroId;
    private UUID secondaryHeroId1;
    private UUID secondaryHeroId2;
}
