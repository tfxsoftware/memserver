package com.tfxsoftware.memserver.modules.heroes;

import com.tfxsoftware.memserver.modules.heroes.dto.HeroResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/heroes")
@RequiredArgsConstructor
public class HeroController {

    private final HeroRepository heroService;

    @GetMapping
    public ResponseEntity<List<HeroResponse>> getAllHeroes() {
        return null; // TODO: FINISH THIS ROUTE
    }
}