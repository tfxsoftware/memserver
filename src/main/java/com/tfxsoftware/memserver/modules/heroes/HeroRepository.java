package com.tfxsoftware.memserver.modules.heroes;

import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;



@Repository
public interface HeroRepository extends JpaRepository<Hero, UUID> {
    
}
