package com.tfxsoftware.memserver.modules.rosters.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRosterDto {
    @NotNull(message = "addPlayerIds cannot be null")
    private List<UUID> addPlayerIds;
    @NotNull(message = "removePlayerIds cannot be null")
    private List<UUID> removePlayerIds;
}

