package com.tfxsoftware.memserver.modules.heroes;

import com.tfxsoftware.memserver.modules.heroes.dto.HeroMetaEntryDto;
import com.tfxsoftware.memserver.modules.heroes.dto.HeroResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;

@RestController
@RequestMapping("/api/heroes")
@RequiredArgsConstructor
public class HeroController {

    private final HeroService heroService;

    @GetMapping
    public ResponseEntity<List<HeroResponse>> getAllHeroes() {
        return ResponseEntity.ok(heroService.getAllHeroes());
    }

    @GetMapping("/meta")
    public ResponseEntity<Map<HeroRole, List<HeroMetaEntryDto>>> getHeroesByMeta() {
        return ResponseEntity.ok(heroService.getHeroesByMeta());
    }
}