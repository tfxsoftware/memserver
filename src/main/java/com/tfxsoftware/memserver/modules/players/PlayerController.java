package com.tfxsoftware.memserver.modules.players;

import com.tfxsoftware.memserver.modules.players.dto.PlayerResponse;
import com.tfxsoftware.memserver.modules.users.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@EnableMethodSecurity
public class PlayerController {

    private final PlayerService playerService;

    /**
     * View the public marketplace (Players without owners or explicitly listed).
     */
    @GetMapping("/market")
    public ResponseEntity<List<PlayerResponse>> getMarketplace() {
        return ResponseEntity.ok(playerService.getMarketplace());
    }

    @PostMapping("/discover/rookie")
    public ResponseEntity<PlayerResponse> generatePlayer(@AuthenticationPrincipal User currentUser) {
        Player player = playerService.generateRookie(currentUser);
        return ResponseEntity.ok(playerService.mapToResponse(player));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<PlayerResponse> createPlayer(@RequestBody @Valid Player dto){
        Player player = playerService.createPlayer(dto);
        return ResponseEntity.ok(playerService.mapToResponse(player));
    }

}