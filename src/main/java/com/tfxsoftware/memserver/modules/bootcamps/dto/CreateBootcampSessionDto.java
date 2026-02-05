package com.tfxsoftware.memserver.modules.bootcamps.dto;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Main wrapper for starting a bootcamp session.
 * Using records for conciseness and immutability.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBootcampSessionDto {
    @NotEmpty(message = "Bootcamp must include at least one player configuration")
    @Valid
    private List<PlayerTrainingConfigDto> configs;

    public List<PlayerTrainingConfigDto> configs() {
        return configs;
    }

    /**
     * Individual training configuration for a player.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerTrainingConfigDto {
        @NotNull(message = "Player ID is required")
        private UUID playerId;

        @NotNull(message = "Target role is required")
        private HeroRole targetRole;

        @NotNull(message = "Primary hero is required")
        private UUID primaryHeroId;

        private UUID secondaryHeroId1;
        private UUID secondaryHeroId2;
    }
}