package com.tfxsoftware.memserver.modules.users;

import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.players.dto.PlayerResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@EnableMethodSecurity
public class UserController {

    private final PlayerService playerService;


    @GetMapping("/me/players")
    public ResponseEntity<List<PlayerResponse>> getMyPlayers(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(playerService.getOwnedPlayers(user));
    }
}