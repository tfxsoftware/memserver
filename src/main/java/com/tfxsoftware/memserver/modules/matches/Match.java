package com.tfxsoftware.memserver.modules.matches;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID homeRosterId;

    @Column(nullable = false)
    private UUID awayRosterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private LocalDateTime playedAt;

    // --- The Draft Instructions ---

    @ElementCollection
    @CollectionTable(name = "match_bans_home", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "hero_id")
    private List<UUID> homeBans;

    @ElementCollection
    @CollectionTable(name = "match_bans_away", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "hero_id")
    private List<UUID> awayBans;

    /**
     * Storing the manager's pick preferences for the simulation.
     */
    @ElementCollection
    @CollectionTable(name = "match_picks_home", joinColumns = @JoinColumn(name = "match_id"))
    private List<MatchPick> homePickIntentions;

    @ElementCollection
    @CollectionTable(name = "match_picks_away", joinColumns = @JoinColumn(name = "match_id"))
    private List<MatchPick> awayPickIntentions;

    /**
     * status for async drafting workflow.
     */
    public enum MatchStatus {
        SCHEDULED,
        COMPLETED,
        CANCELLED
    }

    /**
     * Embeddable helper to store hero preferences for each player.
     * Managers choose 3 heroes in order of preference.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchPick {
        private UUID playerId;

        @Enumerated(EnumType.STRING)
        private HeroRole role;

        // The 3 preferred heroes (ordered by priority)
        private UUID preferredHeroId1;
        private UUID preferredHeroId2;
        private UUID preferredHeroId3;

        private Integer pickOrder; // 1 to 5 (Position in the team's draft sequence)
    }
}