package com.tfxsoftware.memserver.modules.users.dto;

import com.tfxsoftware.memserver.modules.users.User.Region;
import com.tfxsoftware.memserver.modules.users.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String username;
    private BigDecimal balance;
    private UserRole role;
    private Region region;
    private String organizationName;
    private String organizationImageUrl;
}
