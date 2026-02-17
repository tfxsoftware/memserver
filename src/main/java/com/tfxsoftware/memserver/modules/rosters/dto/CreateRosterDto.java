package com.tfxsoftware.memserver.modules.rosters.dto;

import com.tfxsoftware.memserver.modules.users.User.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRosterDto {


    @NotBlank
    @Size(max = 20)
    private String name;

    @NotNull
    private Region region;

}
