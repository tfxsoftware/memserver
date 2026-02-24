package com.tfxsoftware.memserver.modules.bootcamps;

import com.tfxsoftware.memserver.modules.bootcamps.dto.CreateBootcampSessionDto;
import com.tfxsoftware.memserver.modules.heroes.Hero;
import com.tfxsoftware.memserver.modules.players.MasteryService;
import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.rosters.RosterService;
import com.tfxsoftware.memserver.modules.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BootcampServiceTest {

    @Mock
    private RosterService rosterService;
    @Mock
    private BootcampSessionRepository sessionRepository;
    @Mock
    private PlayerService playerService;
    @Mock
    private MasteryService masteryService;

    @InjectMocks
    private BootcampService bootcampService;

    private User user;
    private Roster roster;
    private UUID rosterId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());

        rosterId = UUID.randomUUID();
        roster = new Roster();
        roster.setId(rosterId);
        roster.setOwner(user);
        roster.setActivity(Roster.RosterActivity.IDLE);
    }

    @Test
    void startBootcamp_withDuplicateHeroes_throwsException() {
        UUID heroId = UUID.randomUUID();
        CreateBootcampSessionDto.PlayerTrainingConfigDto configDto = new CreateBootcampSessionDto.PlayerTrainingConfigDto();
        configDto.setPlayerId(UUID.randomUUID());
        configDto.setTargetRole(Hero.HeroRole.MID);
        configDto.setPrimaryHeroId(heroId);
        configDto.setSecondaryHeroId1(heroId); // Duplicate

        CreateBootcampSessionDto request = new CreateBootcampSessionDto();
        request.setConfigs(List.of(configDto));

        when(rosterService.findById(rosterId)).thenReturn(Optional.of(roster));

        assertThrows(ResponseStatusException.class, () -> {
            bootcampService.startBootcamp(user, rosterId, request);
        });
    }

    @Test
    void startBootcamp_withUniqueHeroes_succeeds() {
        UUID heroId1 = UUID.randomUUID();
        UUID heroId2 = UUID.randomUUID();
        CreateBootcampSessionDto.PlayerTrainingConfigDto configDto = new CreateBootcampSessionDto.PlayerTrainingConfigDto();
        configDto.setPlayerId(UUID.randomUUID());
        configDto.setTargetRole(Hero.HeroRole.MID);
        configDto.setPrimaryHeroId(heroId1);
        configDto.setSecondaryHeroId1(heroId2);

        CreateBootcampSessionDto request = new CreateBootcampSessionDto();
        request.setConfigs(List.of(configDto));

        when(rosterService.findById(rosterId)).thenReturn(Optional.of(roster));

        bootcampService.startBootcamp(user, rosterId, request);
    }
}
