package com.tfxsoftware.memserver.modules.bootcamps;

import org.springframework.web.server.ResponseStatusException;
import com.tfxsoftware.memserver.modules.bootcamps.dto.CreateBootcampSessionDto;
import com.tfxsoftware.memserver.modules.players.MasteryService;
import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.rosters.RosterService;
import com.tfxsoftware.memserver.modules.users.User;

import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BootcampService {
    
    private final RosterService rosterService;
    private final BootcampSessionRepository sessionRepository;
    private final PlayerService playerService;
    private final MasteryService masteryService;

    private static final long BASE_PRIMARY_HERO_XP = 100L;
    private static final long BASE_SECONDARY_HERO_XP = 50L;
    private static final long BASE_ADAPTIVE_HERO_XP = 80L;
    private static final long BASE_ROLE_XP = 50L;
    private static final int TICK_HOURS = 6;
    private static final int BASE_ENERGY_COST_PER_TICK = 10;
    private static final int WORKAHOLIC_ENERGY_REDUCTION = 1;
    private static final java.math.BigDecimal BASE_COHESION_GAIN = new java.math.BigDecimal("0.1");
    private static final java.math.BigDecimal LEADER_COHESION_GAIN = new java.math.BigDecimal("0.2");
    private static final java.math.BigDecimal MAX_COHESION = new java.math.BigDecimal("10.00");

    /**
     * Starts the bootcamp and creates the transient configuration.
     */
    @Transactional
    public void startBootcamp(User owner, UUID rosterId, CreateBootcampSessionDto request) {
        Roster roster = rosterService.findById(rosterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster not found"));

        if (!roster.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this roster");
        }

        if (roster.getActivity() != Roster.RosterActivity.IDLE) {
            throw new IllegalStateException("Roster must be IDLE to start bootcamp");
        }

        LocalDateTime now = LocalDateTime.now();
        
        // 1. Create the Session
        BootcampSession session = BootcampSession.builder()
                .roster(roster)
                .startedAt(now)
                .lastTickAt(now)
                .build();

        // 2. Map DTOs to Entities using the wrapper list
        List<PlayerTrainingConfig> entities = request.configs().stream().map(dto -> 
            PlayerTrainingConfig.builder()
                .session(session)
                .playerId(dto.getPlayerId())
                .targetRole(dto.getTargetRole())
                .primaryHeroId(dto.getPrimaryHeroId())
                .secondaryHeroId1(dto.getSecondaryHeroId1())
                .secondaryHeroId2(dto.getSecondaryHeroId2())
                .build()
        ).toList();

        session.setPlayerConfigs(entities);
        
        
        // 3. Update Roster Status
        roster.setActivity(Roster.RosterActivity.BOOTCAMP);
        
        sessionRepository.save(session);
        rosterService.save(roster);
        
        log.info("Bootcamp session created for roster {}", rosterId);
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void processBootcampTicks() {
        log.info("Processing XP ticks");
        LocalDateTime threshold = LocalDateTime.now().minusHours(TICK_HOURS);
        List<BootcampSession> activeSessions = sessionRepository.findAllReadyForTick(threshold);
        LocalDateTime now = LocalDateTime.now();

        for (BootcampSession session : activeSessions) {
            Roster roster = session.getRoster();
            if (roster.getEnergy() < BASE_ENERGY_COST_PER_TICK) {
                log.info("Roster {} has low energy ({}). Stopping bootcamp.", roster.getId(), roster.getEnergy());
                stopBootcampInternal(roster);
                continue;
            }
            applyXpTick(session);
            session.setLastTickAt(now);
            sessionRepository.save(session);
        }
    }

    private void applyXpTick(BootcampSession session) {
        Roster roster = session.getRoster();
        double strength = rosterService.calculateRosterStrength(roster);

        // Deduct energy
        int workaholics = 0;
        if (roster.getPlayers() != null) {
            workaholics = (int) roster.getPlayers().stream()
                    .filter(p -> p.getTraits().contains(Player.PlayerTrait.WORKAHOLIC))
                    .count();
        }
        int energyDeduction = Math.max(0, BASE_ENERGY_COST_PER_TICK - (workaholics * WORKAHOLIC_ENERGY_REDUCTION));

        int currentEnergy = roster.getEnergy() != null ? roster.getEnergy() : 0;
        roster.setEnergy(Math.max(0, currentEnergy - energyDeduction));

        // Increase cohesion
        boolean hasLeader = false;
        if (roster.getPlayers() != null) {
            hasLeader = roster.getPlayers().stream()
                    .anyMatch(p -> p.getTraits().contains(Player.PlayerTrait.LEADER));
        }
        java.math.BigDecimal gain = hasLeader ? LEADER_COHESION_GAIN : BASE_COHESION_GAIN;
        java.math.BigDecimal newCohesion = roster.getCohesion().add(gain);
        if (newCohesion.compareTo(MAX_COHESION) > 0) {
            newCohesion = MAX_COHESION;
        }
        roster.setCohesion(newCohesion);

        rosterService.save(roster);

        for (PlayerTrainingConfig config : session.getPlayerConfigs()) {
            Player player = playerService.findById(config.getPlayerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found in bootcamp config"));

            long roleXp = (long) (BASE_ROLE_XP * strength);
            masteryService.addRoleExperience(player, config.getTargetRole(), roleXp);

            boolean isAdaptive = player.getTraits().contains(Player.PlayerTrait.ADAPTIVE);
            long primaryBase = isAdaptive ? BASE_ADAPTIVE_HERO_XP : BASE_PRIMARY_HERO_XP;
            long secondaryBase = isAdaptive ? BASE_ADAPTIVE_HERO_XP : BASE_SECONDARY_HERO_XP;

            if (config.getPrimaryHeroId() != null) {
                masteryService.addHeroExperience(player, config.getPrimaryHeroId(), (long) (primaryBase * strength));
            }
            if (config.getSecondaryHeroId1() != null) {
                masteryService.addHeroExperience(player, config.getSecondaryHeroId1(), (long) (secondaryBase * strength));
            }
            if (config.getSecondaryHeroId2() != null) {
                masteryService.addHeroExperience(player, config.getSecondaryHeroId2(), (long) (secondaryBase * strength));
            }
        }
    }

    private void stopBootcampInternal(Roster roster) {
        sessionRepository.deleteById(roster.getId()); // Cascades to configs
        roster.setActivity(Roster.RosterActivity.IDLE);
        rosterService.save(roster);
    }

    @Transactional
    public void stopBootcamp(User owner, UUID rosterId) {
        Roster roster = rosterService.findById(rosterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster not found"));

        if (!roster.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this roster");
        }

        stopBootcampInternal(roster);
    }
}