package com.tfxsoftware.memserver.modules.scenarios.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateScenarioDto {
    @NotNull
    private UUID user1Id;
    @NotNull
    private UUID user2Id;
}
