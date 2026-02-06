package com.tfxsoftware.memserver.modules.matches;

import com.tfxsoftware.memserver.modules.heroes.Hero;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchResultService {

    private final MatchResultRepository matchResultRepository;

    @Transactional
    public MatchResult save(MatchResult matchResult) {
        return matchResultRepository.save(matchResult);
    }

    @Transactional(readOnly = true)
    public Optional<MatchResult> findByMatchId(UUID matchId) {
        return matchResultRepository.findById(matchId);
    }

    @Transactional
    public MatchResult createResult(Match match, MatchEngineService.RosterPerformance home, MatchEngineService.RosterPerformance away, UUID winnerId, Map<UUID, Hero> finalizedPicks) {
        java.util.Map<String, Object> playerStats = new java.util.HashMap<>();

        // Helper to populate stats from a RosterPerformance
        populateStats(playerStats, match.getHomePickIntentions(), home, finalizedPicks);
        populateStats(playerStats, match.getAwayPickIntentions(), away, finalizedPicks);

        MatchResult result = MatchResult.builder()
                .matchId(match.getId())
                .winnerRosterId(winnerId)
                .homeTotalPerformance(home.totalStrength())
                .awayTotalPerformance(away.totalStrength())
                .playerStats(playerStats)
                .build();
        return matchResultRepository.save(result);
    }

    private void populateStats(java.util.Map<String, Object> allStats, 
                               java.util.List<Match.MatchPick> intentions, 
                               MatchEngineService.RosterPerformance performance,
                               Map<UUID, Hero> finalizedPicks) {
        for (Match.MatchPick pick : intentions) {
            UUID playerId = pick.getPlayerId();
            Hero hero = finalizedPicks.get(playerId);
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("performancePoints", performance.playerScores().get(playerId));
            stats.put("heroId", hero != null ? hero.getId() : null);
            stats.put("role", pick.getRole());
            allStats.put(playerId.toString(), stats);
        }
    }
}
