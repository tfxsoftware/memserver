package com.tfxsoftware.memserver.modules.users.dto;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value 
@AllArgsConstructor
public class CreateUserDto {
    String email;
    String username;
    String hashedPassword;
}
