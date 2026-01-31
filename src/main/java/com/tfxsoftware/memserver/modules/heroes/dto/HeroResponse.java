package com.tfxsoftware.memserver.modules.heroes.dto;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import com.tfxsoftware.memserver.modules.heroes.Hero.MetaTier;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Output DTO: Flattened structure for easier consumption by the React frontend.
 * Provides pre-calculated multipliers for the Primary and Secondary roles.
 */
@Data
@Builder
public class HeroResponse {
    private UUID id;
    private String name;
    private String pictureUrl;
    
    // Primary Role Info
    private HeroRole primaryRole;
    private MetaTier primaryTier;
    
    // Secondary Role Info (Null if the hero is a specialist)
    private HeroRole secondaryRole;
    private MetaTier secondaryTier;
}