package com.tfxsoftware.memserver.modules.rosters;

import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.users.User;
import com.tfxsoftware.memserver.modules.users.User.Region;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rosters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roster {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RosterActivity activity = RosterActivity.IDLE;

    // --- Team Synergy Stats (0.00 to 10.00) ---

    @Column(precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal cohesion = BigDecimal.ZERO;

    @Column(precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal morale = new BigDecimal("5.00");

    /**
     * One Roster contains many players. 
     * In the game logic, we will limit this to 5 active players.
     */
    @OneToMany(mappedBy = "roster", fetch = FetchType.LAZY)
    private List<Player> players;

    public enum RosterActivity {
        IDLE, BOOTCAMPING, IN_TOURNAMENT
    }
}