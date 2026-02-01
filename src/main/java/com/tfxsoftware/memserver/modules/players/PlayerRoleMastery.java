package com.tfxsoftware.memserver.modules.players;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Mastery for a specific Role (TOP, MID, etc.)
 */
@Entity
@Table(name = "player_role_masteries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerRoleMastery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Enumerated(EnumType.STRING)
    private HeroRole role;

    @Builder.Default
    private int level = 1;

    @Builder.Default
    private long experience = 0L;

    /**
     * Converts level into the numeric strength used by the Match Engine.
     * Example: Level 10 = 10 Strength.
     */
    public int getStrength() {
        return level;
    }
}
