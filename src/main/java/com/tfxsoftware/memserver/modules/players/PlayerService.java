package com.tfxsoftware.memserver.modules.players;

import com.tfxsoftware.memserver.modules.heroes.Hero;
import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import com.tfxsoftware.memserver.modules.heroes.HeroRepository;
import com.tfxsoftware.memserver.modules.players.dto.PlayerResponse;
import com.tfxsoftware.memserver.modules.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final HeroRepository heroRepository;
    private final Random random = new Random();

    // Fixed Economic Values for MVP Generation
    private static final BigDecimal FIXED_MARKET_VALUE = new BigDecimal("5000.00");
    private static final BigDecimal FIXED_SALARY = new BigDecimal("500.00");
    private static final BigDecimal INITIAL_ROLE_MASTERY = BigDecimal.ZERO;
    private static final int INITIAL_HERO_MASTERY_VALUE = 10;

    @Transactional(readOnly = true)
    public List<PlayerResponse> getMarketplace() {
        return playerRepository.findAll().stream()
                .filter(Player::getIsListed)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recruitment Logic: Generates a random "Rookie" and attaches it to the user.
     * Checks if the user has room in their roster (Max 5 players).
     */
    @Transactional
    public Player generateRandomPlayer(User owner) {
        // Business Rule: A user cannot have more than 5 players in this recruitment phase
        List<Player> userPlayers = playerRepository.findByOwnerId(owner.getId());
        if (userPlayers.size() >= 5) {
            throw new IllegalStateException("User already has a full roster (5 players). Cannot recruit more rookies.");
        }

        String nickname = "Rookie_" + random.nextInt(10000);
        
        // 1. Fixed Role Masteries (Setting all roles to 0.0)
        Map<HeroRole, BigDecimal> roleMasteries = new HashMap<>();
        for (HeroRole role : HeroRole.values()) {
            roleMasteries.put(role, INITIAL_ROLE_MASTERY);
        }

        // 2. Random Hero Masteries (Exactly 3 heroes with 10 mastery each)
        Map<UUID, Integer> heroMasteries = new HashMap<>();
        List<Hero> allHeroes = heroRepository.findAll();
        if (!allHeroes.isEmpty()) {
            Collections.shuffle(allHeroes);
            allHeroes.stream()
                .limit(3)
                .forEach(hero -> heroMasteries.put(hero.getId(), INITIAL_HERO_MASTERY_VALUE));
        }

        // 3. Assign a single random trait
        Player.PlayerTrait trait = Player.PlayerTrait.values()[random.nextInt(Player.PlayerTrait.values().length)];

        Player player = Player.builder()
                .nickname(nickname)
                .pictureUrl("https://api.dicebear.com/7.x/pixel-art/svg?seed=" + nickname)
                .roleMasteries(roleMasteries)
                .heroMasteries(heroMasteries)
                .trait(trait)
                .condition(Player.PlayerCondition.HEALTHY)
                .salary(FIXED_SALARY)
                .marketValue(FIXED_MARKET_VALUE)
                .salaryDaysLeft(7)
                .owner(owner)     // Automatically attach to user
                .isListed(false)  // Not on market since it's owned
                .isStar(false)
                .build();

        log.info("Generated and assigned new rookie player {} to user {}", nickname, owner.getUsername());
        return playerRepository.save(player);
    }

    /**
     * Admin functionality: Manually create a player with full customization.
     */
    @Transactional
    public Player createPlayer(Player player) {
        log.info("Admin manually creating player: {}", player.getNickname());
        return playerRepository.save(player);
    }

    /**
     * Maps the Player entity to a PlayerResponse DTO.
     * Visibility changed to public for access in controllers.
     */
    public PlayerResponse mapToResponse(Player player) {
        return PlayerResponse.builder()
                .id(player.getId())
                .nickname(player.getNickname())
                .pictureUrl(player.getPictureUrl())
                .ownerId(player.getOwner() != null ? player.getOwner().getId() : null)
                .ownerName(player.getOwner() != null ? player.getOwner().getUsername() : "Free Agent")
                .isFreeAgent(player.getOwner() == null)
                // Detach from Hibernate by creating a new HashMap copy
                .roleMasteries(player.getRoleMasteries() != null ? new HashMap<>(player.getRoleMasteries()) : new HashMap<>())
                .championMastery(player.getHeroMasteries() != null ? new HashMap<>(player.getHeroMasteries()) : new HashMap<>())
                .trait(player.getTrait())
                .condition(player.getCondition())
                .isStar(player.getIsStar())
                .salary(player.getSalary())
                .salaryDaysLeft(player.getSalaryDaysLeft())
                .marketValue(player.getMarketValue())
                .trainingHeroId(player.getTrainingHeroId())
                .trainingRole(player.getTrainingRole())
                .build();
    }
}