package com.tfxsoftware.memserver.modules.players.dto;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import com.tfxsoftware.memserver.modules.players.Player.PlayerCondition;
import com.tfxsoftware.memserver.modules.players.Player.PlayerTrait;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
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
    private Map<HeroRole, BigDecimal> roleMasteries;
    private Map<UUID, Integer> championMastery;
    private PlayerTrait trait;
    private PlayerCondition condition;
    private boolean isStar;

    // Financials & Training
    private BigDecimal salary;
    private BigDecimal marketValue;
    private UUID trainingHeroId;
    private HeroRole trainingRole;
}