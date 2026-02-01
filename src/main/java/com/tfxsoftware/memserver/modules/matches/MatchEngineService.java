package com.tfxsoftware.memserver.modules.matches;

import com.tfxsoftware.memserver.modules.heroes.Hero;
import com.tfxsoftware.memserver.modules.heroes.HeroService;
import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.rosters.RosterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchEngineService {

    private final MatchRepository matchRepository;
    private final PlayerService playerService;
    private final HeroService heroService;
    private final RosterService rosterService;
    private final MatchResultService matchResultService;

    private static final BigDecimal CLUTCH_THRESHOLD_PERCENT = new BigDecimal("0.05");
    private static final double CLUTCH_PROBABILITY_BONUS = 0.20;

    @Transactional
    public void simulateMatch(UUID matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow();
        if (match.getStatus() != Match.MatchStatus.SCHEDULED) return;

        log.info("Starting simulation for Match: {}", matchId);

        // 1. Resolve Draft
        Map<UUID, Hero> finalizedPicks = resolveDraft(match);
        List<Hero> homeHeroes = match.getHomePickIntentions().stream().map(p -> finalizedPicks.get(p.getPlayerId())).toList();
        List<Hero> awayHeroes = match.getAwayPickIntentions().stream().map(p -> finalizedPicks.get(p.getPlayerId())).toList();

        // 2. Calculate Roster Performances
        RosterPerformance homePerf = calculateRosterPerformance(match.getHomeRosterId(), match.getHomePickIntentions(), finalizedPicks, awayHeroes);
        RosterPerformance awayPerf = calculateRosterPerformance(match.getAwayRosterId(), match.getAwayPickIntentions(), finalizedPicks, homeHeroes);

        // 3. Determine Winner (Probabilistic Clutch Logic)
        UUID winnerId = determineWinner(homePerf, awayPerf, match);

        // 4. Persistence
        saveMatchResult(match, homePerf, awayPerf, winnerId);

        log.info("Match {} simulation complete. Winner: {}", matchId, winnerId);
    }

    private RosterPerformance calculateRosterPerformance(UUID rosterId, List<Match.MatchPick> intentions, Map<UUID, Hero> picks, List<Hero> opponents) {
        Roster roster = rosterService.findById(rosterId).orElseThrow();
        List<Hero> teamHeroes = intentions.stream().map(i -> picks.get(i.getPlayerId())).toList();

        Map<UUID, BigDecimal> playerScores = new HashMap<>();
        boolean hasClutchPlayer = false;
        BigDecimal sumPlayerPerformance = BigDecimal.ZERO;

        for (Match.MatchPick pick : intentions) {
            Player player = playerService.findById(pick.getPlayerId()).orElseThrow();
            Hero hero = picks.get(pick.getPlayerId());

            if (player.getTrait() == Player.PlayerTrait.CLUTCH_FACTOR) {
                hasClutchPlayer = true;
            }

            BigDecimal pPerf = calculatePlayerPerformance(player, hero, pick.getRole());
            playerScores.put(player.getId(), pPerf);
            sumPlayerPerformance = sumPlayerPerformance.add(pPerf);
        }

        BigDecimal counterStrength = calculateCounterStrength(teamHeroes, opponents);
        BigDecimal synergyStrength = calculateSynergyStrength(teamHeroes);

        BigDecimal cohesionMult = BigDecimal.ONE.add(roster.getCohesion().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        BigDecimal moraleMult = BigDecimal.ONE.add(roster.getMorale().subtract(new BigDecimal("5.0")).divide(new BigDecimal("50"), 4, RoundingMode.HALF_UP));

        BigDecimal totalStrength = sumPlayerPerformance
                .add(counterStrength)
                .add(synergyStrength)
                .multiply(cohesionMult)
                .multiply(moraleMult);

        return new RosterPerformance(
                totalStrength,
                playerScores,
                counterStrength,
                synergyStrength,
                roster.getCohesion(),
                roster.getMorale(),
                hasClutchPlayer
        );
    }

    private BigDecimal calculatePlayerPerformance(Player player, Hero hero, Hero.HeroRole role) {
        BigDecimal baseRS = player.getRoleMasteries().getOrDefault(role, BigDecimal.ZERO);
        BigDecimal roleEfficiency = hero.getEfficiencyForRole(role);
        BigDecimal effectiveRS = baseRS.multiply(roleEfficiency);

        BigDecimal cs = BigDecimal.valueOf(player.getHeroMasteries().getOrDefault(hero.getId(), 0));
        BigDecimal metaMult = hero.getMultiplierForRole(role);

        BigDecimal pPower = effectiveRS.multiply(new BigDecimal("0.60"))
                .add(cs.multiply(metaMult).multiply(new BigDecimal("0.40")));

        if (player.getTrait() == Player.PlayerTrait.LONE_WOLF) {
            pPower = pPower.multiply(new BigDecimal("1.10"));
        }

        return pPower.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Probabilistic Winner Logic:
     * 1. Base prob = HomePower / TotalPower.
     * 2. If close (< 5%), shift prob by 20% in favor of the Clutch team.
     * 3. Clamp between 5% and 95%.
     */
    private UUID determineWinner(RosterPerformance home, RosterPerformance away, Match match) {
        BigDecimal homeStr = home.totalStrength();
        BigDecimal awayStr = away.totalStrength();
        BigDecimal total = homeStr.add(awayStr);

        if (total.compareTo(BigDecimal.ZERO) == 0) return match.getHomeRosterId();

        double winProbHome = homeStr.doubleValue() / total.doubleValue();

        // Check for Clutch Window
        BigDecimal diff = homeStr.subtract(awayStr).abs();
        BigDecimal threshold = total.multiply(CLUTCH_THRESHOLD_PERCENT);

        if (diff.compareTo(threshold) < 0) {
            if (home.hasClutchPlayer() && !away.hasClutchPlayer()) {
                winProbHome += CLUTCH_PROBABILITY_BONUS;
                log.info("Clutch bonus applied to Home team (+20%)");
            } else if (away.hasClutchPlayer() && !home.hasClutchPlayer()) {
                winProbHome -= CLUTCH_PROBABILITY_BONUS;
                log.info("Clutch bonus applied to Away team (+20%)");
            }
        }

        // Clamp to ensure there's always a chance for either side
        winProbHome = Math.max(0.05, Math.min(0.95, winProbHome));

        return Math.random() < winProbHome ? match.getHomeRosterId() : match.getAwayRosterId();
    }

    private BigDecimal calculateCounterStrength(List<Hero> team, List<Hero> opponents) {
        int points = 0;
        for (Hero t : team) {
            for (Hero o : opponents) {
                if (t.getArchetype().counters(o.getArchetype())) points++;
            }
        }
        return new BigDecimal(points).multiply(new BigDecimal("5.00"));
    }

    private BigDecimal calculateSynergyStrength(List<Hero> team) {
        int points = 0;
        for (int i = 0; i < team.size(); i++) {
            for (int j = i + 1; j < team.size(); j++) {
                if (team.get(i).getArchetype().synergizesWith(team.get(j).getArchetype())) points++;
            }
        }
        return new BigDecimal(points).multiply(new BigDecimal("3.00"));
    }

    private void saveMatchResult(Match match, RosterPerformance home, RosterPerformance away, UUID winnerId) {
        match.setStatus(Match.MatchStatus.COMPLETED);
        match.setPlayedAt(LocalDateTime.now());
        matchResultService.createResult(match, home, away, winnerId);
        matchRepository.save(match);
    }

    private Map<UUID, Hero> resolveDraft(Match match) {
        Set<UUID> unavailable = Stream.concat(match.getHomeBans().stream(), match.getAwayBans().stream())
                .collect(Collectors.toSet());

        List<DraftEntry> sequence = Stream.concat(
                match.getHomePickIntentions().stream().map(p -> new DraftEntry(p, true)),
                match.getAwayPickIntentions().stream().map(p -> new DraftEntry(p, false))
        ).sorted(Comparator.comparingInt(e -> e.pick().getPickOrder())).toList();

        Map<UUID, Hero> finalPicks = new HashMap<>();
        List<Hero> allHeroes = heroService.findAll();

        for (DraftEntry entry : sequence) {
            Match.MatchPick intent = entry.pick();
            Hero assigned = tryAssign(intent.getPreferredHeroId1(), unavailable, allHeroes);
            if (assigned == null) assigned = tryAssign(intent.getPreferredHeroId2(), unavailable, allHeroes);
            if (assigned == null) assigned = tryAssign(intent.getPreferredHeroId3(), unavailable, allHeroes);
            if (assigned == null) assigned = findBestMetaHero(intent.getRole(), unavailable, allHeroes);

            finalPicks.put(intent.getPlayerId(), assigned);
            unavailable.add(assigned.getId());
        }
        return finalPicks;
    }

    private Hero tryAssign(UUID id, Set<UUID> unavailable, List<Hero> all) {
        if (id == null || unavailable.contains(id)) return null;
        return all.stream().filter(h -> h.getId().equals(id)).findFirst().orElse(null);
    }

    private Hero findBestMetaHero(Hero.HeroRole role, Set<UUID> unavailable, List<Hero> all) {
        return all.stream()
                .filter(h -> !unavailable.contains(h.getId()))
                .filter(h -> h.getPrimaryRole() == role || h.getSecondaryRole() == role)
                .min(Comparator.comparingInt(h -> h.getPrimaryTier().ordinal()))
                .orElseThrow();
    }

    private record DraftEntry(Match.MatchPick pick, boolean isHome) {}

    public record RosterPerformance(
            BigDecimal totalStrength,
            Map<UUID, BigDecimal> playerScores,
            BigDecimal counterStrength,
            BigDecimal synergyStrength,
            BigDecimal cohesionSnapshot,
            BigDecimal moraleSnapshot,
            boolean hasClutchPlayer
    ) {}
}