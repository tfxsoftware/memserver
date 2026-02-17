package com.tfxsoftware.memserver.modules.players.dto;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class AddExperienceDto {
    @NotNull
    private HeroRole role;

    @NotNull
    private UUID heroId;

    @NotNull
    @Positive
    private Long amount;
}
