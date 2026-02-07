package com.tfxsoftware.memserver.modules.players.dto;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import com.tfxsoftware.memserver.modules.players.Player.PlayerCondition;
import com.tfxsoftware.memserver.modules.players.Player.PlayerTrait;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Output DTO for Player data.
 * Updated to match the refined MVP structure (singular trait, flattened stats).
 */
@Data
@Builder
public class PlayerResponse {
    private UUID id;
    private String nickname;
    private String pictureUrl;
    
    // Ownership
    private UUID ownerId;
    private String ownerName;
    private boolean isFreeAgent;

    // Stats & Strategy
    private boolean isScouted;
    private Map<HeroRole, Integer> roleMasteries;
    private Map<UUID, Integer> heroMastery;
    private java.util.Set<PlayerTrait> traits;
    private PlayerCondition condition;
    private boolean isStar;

    // Financials & Training
    private BigDecimal salary;
    private LocalDateTime nextSalaryPaymentDate;
    private UUID trainingHeroId;
    private HeroRole trainingRole;
    private UUID rosterId;
}