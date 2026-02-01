package com.tfxsoftware.memserver.modules.matches;

import com.tfxsoftware.memserver.modules.matches.dto.CreateMatchDto;
import com.tfxsoftware.memserver.modules.matches.dto.MatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;

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
