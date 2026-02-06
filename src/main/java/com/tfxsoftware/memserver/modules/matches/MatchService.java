package com.tfxsoftware.memserver.modules.matches;

import com.tfxsoftware.memserver.modules.events.Event;
import com.tfxsoftware.memserver.modules.events.EventRepository;
import com.tfxsoftware.memserver.modules.matches.dto.*;
import com.tfxsoftware.memserver.modules.heroes.HeroService;
import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.rosters.RosterService;
import com.tfxsoftware.memserver.modules.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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
    private final EventRepository eventRepository; // Added to resolve Event references
    private final PlayerService playerService;
    private final RosterService rosterService;
    private final HeroService heroService;
    private final MatchResultRepository matchResultRepository;

    @Transactional
    public MatchResponse create(CreateMatchDto dto) {
        // Use getReferenceById to link the event without a heavy SELECT query
        Event event = dto.getEventId() != null ? 
                eventRepository.getReferenceById(dto.getEventId()) : null;

        Match match = Match.builder()
                .homeRosterId(dto.getHomeRosterId())
                .awayRosterId(dto.getAwayRosterId())
                .scheduledTime(dto.getScheduledTime())
                .event(event) // Updated from eventId(UUID) to event(Event)
                .build();

        Match savedMatch = matchRepository.save(match);
        return mapToResponse(savedMatch);
    }

    @Transactional
    public MatchResponse updateDraft(UUID matchId, UpdateMatchDraftDto dto, User currentUser) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));

        if (match.getStatus() != Match.MatchStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update draft for non-scheduled match");
        }

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
                match.getHomeBans().clear();
                match.getHomeBans().addAll(dto.getTeamBans());
            }
            if (dto.getPickIntentions() != null) {
                updatePickIntentions(match.getHomePickIntentions(), dto.getPickIntentions(), userRosterId);
            }
        } else {
            if (dto.getTeamBans() != null) {
                validateHeroIds(dto.getTeamBans());
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
            Player pEntity = playerService.findById(pickDto.getPlayerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found: " + pickDto.getPlayerId()));

            if (pEntity.getRoster() == null || !pEntity.getRoster().getId().equals(rosterId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player does not belong to the roster");
            }

            List<UUID> heroIds = new ArrayList<>();
            if (pickDto.getPreferredHeroId1() != null) heroIds.add(pickDto.getPreferredHeroId1());
            if (pickDto.getPreferredHeroId2() != null) heroIds.add(pickDto.getPreferredHeroId2());
            if (pickDto.getPreferredHeroId3() != null) heroIds.add(pickDto.getPreferredHeroId3());
            validateHeroIds(heroIds);

            currentPicks.removeIf(p -> p.getPlayerId().equals(pickDto.getPlayerId()));

            currentPicks.add(new Match.MatchPick(
                    pickDto.getPlayerId(),
                    pickDto.getRole(),
                    pickDto.getPreferredHeroId1(),
                    pickDto.getPreferredHeroId2(),
                    pickDto.getPreferredHeroId3(),
                    pickDto.getPickOrder()
            ));
        }

        Set<Object> roles = new HashSet<>();
        Set<Integer> pickOrders = new HashSet<>();
        for (Match.MatchPick pick : currentPicks) {
            if (!roles.add(pick.getRole())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate role: " + pick.getRole());
            }
            if (!pickOrders.add(pick.getPickOrder())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate pick order: " + pick.getPickOrder());
            }
        }
    }

    private void validateHeroIds(List<UUID> heroIds) {
        if (heroIds == null) return;
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
                match.getEvent() != null ? match.getEvent().getId() : null // Correctly extract UUID from Event proxy
        );
    }

    @Transactional(readOnly = true)
    public List<UserMatchScheduleResponse> getMyScheduledMatches(User currentUser) {
        List<Roster> myRosters = rosterService.getMyRostersEntities(currentUser);
        List<UUID> myRosterIds = myRosters.stream().map(Roster::getId).toList();

        if (myRosterIds.isEmpty()) {
            return List.of();
        }

        List<Match> scheduledMatches = matchRepository.findAllByStatusAndHomeRosterIdInOrAwayRosterIdIn(
                Match.MatchStatus.SCHEDULED,
                myRosterIds,
                myRosterIds
        );

        return scheduledMatches.stream().map(match -> {
            boolean isHome = myRosterIds.contains(match.getHomeRosterId());
            UUID myId = isHome ? match.getHomeRosterId() : match.getAwayRosterId();
            UUID opponentId = isHome ? match.getAwayRosterId() : match.getHomeRosterId();

            String myName = rosterService.findById(myId).map(Roster::getName).orElse("Unknown");
            String opponentName = rosterService.findById(opponentId).map(Roster::getName).orElse("Unknown");
            
            return UserMatchScheduleResponse.builder()
                    .matchId(match.getId())
                    .eventId(match.getEvent() != null ? match.getEvent().getId() : null)
                    .eventName(match.getEvent() != null ? match.getEvent().getName() : null)
                    .myRosterId(myId)
                    .myRosterName(myName)
                    .opponentRosterId(opponentId)
                    .opponentRosterName(opponentName)
                    .scheduledTime(match.getScheduledTime())
                    .status(match.getStatus())
                    .myBans(isHome ? match.getHomeBans() : match.getAwayBans())
                    .myPickIntentions(isHome ? match.getHomePickIntentions() : match.getAwayPickIntentions())
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public Page<UserMatchHistoryResponse> getMyMatchHistory(User currentUser, Pageable pageable) {
        List<Roster> myRosters = rosterService.getMyRostersEntities(currentUser);
        List<UUID> myRosterIds = myRosters.stream().map(Roster::getId).toList();

        if (myRosterIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Match> completedMatches = matchRepository.findByStatusAndRosterIdsIn(
                Match.MatchStatus.COMPLETED,
                myRosterIds,
                pageable
        );

        return completedMatches.map(match -> {
            boolean isHome = myRosterIds.contains(match.getHomeRosterId());
            UUID myId = isHome ? match.getHomeRosterId() : match.getAwayRosterId();
            UUID opponentId = isHome ? match.getAwayRosterId() : match.getHomeRosterId();

            String myName = rosterService.findById(myId).map(Roster::getName).orElse("Unknown");
            String opponentName = rosterService.findById(opponentId).map(Roster::getName).orElse("Unknown");

            MatchResult result = matchResultRepository.findById(match.getId()).orElse(null);
            boolean isWin = result != null && result.getWinnerRosterId().equals(myId);

            return UserMatchHistoryResponse.builder()
                    .matchId(match.getId())
                    .eventId(match.getEvent() != null ? match.getEvent().getId() : null)
                    .eventName(match.getEvent() != null ? match.getEvent().getName() : null)
                    .myRosterId(myId)
                    .myRosterName(myName)
                    .opponentRosterId(opponentId)
                    .opponentRosterName(opponentName)
                    .playedAt(match.getPlayedAt())
                    .status(match.getStatus())
                    .isWin(isWin)
                    .result(result)
                    .build();
        });
    }
}