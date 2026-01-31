package com.tfxsoftware.memserver.modules.heroes;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
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

    @Column(nullable = true)
    private String pictureUrl;

    // --- Primary Role Settings ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HeroRole primaryRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetaTier primaryTier;

    // --- Secondary Role Settings (Optional) ---
    @Enumerated(EnumType.STRING)
    private HeroRole secondaryRole;

    @Enumerated(EnumType.STRING)
    private MetaTier secondaryTier;

    public enum HeroRole {
        TOP, JUNGLE, MID, ADC, SUPPORT
    }

    public enum MetaTier {
        S, A, B, C, D;

        public BigDecimal getMultiplier() {
            return switch (this) {
                case S -> new BigDecimal("1.20");
                case A -> new BigDecimal("1.0");
                case B -> new BigDecimal("0.80");
                case C -> new BigDecimal("0.60");
                case D -> new BigDecimal("0.30");
            };
        }
    }

    /**
     * Simulation Logic: Calculates efficiency based on the selected role.
     */
    public BigDecimal getEfficiencyForRole(HeroRole role) {
        if (role == primaryRole) return new BigDecimal("1.00");
        if (role == secondaryRole) return new BigDecimal("0.80");
        return new BigDecimal("0.10"); // Emergency pick
    }

    /**
     * Simulation Logic: Gets multiplier based on role tier.
     */
    public BigDecimal getMultiplierForRole(HeroRole role) {
        if (role == primaryRole) return primaryTier.getMultiplier();
        if (role == secondaryRole && secondaryTier != null) return secondaryTier.getMultiplier();
        return BigDecimal.ONE; // Neutral for undefined roles
    }
}