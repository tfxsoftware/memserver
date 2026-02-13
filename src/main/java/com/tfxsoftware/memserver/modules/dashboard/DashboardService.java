package com.tfxsoftware.memserver.modules.dashboard;

import com.tfxsoftware.memserver.modules.dashboard.dto.*;
import com.tfxsoftware.memserver.modules.matches.Match;
import com.tfxsoftware.memserver.modules.matches.MatchRepository;
import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.players.PlayerRepository;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.rosters.RosterRepository;
import com.tfxsoftware.memserver.modules.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RosterRepository rosterRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboardData(User user) {
        UserProfileDto profile = new UserProfileDto(
                user.getUsername(),
                user.getRegion(),
                user.getBalance(),
                user.getOrganizationImageUrl()
        );

        List<Roster> ownedRosters = rosterRepository.findAllByOwnerId(user.getId());
        List<RosterVitalsDto> rostersDto = ownedRosters.stream()
                .map(r -> new RosterVitalsDto(
                        r.getId(),
                        r.getName(),
                        r.getEnergy(),
                        r.getMorale(),
                        r.getCohesion(),
                        r.getActivity()
                ))
                .collect(Collectors.toList());

        List<Player> ownedPlayers = playerRepository.findByOwnerId(user.getId());
        List<PlayerPedestalDto> playersDto = ownedPlayers.stream()
                .map(this::mapToPlayerPedestal)
                .collect(Collectors.toList());

        UpcomingMatchDto nextMatch = null;
        if (!ownedRosters.isEmpty()) {
            // Looking for matches of any of the user's rosters
            List<UUID> rosterIds = ownedRosters.stream().map(Roster::getId).collect(Collectors.toList());
            nextMatch = getUpcomingMatchForRosters(rosterIds);
        }

        return new DashboardResponseDto(profile, rostersDto, playersDto, nextMatch);
    }

    private PlayerPedestalDto mapToPlayerPedestal(Player player) {
        return new PlayerPedestalDto(
                player.getId(),
                player.getNickname(),
                player.getPictureUrl(),
                player.getRoster() != null ? player.getRoster().getName() : null,
                player.getCondition(),
                new HashSet<>(player.getTraits()),
                player.getSalary(),
                player.getNextSalaryPaymentDate(),
                player.getIsStar()
        );
    }

    private UpcomingMatchDto getUpcomingMatchForRosters(List<UUID> rosterIds) {
        List<Match> matches = matchRepository.findAllByStatusAndHomeRosterIdInOrAwayRosterIdIn(
                Match.MatchStatus.SCHEDULED,
                rosterIds,
                rosterIds
        );

        return matches.stream()
                .filter(m -> m.getScheduledTime().isAfter(LocalDateTime.now()))
                .min(Comparator.comparing(Match::getScheduledTime))
                .map(m -> {
                    UUID myRosterIdInMatch = rosterIds.contains(m.getHomeRosterId()) ? m.getHomeRosterId() : m.getAwayRosterId();
                    UUID opponentId = m.getHomeRosterId().equals(myRosterIdInMatch) ? m.getAwayRosterId() : m.getHomeRosterId();
                    String opponentName = rosterRepository.findById(opponentId).map(Roster::getName).orElse("Unknown");
                    return new UpcomingMatchDto(m.getId(), opponentName, m.getScheduledTime(), m.getEvent().getName());
                })
                .orElse(null);
    }
}
