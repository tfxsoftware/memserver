package com.tfxsoftware.memserver.modules.matches;

import org.springframework.web.server.ResponseStatusException;
import com.tfxsoftware.memserver.modules.matches.dto.CreateMatchDto;
import com.tfxsoftware.memserver.modules.matches.dto.MatchResponse;
import com.tfxsoftware.memserver.modules.matches.dto.UpdateMatchDraftDto;
import com.tfxsoftware.memserver.modules.heroes.HeroService;
import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.rosters.RosterService;
import com.tfxsoftware.memserver.modules.users.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

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
                .homeRosterId(dto.getHomeRosterId())
                .awayRosterId(dto.getAwayRosterId())
                .scheduledTime(dto.getScheduledTime())
                .eventId(dto.getEventId())
                .build();

        Match savedMatch = matchRepository.save(match);
        return mapToResponse(savedMatch);
    }

    @Transactional
    public MatchResponse updateDraft(UUID matchId, UpdateMatchDraftDto dto, User currentUser) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));

        // Determine if current user is home or away based on the rosters in the match
        Roster homeRoster = rosterService.findById(match.getHomeRosterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Home roster not found"));
        Roster awayRoster = rosterService.findById(match.getAwayRosterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Away roster not found"));

        boolean isHome = homeRoster.getOwner().getId().equals(currentUser.getId());
        boolean isAway = awayRoster.getOwner().getId().equals(currentUser.getId());

        if (!isHome && !isAway) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own any roster in this match");
        }

        UUID userRosterId = isHome ? match.getHomeRosterId() : match.getAwayRosterId();

        if (isHome) {
            if (dto.getTeamBans() != null) {
                validateHeroIds(dto.getTeamBans());
                if (dto.getTeamBans().size() > 5) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ban list cannot exceed 5 heroes");
                }
                match.getHomeBans().clear();
                match.getHomeBans().addAll(dto.getTeamBans());
            }
            if (dto.getPickIntentions() != null) {
                updatePickIntentions(match.getHomePickIntentions(), dto.getPickIntentions(), userRosterId);
            }
        } else {
            if (dto.getTeamBans() != null) {
                validateHeroIds(dto.getTeamBans());
                if (dto.getTeamBans().size() > 5) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ban list cannot exceed 5 heroes");
                }
                match.getAwayBans().clear();
                match.getAwayBans().addAll(dto.getTeamBans());
            }
            if (dto.getPickIntentions() != null) {
                updatePickIntentions(match.getAwayPickIntentions(), dto.getPickIntentions(), userRosterId);
            }
        }

        return mapToResponse(matchRepository.save(match));
    }

    private void updatePickIntentions(List<Match.MatchPick> currentPicks, List<UpdateMatchDraftDto.MatchPickDto> newPicks, UUID rosterId) {
        for (UpdateMatchDraftDto.MatchPickDto pickDto : newPicks) {
            // Consistency: Ensure all playerIds inside the pickIntentions list actually belong to the team being updated
            Player pEntity = playerService.findById(pickDto.getPlayerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player in pick intentions not found: " + pickDto.getPlayerId()));

            if (pEntity.getRoster() == null || !pEntity.getRoster().getId().equals(rosterId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player " + pickDto.getPlayerId() + " does not belong to the roster");
            }

            // Hero Validation: Ensure preferred heroes exist
            List<UUID> heroIds = new ArrayList<>();
            heroIds.add(pickDto.getPreferredHeroId1());
            heroIds.add(pickDto.getPreferredHeroId2());
            heroIds.add(pickDto.getPreferredHeroId3());
            validateHeroIds(heroIds);

            // Remove existing pick for this player if it exists
            currentPicks.removeIf(p -> p.getPlayerId().equals(pickDto.getPlayerId()));

            // Add new pick
            Match.MatchPick newPick = new Match.MatchPick(
                    pickDto.getPlayerId(),
                    pickDto.getRole(),
                    pickDto.getPreferredHeroId1(),
                    pickDto.getPreferredHeroId2(),
                    pickDto.getPreferredHeroId3(),
                    pickDto.getPickOrder()
            );
            currentPicks.add(newPick);
        }

        // Validate: Ensure the Role and PickOrder are unique within the 5-man squad
        Set<Object> roles = new HashSet<>();
        Set<Integer> pickOrders = new HashSet<>();

        for (Match.MatchPick pick : currentPicks) {
            if (!roles.add(pick.getRole())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate role in pick intentions: " + pick.getRole());
            }
            if (!pickOrders.add(pick.getPickOrder())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate pick order in pick intentions: " + pick.getPickOrder());
            }
        }
    }

    private void validateHeroIds(List<UUID> heroIds) {
        if (heroIds == null || heroIds.isEmpty()) return;
        for (UUID id : heroIds) {
            if (id != null && !heroService.existsById(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hero not found: " + id);
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
