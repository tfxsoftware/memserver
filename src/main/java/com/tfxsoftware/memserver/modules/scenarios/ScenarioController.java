package com.tfxsoftware.memserver.modules.scenarios;

import com.tfxsoftware.memserver.modules.scenarios.dto.CreateScenarioDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
@EnableMethodSecurity
public class ScenarioController {

    private final ScenarioService scenarioService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Void> createScenario(@RequestBody @Valid CreateScenarioDto dto) {
        scenarioService.createScenario(dto.getUser1Id(), dto.getUser2Id());
        return ResponseEntity.ok().build();
    }
}
