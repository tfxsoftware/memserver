package com.tfxsoftware.memserver.modules.bootcamps;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface BootcampSessionRepository extends JpaRepository<BootcampSession, UUID>{ 
    
    @Query("SELECT s FROM BootcampSession s WHERE s.lastTickAt <= :threshold")
    List<BootcampSession> findAllReadyForTick(@Param("threshold") LocalDateTime threshold);
}
