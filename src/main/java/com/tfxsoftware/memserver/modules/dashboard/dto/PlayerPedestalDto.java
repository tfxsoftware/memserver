package com.tfxsoftware.memserver.modules.dashboard.dto;

import com.tfxsoftware.memserver.modules.players.Player;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record PlayerPedestalDto(
    UUID id,
    String nickname,
    String pictureUrl,
    String rosterName,
    Player.PlayerCondition condition,
    Set<Player.PlayerTrait> traits,
    BigDecimal salary,
    LocalDateTime nextSalaryPaymentDate,
    Boolean isStar
) {}
