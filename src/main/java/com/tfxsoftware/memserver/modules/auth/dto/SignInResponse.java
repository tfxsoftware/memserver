package com.tfxsoftware.memserver.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignInResponse {
    private String token;
}