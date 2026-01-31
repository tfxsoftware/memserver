package com.tfxsoftware.memserver.modules.heroes;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "heroes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hero {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    /**
     * Maps a Role to its specific Meta Data for the current season.
     * This allows a hero to have different Tiers and Efficiencies depending on the lane.
     */
    @ElementCollection
    @CollectionTable(name = "hero_role_metadata", joinColumns = @JoinColumn(name = "hero_id"))
    @MapKeyColumn(name = "role")
    @MapKeyEnumerated(EnumType.STRING)
    private Map<HeroRole, RoleMetadata> roleSettings;

    public enum HeroRole {
        TOP, JUNGLE, MID, CARRY, SUPPORT
    }

    public enum MetaTier {
        S, A, B, C, D
    }

    /**
     * Nested class to hold role-specific balance data.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleMetadata {
        @Column(precision = 3, scale = 2)
        private BigDecimal efficiency; // Base capability in this role (e.g. 1.0 or 0.8)

        @Enumerated(EnumType.STRING)
        private MetaTier tier; // Current Meta Tier for this specific role

        @Column(precision = 3, scale = 2)
        private BigDecimal multiplier; // The multiplier applied in the Win Formula (e.g. 1.2 for S-tier)
    }

    /**
     * Helper for the Match Engine to get efficiency.
     */
    public BigDecimal getEfficiencyForRole(HeroRole role) {
        if (roleSettings == null || !roleSettings.containsKey(role)) return BigDecimal.ZERO;
        return roleSettings.get(role).getEfficiency();
    }

    /**
     * Helper for the Match Engine to get the Meta Multiplier.
     */
    public BigDecimal getMultiplierForRole(HeroRole role) {
        if (roleSettings == null || !roleSettings.containsKey(role)) return BigDecimal.ONE;
        return roleSettings.get(role).getMultiplier();
    }
}