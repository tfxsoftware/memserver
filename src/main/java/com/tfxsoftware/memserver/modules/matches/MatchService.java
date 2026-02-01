package com.tfxsoftware.memserver.modules.matches;

import com.tfxsoftware.memserver.modules.matches.dto.CreateMatchDto;
import com.tfxsoftware.memserver.modules.matches.dto.MatchResponse;
import com.tfxsoftware.memserver.modules.matches.dto.UpdateMatchDraftDto;
import com.tfxsoftware.memserver.modules.heroes.HeroService;
import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.rosters.RosterService;
import com.tfxsoftware.memserver.modules.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final PlayerService playerService;
    private final RosterService rosterService;
    private final HeroService heroService;

    @Transactional
    public MatchResponse create(CreateMatchDto dto) {
        Match match = Match.builder()
                .homeRosterId(dto.homeRosterId())
                .awayRosterId(dto.awayRosterId())
                .scheduledTime(dto.scheduledTime())
                .eventId(dto.eventId())
                .build();

        Match savedMatch = matchRepository.save(match);
        return mapToResponse(savedMatch);
    }

    @Transactional
    public MatchResponse updateDraft(UUID matchId, UpdateMatchDraftDto dto, User currentUser) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // Determine if current user is home or away based on the rosters in the match
        Roster homeRoster = rosterService.findById(match.getHomeRosterId())
                .orElseThrow(() -> new RuntimeException("Home roster not found"));
        Roster awayRoster = rosterService.findById(match.getAwayRosterId())
                .orElseThrow(() -> new RuntimeException("Away roster not found"));

        boolean isHome = homeRoster.getOwner().getId().equals(currentUser.getId());
        boolean isAway = awayRoster.getOwner().getId().equals(currentUser.getId());

        if (!isHome && !isAway) {
            throw new RuntimeException("You do not own any roster in this match");
        }

        UUID userRosterId = isHome ? match.getHomeRosterId() : match.getAwayRosterId();

        if (isHome) {
            if (dto.teamBans() != null) {
                validateHeroIds(dto.teamBans());
                if (dto.teamBans().size() > 5) {
                    throw new RuntimeException("Ban list cannot exceed 5 heroes");
                }
                match.getHomeBans().clear();
                match.getHomeBans().addAll(dto.teamBans());
            }
            if (dto.pickIntentions() != null) {
                updatePickIntentions(match.getHomePickIntentions(), dto.pickIntentions(), userRosterId);
            }
        } else {
            if (dto.teamBans() != null) {
                validateHeroIds(dto.teamBans());
                if (dto.teamBans().size() > 5) {
                    throw new RuntimeException("Ban list cannot exceed 5 heroes");
                }
                match.getAwayBans().clear();
                match.getAwayBans().addAll(dto.teamBans());
            }
            if (dto.pickIntentions() != null) {
                updatePickIntentions(match.getAwayPickIntentions(), dto.pickIntentions(), userRosterId);
            }
        }

        return mapToResponse(matchRepository.save(match));
    }

    private void updatePickIntentions(List<Match.MatchPick> currentPicks, List<UpdateMatchDraftDto.MatchPickDto> newPicks, UUID rosterId) {
        for (UpdateMatchDraftDto.MatchPickDto pickDto : newPicks) {
            // Consistency: Ensure all playerIds inside the pickIntentions list actually belong to the team being updated
            Player pEntity = playerService.findById(pickDto.playerId())
                    .orElseThrow(() -> new RuntimeException("Player in pick intentions not found: " + pickDto.playerId()));

            if (pEntity.getRoster() == null || !pEntity.getRoster().getId().equals(rosterId)) {
                throw new RuntimeException("Player " + pickDto.playerId() + " does not belong to the roster");
            }

            // Hero Validation: Ensure preferred heroes exist
            List<UUID> heroIds = new ArrayList<>();
            heroIds.add(pickDto.preferredHeroId1());
            heroIds.add(pickDto.preferredHeroId2());
            heroIds.add(pickDto.preferredHeroId3());
            validateHeroIds(heroIds);

            // Remove existing pick for this player if it exists
            currentPicks.removeIf(p -> p.getPlayerId().equals(pickDto.playerId()));

            // Add new pick
            Match.MatchPick newPick = new Match.MatchPick(
                    pickDto.playerId(),
                    pickDto.role(),
                    pickDto.preferredHeroId1(),
                    pickDto.preferredHeroId2(),
                    pickDto.preferredHeroId3(),
                    pickDto.pickOrder()
            );
            currentPicks.add(newPick);
        }

        // Validate: Ensure the Role and PickOrder are unique within the 5-man squad
        Set<Object> roles = new HashSet<>();
        Set<Integer> pickOrders = new HashSet<>();

        for (Match.MatchPick pick : currentPicks) {
            if (!roles.add(pick.getRole())) {
                throw new RuntimeException("Duplicate role in pick intentions: " + pick.getRole());
            }
            if (!pickOrders.add(pick.getPickOrder())) {
                throw new RuntimeException("Duplicate pick order in pick intentions: " + pick.getPickOrder());
            }
        }
    }

    private void validateHeroIds(List<UUID> heroIds) {
        if (heroIds == null || heroIds.isEmpty()) return;
        for (UUID id : heroIds) {
            if (id != null && !heroService.existsById(id)) {
                throw new RuntimeException("Hero not found: " + id);
            }
        }
    }

    private MatchResponse mapToResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getHomeRosterId(),
                match.getAwayRosterId(),
                match.getStatus(),
                match.getScheduledTime(),
                match.getPlayedAt(),
                match.getEventId()
        );
    }
}
