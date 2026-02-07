package com.tfxsoftware.memserver.modules.users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDto {
    private String organizationName;
    private String organizationImageUrl;
}
