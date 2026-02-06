package com.tfxsoftware.memserver.modules.matches;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findAllByStatusAndScheduledTimeBefore(
        Match.MatchStatus status, 
        LocalDateTime time
    );

    List<Match> findAllByStatusAndHomeRosterIdInOrAwayRosterIdIn(
        Match.MatchStatus status,
        List<UUID> homeRosterIds,
        List<UUID> awayRosterIds
    );

    @Query("SELECT m FROM Match m WHERE m.status = :status AND (m.homeRosterId IN :rosterIds OR m.awayRosterId IN :rosterIds)")
    Page<Match> findByStatusAndRosterIdsIn(@Param("status") Match.MatchStatus status, @Param("rosterIds") List<UUID> rosterIds, Pageable pageable);
}
