package com.tfxsoftware.memserver.modules.dashboard.dto;

import com.tfxsoftware.memserver.modules.users.User;
import java.math.BigDecimal;

public record UserProfileDto(
    String username,
    User.Region region,
    BigDecimal balance,
    String organizationImageUrl
) {}
