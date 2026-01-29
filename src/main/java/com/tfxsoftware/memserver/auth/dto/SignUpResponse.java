package com.tfxsoftware.memserver.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class SignUpResponse {
    private String message;
}