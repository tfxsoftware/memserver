package com.tfxsoftware.memserver.modules.players;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import com.tfxsoftware.memserver.modules.players.dto.MasteryLevelExpDto;
import com.tfxsoftware.memserver.modules.players.dto.PlayerResponse;
import com.tfxsoftware.memserver.modules.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    public PlayerResponse getOwnedPlayer(User owner, UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        if (player.getOwner() == null || !player.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this player");
        }

        return mapToResponse(player);
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
     * Checks if the user has room (max 10 players).
     */
    @Transactional
    public Player generateRookie(User owner) {
        // Business Rule: A user cannot have more than 10 players
        List<Player> userPlayers = playerRepository.findByOwnerId(owner.getId());
        if (userPlayers.size() >= 10) {
            throw new IllegalStateException("User already has the maximum of 10 players. Cannot recruit more rookies.");
        }

        String nickname = "Rookie_" + random.nextInt(10000);

        // 3. Assign a single random trait
        Player.PlayerTrait trait = Player.PlayerTrait.values()[random.nextInt(Player.PlayerTrait.values().length)];

        Player player = Player.builder()
                .nickname(nickname)
                .pictureUrl("https://api.dicebear.com/7.x/big-smile/svg?seed=" + nickname)
                .traits(Set.of(trait))
                .condition(Player.PlayerCondition.HEALTHY)
                .salary(FIXED_SALARY)
                .nextSalaryPaymentDate(LocalDateTime.now().plusDays(7))
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
     * Adds experience to both role and hero mastery for a player owned by the given user.
     * Returns the updated player response.
     */
    @Transactional
    public PlayerResponse addExperienceForOwner(User owner, UUID playerId, HeroRole role, UUID heroId, long amount) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        if (player.getOwner() == null || !player.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this player");
        }

        addExperience(player, role, heroId, amount);
        Player updated = playerRepository.findById(playerId).orElse(player);
        return mapToResponse(updated);
    }

    /**
     * Maps the Player entity to a PlayerResponse DTO.
     * Visibility changed to public for access in controllers.
     */
    public PlayerResponse mapToResponse(Player player) {
        Map<HeroRole, MasteryLevelExpDto> roleMasteries = new HashMap<>();
        if (player.getRoleMasteries() != null) {
            for (PlayerRoleMastery rm : player.getRoleMasteries()) {
                roleMasteries.put(rm.getRole(), MasteryLevelExpDto.builder()
                        .level(rm.getLevel())
                        .experience(rm.getExperience())
                        .build());
            }
        }

        Map<UUID, MasteryLevelExpDto> heroMasteries = new HashMap<>();
        if (player.getHeroMasteries() != null) {
            for (PlayerHeroMastery hm : player.getHeroMasteries()) {
                heroMasteries.put(hm.getHeroId(), MasteryLevelExpDto.builder()
                        .level(hm.getLevel())
                        .experience(hm.getExperience())
                        .build());
            }
        }

        return PlayerResponse.builder()
                .id(player.getId())
                .nickname(player.getNickname())
                .pictureUrl(player.getPictureUrl())
                .description(player.getDescription())
                .ownerId(player.getOwner() != null ? player.getOwner().getId() : null)
                .ownerName(player.getOwner() != null ? player.getOwner().getUsername() : "Free Agent")
                .isFreeAgent(player.getOwner() == null)
                .roleMasteries(roleMasteries)
                .heroMastery(heroMasteries)
                .traits(player.getTraits())
                .condition(player.getCondition())
                .isStar(player.getIsStar())
                .salary(player.getSalary())
                .nextSalaryPaymentDate(player.getNextSalaryPaymentDate())
                .trainingHeroId(player.getTrainingHeroId())
                .trainingRole(player.getTrainingRole())
                .rosterId(player.getRoster() != null ? player.getRoster().getId() : null)
                .build();
    }
}