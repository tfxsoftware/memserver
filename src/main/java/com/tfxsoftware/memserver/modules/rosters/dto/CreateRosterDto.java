package com.tfxsoftware.memserver.modules.rosters.dto;

import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.users.User.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRosterDto {


    @NotBlank
    @Size(max = 20)
    private String name;

    @NotNull
    private Region region;

    @NotEmpty
    @Size(min = 5, max = 5, message = "A roster must have exactly " + 5 + " players on creation")
    private List<UUID> playerIds;
}
