package com.tfxsoftware.memserver.modules.events.league;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeagueStandingRepository extends JpaRepository<LeagueStanding, UUID> {
    List<LeagueStanding> findAllByLeagueEventIdOrderByWinsDesc(UUID leagueId);
    java.util.Optional<LeagueStanding> findByLeagueEventIdAndRosterId(UUID leagueId, UUID rosterId);
}
