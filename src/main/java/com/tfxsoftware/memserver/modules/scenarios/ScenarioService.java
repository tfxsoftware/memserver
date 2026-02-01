package com.tfxsoftware.memserver.modules.scenarios;

import com.tfxsoftware.memserver.modules.heroes.Hero;
import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import com.tfxsoftware.memserver.modules.heroes.HeroService;
import com.tfxsoftware.memserver.modules.matches.Match;
import com.tfxsoftware.memserver.modules.matches.MatchRepository;
import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.rosters.RosterRepository;
import com.tfxsoftware.memserver.modules.users.User;
import com.tfxsoftware.memserver.modules.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final UserService userService;
    private final PlayerService playerService;
    private final RosterRepository rosterRepository;
    private final MatchRepository matchRepository;
    private final HeroService heroService;

    @Transactional
    public void createScenario(UUID user1Id, UUID user2Id) {
        User user1 = userService.getUserById(user1Id);
        User user2 = userService.getUserById(user2Id);

        Roster roster1 = createRosterWithRookies(user1);
        Roster roster2 = createRosterWithRookies(user2);

        Match match = Match.builder()
                .homeRosterId(roster1.getId())
                .awayRosterId(roster2.getId())
                .scheduledTime(LocalDateTime.now().plusHours(1))
                .status(Match.MatchStatus.SCHEDULED)
                .build();

        List<Hero> allHeroes = heroService.findAll();
        if (allHeroes.size() < 10) {
            throw new IllegalStateException("Not enough heroes to create a scenario match");
        }

        // Setup Draft for User 1
        setupDraft(match.getHomeBans(), match.getHomePickIntentions(), roster1.getPlayers(), allHeroes);

        // Setup Draft for User 2
        setupDraft(match.getAwayBans(), match.getAwayPickIntentions(), roster2.getPlayers(), allHeroes);

        matchRepository.save(match);
    }

    private Roster createRosterWithRookies(User owner) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            players.add(playerService.generateRookie(owner));
        }

        Roster roster = Roster.builder()
                .owner(owner)
                .name("Roster of " + owner.getUsername() + " " + UUID.randomUUID().toString().substring(0, 5))
                .region(owner.getRegion())
                .cohesion(BigDecimal.ZERO)
                .morale(new BigDecimal("5.00"))
                .energy(100)
                .build();

        roster = rosterRepository.save(roster);

        for (Player p : players) {
            p.setRoster(roster);
        }
        playerService.saveAll(players);
        roster.setPlayers(players);

        return roster;
    }

    private void setupDraft(List<UUID> bans, List<Match.MatchPick> picks, List<Player> players, List<Hero> allHeroes) {
        // Ban 5 random heroes
        for (int i = 0; i < 5; i++) {
            bans.add(allHeroes.get(i).getId());
        }

        // Create picks for each player
        HeroRole[] roles = HeroRole.values();
        for (int i = 0; i < 5; i++) {
            Player player = players.get(i);
            HeroRole role = roles[i % roles.length];
            
            Match.MatchPick pick = new Match.MatchPick();
            pick.setPlayerId(player.getId());
            pick.setRole(role);
            pick.setPickOrder(i + 1);
            
            // Just assign some heroes
            pick.setPreferredHeroId1(allHeroes.get(5).getId());
            pick.setPreferredHeroId2(allHeroes.get(6).getId());
            pick.setPreferredHeroId3(allHeroes.get(7).getId());
            
            picks.add(pick);
        }
    }
}
