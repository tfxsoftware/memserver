package com.tfxsoftware.memserver.modules.users.dto;

import com.tfxsoftware.memserver.modules.users.User.Region;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value 
@AllArgsConstructor
public class CreateUserDto {
    @Email
    @NotBlank(message = "Email cannot be empty")
    String email;

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    String username;

    @NotBlank(message = "Hashed password cannot be empty")
    String hashedPassword;

    @NotNull(message = "Region cannot be null")
    Region region;
}

