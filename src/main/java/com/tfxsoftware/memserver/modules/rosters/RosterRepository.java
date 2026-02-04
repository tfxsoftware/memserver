package com.tfxsoftware.memserver.modules.rosters;

import com.tfxsoftware.memserver.modules.users.User.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RosterRepository extends JpaRepository<Roster, UUID> {
    boolean existsByOwnerId(UUID ownerId);

    boolean existsByOwnerIdAndRegion(UUID ownerId, Region region);
    
    @Query("SELECT r FROM Roster r LEFT JOIN FETCH r.players WHERE r.owner.id = :ownerId")
    List<Roster> findAllByOwnerId(@Param("ownerId") UUID ownerId);

    List<Roster> findAllByEnergyLessThan(Integer energy);
}
