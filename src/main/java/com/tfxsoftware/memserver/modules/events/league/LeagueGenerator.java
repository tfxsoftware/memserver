package com.tfxsoftware.memserver.modules.events.league;

import com.tfxsoftware.memserver.modules.events.Event;
import com.tfxsoftware.memserver.modules.events.EventRegistration;
import com.tfxsoftware.memserver.modules.matches.Match;
import com.tfxsoftware.memserver.modules.matches.MatchRepository;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors; 

/**
 * Handles the creation of Match entities and Standings for a League.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeagueGenerator {

    private final MatchRepository matchRepository;
    private final LeagueStandingRepository standingRepository;

    /**
     * Generates a complete schedule for a Round Robin league and calculates the event's end time.
     */
    public void generateFullSeason(Event event) {
        League league = event.getLeague();
        List<Roster> participants = event.getRegistrations().stream()
                .map(EventRegistration::getRoster)
                .collect(Collectors.toList());

        // 1. Initialize Standings
        initializeStandings(league, participants);

        // 2. Initial position calculation (alphabetical or registration order)
        recalculatePositions(event.getId());

        // 3. Generate Pairings using Circle Method
        List<Roster> teams = new ArrayList<>(participants);
        if (teams.size() % 2 != 0) {
            teams.add(null); // Add a "Bye" team if odd
        }

        int numTeams = teams.size();
        int numRounds = (numTeams - 1) * league.getRoundRobinCount();
        
        LocalDateTime currentMatchTime = event.getStartsAt().plusMinutes(event.getMinutesBetweenBlocks());
        LocalDateTime lastScheduledTime = currentMatchTime;
        int currentBlockCount = 0;
        List<Match> seasonMatches = new ArrayList<>();

        for (int r = 0; r < numRounds; r++) {
            for (int i = 0; i < numTeams / 2; i++) {
                Roster home = teams.get(i);
                Roster away = teams.get(numTeams - 1 - i);

                // Skip "Bye" matches
                if (home == null || away == null) continue;

                // Track this as the last scheduled match time
                lastScheduledTime = currentMatchTime;

                // Alternate home/away sides every other round for fairness
                Match match = (r % 2 == 0) 
                    ? buildMatch(event, home, away, currentMatchTime)
                    : buildMatch(event, away, home, currentMatchTime);
                
                seasonMatches.add(match);

                // Pacing Math: Calculate the time for the NEXT match
                currentBlockCount++;
                if (currentBlockCount >= event.getGamesPerBlock()) {
                    // Block end: Apply the long rest period
                    currentMatchTime = currentMatchTime.plusMinutes(event.getMinutesBetweenBlocks());
                    currentBlockCount = 0;
                } else {
                    // Inside block: Apply the short rest period
                    currentMatchTime = currentMatchTime.plusMinutes(event.getMinutesBetweenGames());
                }
            }

            // Rotate teams for the next round (fixing the first team in place)
            Collections.rotate(teams.subList(1, numTeams), 1);
        }

        // 3. Set the predicted finish time for the event
        // We add a 60-minute buffer to the last match's start time to account for simulation duration
        event.setFinishesAt(lastScheduledTime.plusMinutes(60));

        matchRepository.saveAll(seasonMatches);
        log.info("Generated {} matches for league {}. Predicted finish at: {}", 
                seasonMatches.size(), event.getName(), event.getFinishesAt());
    }

    private void initializeStandings(League league, List<Roster> participants) {
                List<LeagueStanding> standings = participants.stream().map((Roster roster) ->
                    LeagueStanding.builder()
                        .league(league)
                        .roster(roster)
                        .wins(0)
                        .losses(0)
                        .build()
                ).collect(Collectors.toList());        standingRepository.saveAll(standings);
    }

    private void recalculatePositions(UUID eventId) {
        List<LeagueStanding> standings = standingRepository.findAllByLeagueEventIdOrderByWinsDesc(eventId);
        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setPosition(i + 1);
        }
        standingRepository.saveAll(standings);
    }

    private Match buildMatch(Event event, Roster home, Roster away, LocalDateTime time) {
        return Match.builder()
                .event(event)
                .homeRosterId(home.getId())
                .awayRosterId(away.getId())
                .status(Match.MatchStatus.SCHEDULED)
                .scheduledTime(time)
                .build();
    }
}