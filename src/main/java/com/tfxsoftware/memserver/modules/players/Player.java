package com.tfxsoftware.memserver.modules.players;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.users.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String nickname;

    private String pictureUrl;

    // --- Mastery Stats ---

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_role_masteries", joinColumns = @JoinColumn(name = "player_id"))
    @MapKeyColumn(name = "role")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "mastery_value")
    private Map<HeroRole, BigDecimal> roleMasteries;

    @ElementCollection
    @CollectionTable(name = "player_hero_masteries", joinColumns = @JoinColumn(name = "player_id"))
    @MapKeyColumn(name = "hero_id")
    @Column(name = "mastery_value")
    private Map<UUID, Integer> heroMasteries;

    // --- State & Strategy ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlayerCondition condition = PlayerCondition.HEALTHY;

    private UUID trainingHeroId;

    @Enumerated(EnumType.STRING)
    private HeroRole trainingRole;

    @Builder.Default
    private Boolean isStar = false;
    
    @Enumerated(EnumType.STRING)
    private PlayerTrait trait;

    // --- Financials & Ownership ---

    @Column(nullable = false)
    private BigDecimal salary;

    @Column(nullable = false)
    private BigDecimal marketValue;

    @Builder.Default
    private Boolean isListed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roster_id")
    private Roster roster;

    public enum PlayerCondition {
        HEALTHY, SICK, INJURED, BURNT_OUT
    }

    public enum PlayerTrait {
        CLUTCH_FACTOR,  // Can make difference if game too close
        LEADER,         // Boosts team Morale when winning, reduces loss when losing
        LONE_WOLF,      // Increased multipliers, but reduces team Cohesion
        TEAM_PLAYER,    // Increases Roster Cohesion gain rate
        ADAPTIVE,       // Reduces "Off-Role" penalty
        WORKAHOLIC      // Reduces roster energy loss during Bootcamp/tournament
    }
}