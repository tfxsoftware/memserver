package com.tfxsoftware.memserver.modules.players;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findByOwnerIsNull(); // For the Free Agent market
    List<Player> findByOwnerId(UUID ownerId); // For the User's roster
    List<Player> findAllByNextSalaryPaymentDateBefore(java.time.LocalDateTime dateTime);
}