package com.tfxsoftware.memserver.modules.matches.dto;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UpdateMatchDraftDto(
    List<UUID> teamBans,
    List<MatchPickDto> pickIntentions
) {
    public record MatchPickDto(
        @NotNull UUID playerId,
        @NotNull HeroRole role,
        @NotNull UUID preferredHeroId1,
        @NotNull UUID preferredHeroId2,
        @NotNull UUID preferredHeroId3,
        @NotNull Integer pickOrder
    ) {}
}
