package com.tfxsoftware.memserver.modules.players;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import com.tfxsoftware.memserver.modules.players.dto.PlayerResponse;
import com.tfxsoftware.memserver.modules.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final PlayerRoleMasteryRepository roleMasteryRepository;
    private final MasteryService masteryService;
    private final Random random = new Random();

    // Fixed Economic Values for MVP Generation
    private static final BigDecimal FIXED_MARKET_VALUE = new BigDecimal("5000.00");
    private static final BigDecimal FIXED_SALARY = new BigDecimal("500.00");
    private static final int INITIAL_ROLE_LEVEL = 1;
    private static final long INITIAL_EXPERIENCE = 0L;

    @Transactional(readOnly = true)
    public List<PlayerResponse> getMarketplace() {
        return playerRepository.findAll().stream()
                .filter(Player::getIsListed)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> getOwnedPlayers(User owner) {
        return playerRepository.findByOwnerId(owner.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Player> findById(UUID id) {
        return playerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Player> findAllById(Iterable<UUID> ids) {
        return playerRepository.findAllById(ids);
    }

    @Transactional
    public List<Player> saveAll(Iterable<Player> players) {
        return playerRepository.saveAll(players);
    }

    /**
     * Recruitment Logic: Generates a random "Rookie" and attaches it to the user.
     * Checks if the user has room in their roster (Max 5 players).
     */
    @Transactional
    public Player generateRookie(User owner) {
        // Business Rule: A user cannot have more than 5 players in this recruitment phase
        List<Player> userPlayers = playerRepository.findByOwnerId(owner.getId());
        if (userPlayers.size() >= 5) {
            throw new IllegalStateException("User already has a full roster (5 players). Cannot recruit more rookies.");
        }

        String nickname = "Rookie_" + random.nextInt(10000);

        // 3. Assign a single random trait
        Player.PlayerTrait trait = Player.PlayerTrait.values()[random.nextInt(Player.PlayerTrait.values().length)];

        Player player = Player.builder()
                .nickname(nickname)
                .pictureUrl("https://api.dicebear.com/7.x/pixel-art/svg?seed=" + nickname)
                .traits(Set.of(trait))
                .condition(Player.PlayerCondition.HEALTHY)
                .salary(FIXED_SALARY)
                .marketValue(FIXED_MARKET_VALUE)
                .salaryDaysLeft(7)
                .owner(owner)     // Automatically attach to user
                .isListed(false)  // Not on market since it's owned
                .isStar(false)
                .build();

        player = playerRepository.save(player);

        // Create Role Masteries for all 5 roles
        List<PlayerRoleMastery> roleMasteries = new ArrayList<>();
        for (HeroRole role : HeroRole.values()) {
            PlayerRoleMastery mastery = PlayerRoleMastery.builder()
                    .player(player)
                    .role(role)
                    .level(INITIAL_ROLE_LEVEL)
                    .experience(INITIAL_EXPERIENCE)
                    .build();
            roleMasteries.add(mastery);
        }
        roleMasteryRepository.saveAll(roleMasteries);
        player.setRoleMasteries(roleMasteries);

        log.info("Generated and assigned new rookie player {} to user {}", nickname, owner.getUsername());
        return player;
    }

    @Transactional
    public Player createPlayer(Player dto) {
        Player player = playerRepository.save(dto);

        // Create Role Masteries for all 5 roles
        List<PlayerRoleMastery> roleMasteries = new ArrayList<>();
        for (HeroRole role : HeroRole.values()) {
            PlayerRoleMastery mastery = PlayerRoleMastery.builder()
                    .player(player)
                    .role(role)
                    .level(INITIAL_ROLE_LEVEL)
                    .experience(INITIAL_EXPERIENCE)
                    .build();
            roleMasteries.add(mastery);
        }
        roleMasteryRepository.saveAll(roleMasteries);
        player.setRoleMasteries(roleMasteries);

        log.info("Admin created custom player {}", player.getNickname());
        return player;
    }

    @Transactional
    public void addExperience(Player player, HeroRole role, UUID heroId, long amount) {
        masteryService.addRoleExperience(player, role, amount);
        masteryService.addHeroExperience(player, heroId, amount);
    }

    /**
     * Maps the Player entity to a PlayerResponse DTO.
     * Visibility changed to public for access in controllers.
     */
    public PlayerResponse mapToResponse(Player player) {
        Map<HeroRole, Integer> roleMasteries = new HashMap<>();
        if (player.getRoleMasteries() != null) {
            for (PlayerRoleMastery rm : player.getRoleMasteries()) {
                roleMasteries.put(rm.getRole(), rm.getStrength());
            }
        }

        Map<UUID, Integer> heroMasteries = new HashMap<>();
        if (player.getHeroMasteries() != null) {
            for (PlayerHeroMastery hm : player.getHeroMasteries()) {
                heroMasteries.put(hm.getHeroId(), hm.getLevel());
            }
        }

        return PlayerResponse.builder()
                .id(player.getId())
                .nickname(player.getNickname())
                .pictureUrl(player.getPictureUrl())
                .ownerId(player.getOwner() != null ? player.getOwner().getId() : null)
                .ownerName(player.getOwner() != null ? player.getOwner().getUsername() : "Free Agent")
                .isFreeAgent(player.getOwner() == null)
                .roleMasteries(roleMasteries)
                .championMastery(heroMasteries)
                .traits(player.getTraits())
                .condition(player.getCondition())
                .isStar(player.getIsStar())
                .salary(player.getSalary())
                .salaryDaysLeft(player.getSalaryDaysLeft())
                .marketValue(player.getMarketValue())
                .trainingHeroId(player.getTrainingHeroId())
                .trainingRole(player.getTrainingRole())
                .rosterId(player.getRoster() != null ? player.getRoster().getId() : null)
                .build();
    }
}