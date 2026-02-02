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
    private final PostMatchProcessor postMatchProcessor;

    private static final BigDecimal CLUTCH_THRESHOLD_PERCENT = new BigDecimal("0.05");
    private static final double CLUTCH_PROBABILITY_BONUS = 0.20;

    @Transactional
    public void simulateMatch(UUID matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow();
        if (match.getStatus() != Match.MatchStatus.SCHEDULED) {
            log.warn("Match {} is not in SCHEDULED status. Skipping simulation.", matchId);
            return;
        }

        log.info("Starting simulation for Match: {}", matchId);

        // 1. Resolve Draft
        log.info("Step 1: Resolving Draft for Match {}", matchId);
        Map<UUID, Hero> finalizedPicks = resolveDraft(match);
        List<Hero> homeHeroes = match.getHomePickIntentions().stream().map(p -> finalizedPicks.get(p.getPlayerId())).toList();
        List<Hero> awayHeroes = match.getAwayPickIntentions().stream().map(p -> finalizedPicks.get(p.getPlayerId())).toList();
        log.info("Draft resolved. Home picks: {}, Away picks: {}", 
                homeHeroes.stream().map(Hero::getName).toList(), 
                awayHeroes.stream().map(Hero::getName).toList());

        // 2. Calculate Roster Performances
        log.info("Step 2: Calculating roster performances");
        RosterPerformance homePerf = calculateRosterPerformance(match.getHomeRosterId(), match.getHomePickIntentions(), finalizedPicks, awayHeroes);
        RosterPerformance awayPerf = calculateRosterPerformance(match.getAwayRosterId(), match.getAwayPickIntentions(), finalizedPicks, homeHeroes);
        log.info("Performance calculated. Home Strength: {}, Away Strength: {}", homePerf.totalStrength(), awayPerf.totalStrength());

        // 3. Determine Winner (Probabilistic Clutch Logic)
        log.info("Step 3: Determining winner");
        UUID winnerId = determineWinner(homePerf, awayPerf, match);
        log.info("Winner determined: {}", winnerId);

        // 4. Persistence
        log.info("Step 4: Saving match result and updating match status");
        saveMatchResult(match, homePerf, awayPerf, winnerId, finalizedPicks);

        log.info("Match {} simulation complete. Result persisted.", matchId);
    }

    private RosterPerformance calculateRosterPerformance(UUID rosterId, List<Match.MatchPick> intentions, Map<UUID, Hero> picks, List<Hero> opponents) {
        Roster roster = rosterService.findById(rosterId).orElseThrow();
        List<Hero> teamHeroes = intentions.stream().map(i -> picks.get(i.getPlayerId())).toList();
        log.info("Calculating performance for roster: {} ({})", roster.getName(), rosterId);

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
            log.info("Player {} performance: {}", player.getNickname(), pPerf);
        }

        BigDecimal counterStrength = calculateCounterStrength(teamHeroes, opponents);
        BigDecimal synergyStrength = calculateSynergyStrength(teamHeroes);

        BigDecimal cohesionMult = BigDecimal.ONE.add(roster.getCohesion().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        BigDecimal moraleMult = BigDecimal.ONE.add(roster.getMorale().subtract(new BigDecimal("5.0")).divide(new BigDecimal("50"), 4, RoundingMode.HALF_UP));

        log.info("Roster Stats - Cohesion: {} (x{}), Morale: {} (x{}), Counter Strength: {}, Synergy Strength: {}", 
                roster.getCohesion(), cohesionMult, roster.getMorale(), moraleMult, counterStrength, synergyStrength);

        BigDecimal performanceBeforeMults = sumPlayerPerformance.add(counterStrength).add(synergyStrength);
        BigDecimal totalStrength = performanceBeforeMults
                .multiply(cohesionMult)
                .multiply(moraleMult);
        
        log.info("Roster {} Total Strength Calculation: (SumPlayerPerf: {} + Counter: {} + Synergy: {}) * CohesionMult: {} * MoraleMult: {} = {}",
                roster.getName(), sumPlayerPerformance, counterStrength, synergyStrength, cohesionMult, moraleMult, totalStrength);

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
        BigDecimal baseRS = player.getRoleMasteries().stream()
                .filter(rm -> rm.getRole() == role)
                .findFirst()
                .map(rm -> new BigDecimal(rm.getStrength()))
                .orElse(BigDecimal.ONE.setScale(2));

        BigDecimal roleEfficiency = hero.getEfficiencyForRole(role);
        BigDecimal effectiveRS = baseRS.multiply(roleEfficiency);

        BigDecimal cs = player.getHeroMasteries().stream()
                .filter(hm -> hm.getHeroId().equals(hero.getId()))
                .findFirst()
                .map(hm -> new BigDecimal(hm.getLevel()))
                .orElse(BigDecimal.ONE);
        
        BigDecimal metaMult = hero.getMultiplierForRole(role);

        BigDecimal rolePower = effectiveRS.multiply(new BigDecimal("0.60"));
        BigDecimal heroPower = cs.multiply(metaMult).multiply(new BigDecimal("0.40"));
        BigDecimal pPower = rolePower.add(heroPower);

        log.info("Player {} performance calculation: (BaseRS: {} * Eff: {} = EffectiveRS: {}) * 0.60 [{}] + (CS: {} * MetaMult: {}) * 0.40 [{}] = Total: {}",
                player.getNickname(), baseRS, roleEfficiency, effectiveRS, rolePower, cs, metaMult, heroPower, pPower);

        if (player.getTrait() == Player.PlayerTrait.LONE_WOLF) {
            BigDecimal beforeTrait = pPower;
            pPower = pPower.multiply(new BigDecimal("1.10"));
            log.info("Lone Wolf trait applied to player {}: {} * 1.10 = {}", player.getNickname(), beforeTrait, pPower);
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

        log.info("Determining winner: Home Strength: {}, Away Strength: {}, Total Strength: {}", homeStr, awayStr, total);

        if (total.compareTo(BigDecimal.ZERO) == 0) return match.getHomeRosterId();

        double winProbHome = homeStr.doubleValue() / total.doubleValue();
        log.info("Base Win Probability - Home: {}%, Away: {}%", 
                String.format("%.2f", winProbHome * 100), 
                String.format("%.2f", (1 - winProbHome) * 100));

        // Check for Clutch Window
        BigDecimal diff = homeStr.subtract(awayStr).abs();
        BigDecimal threshold = total.multiply(CLUTCH_THRESHOLD_PERCENT);

        log.info("Clutch window check - Diff: {}, Threshold (5% of total): {}", diff, threshold);

        if (diff.compareTo(threshold) < 0) {
            log.info("Match is within Clutch Threshold. Checking for Clutch Factor trait...");
            if (home.hasClutchPlayer() && !away.hasClutchPlayer()) {
                winProbHome += CLUTCH_PROBABILITY_BONUS;
                log.info("Home team has a Clutch player. Bonus +20%. New Home Prob: {}%", String.format("%.2f", winProbHome * 100));
            } else if (away.hasClutchPlayer() && !home.hasClutchPlayer()) {
                winProbHome -= CLUTCH_PROBABILITY_BONUS;
                log.info("Away team has a Clutch player. Bonus +20% to Away. New Home Prob: {}%", String.format("%.2f", winProbHome * 100));
            } else if (home.hasClutchPlayer() && away.hasClutchPlayer()) {
                log.info("Both teams have Clutch players. Bonuses cancel out.");
            } else {
                log.info("No Clutch players found on either team.");
            }
        }

        // Clamp to ensure there's always a chance for either side
        double beforeClamp = winProbHome;
        winProbHome = Math.max(0.05, Math.min(0.95, winProbHome));
        if (beforeClamp != winProbHome) {
            log.info("Probability clamped from {}% to {}%", String.format("%.2f", beforeClamp * 100), String.format("%.2f", winProbHome * 100));
        }
        log.info("Final Win Probability - Home: {}%", String.format("%.2f", winProbHome * 100));

        return Math.random() < winProbHome ? match.getHomeRosterId() : match.getAwayRosterId();
    }

    private BigDecimal calculateCounterStrength(List<Hero> team, List<Hero> opponents) {
        int points = 0;
        for (Hero t : team) {
            for (Hero o : opponents) {
                if (t.getArchetype().counters(o.getArchetype())) {
                    points++;
                    log.info("Counter match: {} counters {} (+1 point)", t.getName(), o.getName());
                }
            }
        }
        BigDecimal result = new BigDecimal(points).multiply(new BigDecimal("5.00"));
        log.info("Total Counter Points: {} * 5.00 = {}", points, result);
        return result;
    }

    private BigDecimal calculateSynergyStrength(List<Hero> team) {
        int points = 0;
        for (int i = 0; i < team.size(); i++) {
            for (int j = i + 1; j < team.size(); j++) {
                if (team.get(i).getArchetype().synergizesWith(team.get(j).getArchetype())) {
                    points++;
                    log.info("Synergy match: {} synergizes with {} (+1 point)", team.get(i).getName(), team.get(j).getName());
                }
            }
        }
        BigDecimal result = new BigDecimal(points).multiply(new BigDecimal("3.00"));
        log.info("Total Synergy Points: {} * 3.00 = {}", points, result);
        return result;
    }

    private void saveMatchResult(Match match, RosterPerformance home, RosterPerformance away, UUID winnerId, Map<UUID, Hero> finalizedPicks) {
        match.setStatus(Match.MatchStatus.COMPLETED);
        match.setPlayedAt(LocalDateTime.now());
        matchResultService.createResult(match, home, away, winnerId);
        matchRepository.save(match);

        postMatchProcessor.process(match, winnerId, finalizedPicks);
    }

    private Map<UUID, Hero> resolveDraft(Match match) {
        Set<UUID> unavailable = Stream.concat(match.getHomeBans().stream(), match.getAwayBans().stream())
                .collect(Collectors.toSet());
        log.info("Starting draft resolution. Unavailable heroes (bans): {}", unavailable);

        List<DraftEntry> sequence = Stream.concat(
                match.getHomePickIntentions().stream().map(p -> new DraftEntry(p, true)),
                match.getAwayPickIntentions().stream().map(p -> new DraftEntry(p, false))
        ).sorted(Comparator.comparingInt(e -> e.pick().getPickOrder())).toList();

        Map<UUID, Hero> finalPicks = new HashMap<>();
        List<Hero> allHeroes = heroService.findAll();

        for (DraftEntry entry : sequence) {
            Match.MatchPick intent = entry.pick();
            String teamLabel = entry.isHome() ? "Home" : "Away";
            log.info("Resolving pick for {} team, Player: {}, Role: {}, Order: {}", 
                    teamLabel, intent.getPlayerId(), intent.getRole(), intent.getPickOrder());

            Hero assigned = tryAssign(intent.getPreferredHeroId1(), unavailable, allHeroes);
            if (assigned != null) {
                log.info("Assigned preferred hero 1: {} for player {}", assigned.getName(), intent.getPlayerId());
            } else {
                assigned = tryAssign(intent.getPreferredHeroId2(), unavailable, allHeroes);
                if (assigned != null) {
                    log.info("Assigned preferred hero 2: {} for player {}", assigned.getName(), intent.getPlayerId());
                } else {
                    assigned = tryAssign(intent.getPreferredHeroId3(), unavailable, allHeroes);
                    if (assigned != null) {
                        log.info("Assigned preferred hero 3: {} for player {}", assigned.getName(), intent.getPlayerId());
                    } else {
                        assigned = findBestMetaHero(intent.getRole(), unavailable, allHeroes);
                        log.info("No preferred heroes available. Assigned best meta hero: {} for player {}", 
                                assigned.getName(), intent.getPlayerId());
                    }
                }
            }

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