package com.tfxsoftware.memserver.modules.players;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Mastery for a specific Hero (Luxana, etc.)
 */
@Entity
@Table(name = "player_hero_masteries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerHeroMastery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    private UUID heroId;

    @Builder.Default
    private int level = 1;

    @Builder.Default
    private long experience = 0L;
}
