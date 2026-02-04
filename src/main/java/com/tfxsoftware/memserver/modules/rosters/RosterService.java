package com.tfxsoftware.memserver.modules.rosters;

import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.players.PlayerHeroMastery;
import com.tfxsoftware.memserver.modules.players.PlayerRoleMastery;
import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.rosters.dto.CreateRosterDto;
import com.tfxsoftware.memserver.modules.rosters.dto.RosterResponse;
import com.tfxsoftware.memserver.modules.rosters.dto.UpdateRosterDto;
import com.tfxsoftware.memserver.modules.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RosterService {

    private final RosterRepository rosterRepository;
    private final PlayerService playerService;

    @Transactional(readOnly = true)
    public Optional<Roster> findById(UUID id) {
        return rosterRepository.findById(id);
    }

    @Transactional
    public RosterResponse createRoster(User owner, CreateRosterDto dto) {
        if (rosterRepository.existsByOwnerIdAndRegion(owner.getId(), dto.getRegion())) {
            throw new IllegalArgumentException("You already have a roster in this region.");
        }

        boolean hasRoster = rosterRepository.existsByOwnerId(owner.getId());

        if (!hasRoster && !dto.getRegion().equals(owner.getRegion())) {
            throw new IllegalArgumentException("The first roster must be in the same region as the user.");
        }

        if (dto.getPlayerIds().size() != 5) {
            throw new IllegalArgumentException("A roster must have exactly 5 players.");
        }

        List<Player> players = playerService.findAllById(dto.getPlayerIds());

        if (players.size() != dto.getPlayerIds().size()) {
            throw new IllegalArgumentException("One or more players not found.");
        }

        for (Player player : players) {
            if (player.getOwner() == null || !player.getOwner().getId().equals(owner.getId())) {
                throw new IllegalArgumentException("Player " + player.getNickname() + " does not belong to you.");
            }
            if (player.getRoster() != null) {
                throw new IllegalArgumentException("Player " + player.getNickname() + " is already assigned to a roster.");
            }
        }

        Roster roster = Roster.builder()
                .name(dto.getName())
                .region(dto.getRegion())
                .owner(owner)
                .players(players)
                .build();

        Roster savedRoster = rosterRepository.save(roster);

        for (Player player : players) {
            player.setRoster(savedRoster);
        }
        playerService.saveAll(players);

        log.info("Roster {} created for user {}", savedRoster.getName(), owner.getUsername());
        return mapToResponse(savedRoster);
    }

    @Transactional
    public RosterResponse updateRoster(User owner, UUID rosterId, UpdateRosterDto dto) {
        Roster roster = rosterRepository.findById(rosterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster not found"));

        if (!roster.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this roster");
        }

        List<Player> currentPlayers = roster.getPlayers();
        Set<UUID> currentPlayerIds = currentPlayers.stream().map(Player::getId).collect(Collectors.toSet());

        List<UUID> playerIdsToAdd = dto.getAddPlayerIds() != null ? dto.getAddPlayerIds() : List.of();
        List<UUID> playerIdsToRemove = dto.getRemovePlayerIds() != null ? dto.getRemovePlayerIds() : List.of();

        // 1. Ownership and Existence check for all players in the request
        java.util.Set<UUID> allRequestedIds = new java.util.HashSet<>();
        allRequestedIds.addAll(playerIdsToAdd);
        allRequestedIds.addAll(playerIdsToRemove);

        List<Player> allRequestedPlayers = playerService.findAllById(allRequestedIds);
        if (allRequestedPlayers.size() != allRequestedIds.size()) {
            throw new IllegalArgumentException("One or more players not found.");
        }

        for (Player player : allRequestedPlayers) {
            if (player.getOwner() == null || !player.getOwner().getId().equals(owner.getId())) {
                throw new IllegalArgumentException("Player " + player.getNickname() + " does not belong to you.");
            }
        }

        // Calculate final size
        long finalSize = currentPlayerIds.size() + playerIdsToAdd.stream().filter(id -> !currentPlayerIds.contains(id)).count()
                - playerIdsToRemove.stream().filter(currentPlayerIds::contains).count();

        if (finalSize > 5) {
            throw new IllegalArgumentException("A roster cannot have more than 5 players.");
        }

        // Identify players to remove
        List<Player> playersToRemove = allRequestedPlayers.stream()
                .filter(p -> playerIdsToRemove.contains(p.getId()))
                .toList();

        for (Player p : playersToRemove) {
            if (p.getRoster() == null || !p.getRoster().getId().equals(rosterId)) {
                throw new IllegalArgumentException("Player " + p.getNickname() + " is not in this roster.");
            }
        }

        // Identify players to add (only if not already in roster)
        List<Player> playersToAdd = allRequestedPlayers.stream()
                .filter(p -> playerIdsToAdd.contains(p.getId()) && !currentPlayerIds.contains(p.getId()))
                .toList();

        for (Player player : playersToAdd) {
            if (player.getRoster() != null && !player.getRoster().getId().equals(rosterId)) {
                throw new IllegalArgumentException("Player " + player.getNickname() + " is already assigned to another roster.");
            }
        }

        // Apply cohesion penalty: drop by 1 per added player, never below 0.
        if (!playersToAdd.isEmpty()) {
            BigDecimal penalty = new BigDecimal(playersToAdd.size());
            BigDecimal newCohesion = roster.getCohesion().subtract(penalty);
            if (newCohesion.compareTo(BigDecimal.ZERO) < 0) {
                newCohesion = BigDecimal.ZERO;
            }
            roster.setCohesion(newCohesion);
        }

        // Update relationships
        for (Player p : playersToRemove) {
            p.setRoster(null);
        }
        playerService.saveAll(playersToRemove);

        for (Player p : playersToAdd) {
            p.setRoster(roster);
        }
        playerService.saveAll(playersToAdd);

        // Update roster's player list for the response
        // Fetch all players that should be in the roster now
        Set<UUID> finalPlayerIds = new java.util.HashSet<>(currentPlayerIds);
        playerIdsToRemove.forEach(finalPlayerIds::remove);
        playersToAdd.forEach(p -> finalPlayerIds.add(p.getId()));

        List<Player> finalPlayers = playerService.findAllById(finalPlayerIds);
        roster.setPlayers(finalPlayers);

        Roster savedRoster = rosterRepository.save(roster);
        log.info("Roster {} updated for user {}", savedRoster.getName(), owner.getUsername());
        return mapToResponse(savedRoster);
    }

    @Transactional
    public void deleteRoster(User owner, UUID rosterId) {
        Roster roster = rosterRepository.findById(rosterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster not found"));

        if (!roster.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this roster");
        }

        List<Player> players = roster.getPlayers();
        if (players != null) {
            for (Player player : players) {
                player.setRoster(null);
            }
            playerService.saveAll(players);
        }

        rosterRepository.delete(roster);
        log.info("Roster {} deleted by user {}", rosterId, owner.getUsername());
    }

    @Transactional(readOnly = true)
    public List<RosterResponse> getMyRosters(User owner) {
        return rosterRepository.findAllByOwnerId(owner.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RosterResponse mapToResponse(Roster roster) {
        // Ensure players are loaded if we're in a transaction
        if (roster.getPlayers() != null) {
            roster.getPlayers().size(); 
        }
        return RosterResponse.builder()
                .id(roster.getId())
                .name(roster.getName())
                .region(roster.getRegion())
                .activity(roster.getActivity())
                .cohesion(roster.getCohesion())
                .morale(roster.getMorale())
                .energy(roster.getEnergy())
                .strength(calculateRosterStrength(roster))
                .players(roster.getPlayers() != null ? 
                        roster.getPlayers().stream().map(playerService::mapToResponse).toList() : 
                        List.of())
                .build();
    }

    @Transactional
    public Roster save(Roster roster) {
        return rosterRepository.save(roster);
    }

    public double calculateRosterStrength(Roster roster) {
        List<Player> players = roster.getPlayers();
        if (players == null || players.isEmpty()) {
            return 0.0;
        }

        double totalStrength = 0.0;
        for (Player player : players) {
            int maxRoleMastery = player.getRoleMasteries() != null ?
                    player.getRoleMasteries().stream()
                            .mapToInt(PlayerRoleMastery::getLevel)
                            .max()
                            .orElse(1) : 1;

            int maxHeroMastery = player.getHeroMasteries() != null ?
                    player.getHeroMasteries().stream()
                            .mapToInt(PlayerHeroMastery::getLevel)
                            .max()
                            .orElse(1) : 1;

            totalStrength += (maxRoleMastery + maxHeroMastery) / 2.0;
        }

        return totalStrength / players.size();
    }
}
