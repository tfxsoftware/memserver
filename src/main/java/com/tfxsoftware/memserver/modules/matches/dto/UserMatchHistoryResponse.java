package com.tfxsoftware.memserver.modules.matches.dto;

import com.tfxsoftware.memserver.modules.matches.Match;
import com.tfxsoftware.memserver.modules.matches.MatchResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserMatchHistoryResponse {
    private UUID matchId;
    private UUID eventId;
    private String eventName;
    private UUID myRosterId;
    private String myRosterName;
    private UUID opponentRosterId;
    private String opponentRosterName;
    private LocalDateTime playedAt;
    private Match.MatchStatus status;
    private boolean isWin;
    private MatchResult result;
}
