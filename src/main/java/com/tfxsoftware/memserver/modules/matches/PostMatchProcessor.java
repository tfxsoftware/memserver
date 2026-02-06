package com.tfxsoftware.memserver.modules.matches;

import com.tfxsoftware.memserver.modules.heroes.Hero;
import com.tfxsoftware.memserver.modules.players.MasteryService;
import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.rosters.RosterRepository;
import com.tfxsoftware.memserver.modules.rosters.RosterService;
import com.tfxsoftware.memserver.modules.events.Event;
import com.tfxsoftware.memserver.modules.events.league.LeagueStanding;
import com.tfxsoftware.memserver.modules.events.league.LeagueStandingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostMatchProcessor {

    private final PlayerService playerService;
    private final MasteryService masteryService;
    private final RosterService rosterService;
    private final RosterRepository rosterRepository;
    private final LeagueStandingRepository leagueStandingRepository;

    @Transactional
    public void process(Match match, UUID winnerId, Map<UUID, Hero> finalizedPicks) {
        log.info("Starting PostMatchProcessor for match {}. Winner: {}", match.getId(), winnerId);

        boolean homeWon = match.getHomeRosterId().equals(winnerId);
        Roster homeRoster = rosterService.findById(match.getHomeRosterId())
                .orElseThrow(() -> new IllegalStateException("Home roster not found"));
        Roster awayRoster = rosterService.findById(match.getAwayRosterId())
                .orElseThrow(() -> new IllegalStateException("Away roster not found"));

        log.info("Processing home roster: {} (Won: {})", homeRoster.getName(), homeWon);
        updateRosterStats(homeRoster, match.getHomePickIntentions(), homeWon);
        
        log.info("Processing away roster: {} (Won: {})", awayRoster.getName(), !homeWon);
        updateRosterStats(awayRoster, match.getAwayPickIntentions(), !homeWon);

        log.info("Processing players for home roster");
        processPlayers(match.getHomePickIntentions(), finalizedPicks, homeWon);
        
        log.info("Processing players for away roster");
        processPlayers(match.getAwayPickIntentions(), finalizedPicks, !homeWon);

        rosterRepository.save(homeRoster);
        rosterRepository.save(awayRoster);

        // Update League Standings if applicable
        if (match.getEvent() != null && match.getEvent().getType() == Event.EventType.LEAGUE) {
            updateLeagueStandings(match.getEvent().getId(), homeRoster.getId(), awayRoster.getId(), winnerId);
        }

        log.info("PostMatchProcessor finished for match {}", match.getId());
    }

    private void updateLeagueStandings(UUID eventId, UUID homeRosterId, UUID awayRosterId, UUID winnerId) {
        log.info("Updating league standings for event {}", eventId);
        
        LeagueStanding homeStanding = leagueStandingRepository.findByLeagueEventIdAndRosterId(eventId, homeRosterId)
                .orElseThrow(() -> new IllegalStateException("Standing not found for home roster in league " + eventId));
        LeagueStanding awayStanding = leagueStandingRepository.findByLeagueEventIdAndRosterId(eventId, awayRosterId)
                .orElseThrow(() -> new IllegalStateException("Standing not found for away roster in league " + eventId));

        if (winnerId.equals(homeRosterId)) {
            homeStanding.setWins(homeStanding.getWins() + 1);
            awayStanding.setLosses(awayStanding.getLosses() + 1);
        } else {
            awayStanding.setWins(awayStanding.getWins() + 1);
            homeStanding.setLosses(homeStanding.getLosses() + 1);
        }

        leagueStandingRepository.save(homeStanding);
        leagueStandingRepository.save(awayStanding);
        
        recalculatePositions(eventId);
        
        log.info("League standings updated: {} (W: {}, L: {}), {} (W: {}, L: {})",
                homeRosterId, homeStanding.getWins(), homeStanding.getLosses(),
                awayRosterId, awayStanding.getWins(), awayStanding.getLosses());
    }

    private void recalculatePositions(UUID eventId) {
        List<LeagueStanding> standings = leagueStandingRepository.findAllByLeagueEventIdOrderByWinsDesc(eventId);
        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setPosition(i + 1);
        }
        leagueStandingRepository.saveAll(standings);
    }

    private void processPlayers(List<Match.MatchPick> picks, Map<UUID, Hero> finalizedPicks, boolean won) {
        long baseExp = won ? 100L : 150L;
        log.info("Base XP gain: {} (Won: {})", baseExp, won);

        for (Match.MatchPick pick : picks) {
            playerService.findById(pick.getPlayerId()).ifPresent(player -> {
                Hero hero = finalizedPicks.get(pick.getPlayerId());
                
                long heroExp = baseExp;
                if (!won && player.getTraits().contains(Player.PlayerTrait.ADAPTIVE)) {
                    heroExp = (long) (heroExp * 1.5);
                    log.info("Player {} has ADAPTIVE trait. Hero XP multiplier applied: {} -> {}", 
                            player.getNickname(), baseExp, heroExp);
                }

                log.info("Applying XP to Player {}: Role {} (+{} XP), Hero {} (+{} XP)", 
                        player.getNickname(), pick.getRole(), baseExp, hero.getName(), heroExp);
                
                masteryService.addRoleExperience(player, pick.getRole(), baseExp);
                masteryService.addHeroExperience(player, hero.getId(), heroExp);
            });
        }
    }

    private void updateRosterStats(Roster roster, List<Match.MatchPick> picks, boolean won) {
        List<Player> players = picks.stream()
                .map(p -> playerService.findById(p.getPlayerId()).orElseThrow())
                .toList();

        // Morale
        BigDecimal oldMorale = roster.getMorale();
        BigDecimal moraleDelta = won ? new BigDecimal("0.5") : new BigDecimal("-0.5");
        boolean hasLeader = players.stream().anyMatch(p -> p.getTraits().contains(Player.PlayerTrait.LEADER));
        if (hasLeader) {
            moraleDelta = won ? new BigDecimal("0.75") : new BigDecimal("-0.25");
            log.info("LEADER trait detected in roster {}. Morale delta adjusted to {}", roster.getName(), moraleDelta);
        }
        roster.setMorale(clamp(roster.getMorale().add(moraleDelta), BigDecimal.ZERO, new BigDecimal("10.00")));
        log.info("Roster {} Morale updated: {} -> {} (delta: {})", 
                roster.getName(), oldMorale, roster.getMorale(), moraleDelta);

        // Cohesion
        BigDecimal oldCohesion = roster.getCohesion();
        BigDecimal cohesionDelta = won ? new BigDecimal("0.2") : new BigDecimal("0.1");
        long teamPlayers = players.stream().filter(p -> p.getTraits().contains(Player.PlayerTrait.TEAM_PLAYER)).count();
        long loneWolves = players.stream().filter(p -> p.getTraits().contains(Player.PlayerTrait.LONE_WOLF)).count();
        
        BigDecimal traitBonus = new BigDecimal("0.05").multiply(new BigDecimal(teamPlayers));
        BigDecimal traitPenalty = new BigDecimal("0.05").multiply(new BigDecimal(loneWolves));
        
        cohesionDelta = cohesionDelta.add(traitBonus).subtract(traitPenalty);
        
        log.info("Roster {} Cohesion factors: Base={}, TeamPlayers={}, LoneWolves={}, FinalDelta={}", 
                roster.getName(), won ? "0.2" : "0.1", teamPlayers, loneWolves, cohesionDelta);
        
        roster.setCohesion(clamp(roster.getCohesion().add(cohesionDelta), BigDecimal.ZERO, new BigDecimal("10.00")));
        log.info("Roster {} Cohesion updated: {} -> {}", roster.getName(), oldCohesion, roster.getCohesion());

        // Energy
        int oldEnergy = roster.getEnergy();
        int energyLoss = -15;
        long workaholics = players.stream().filter(p -> p.getTraits().contains(Player.PlayerTrait.WORKAHOLIC)).count();
        energyLoss += (int) (workaholics);
        
        roster.setEnergy(Math.max(0, roster.getEnergy() + energyLoss));
        log.info("Roster {} Energy updated: {} -> {} (Loss: {}, Workaholics: {})", 
                roster.getName(), oldEnergy, roster.getEnergy(), energyLoss, workaholics);
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) return min;
        if (value.compareTo(max) > 0) return max;
        return value;
    }
}
