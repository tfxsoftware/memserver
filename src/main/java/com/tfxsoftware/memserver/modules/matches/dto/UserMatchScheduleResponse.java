package com.tfxsoftware.memserver.modules.matches.dto;

import com.tfxsoftware.memserver.modules.matches.Match;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserMatchScheduleResponse {
    private UUID matchId;
    private UUID eventId;
    private String eventName;
    private UUID myRosterId;
    private String myRosterName;
    private UUID opponentRosterId;
    private String opponentRosterName;
    private LocalDateTime scheduledTime;
    private Match.MatchStatus status;
    
    // User specific config (Opponent config hidden)
    private List<UUID> myBans;
    private List<Match.MatchPick> myPickIntentions;
}
