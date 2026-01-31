package com.tfxsoftware.memserver.modules.users.dto;

import com.tfxsoftware.memserver.modules.users.User.Region;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value 
@AllArgsConstructor
public class CreateUserDto {
    String email;
    String username;
    String hashedPassword;
    Region region;
}
