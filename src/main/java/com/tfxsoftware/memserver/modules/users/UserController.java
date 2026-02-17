package com.tfxsoftware.memserver.modules.users;

import com.tfxsoftware.memserver.modules.players.PlayerService;
import com.tfxsoftware.memserver.modules.players.dto.AddExperienceDto;
import com.tfxsoftware.memserver.modules.players.dto.PlayerResponse;
import com.tfxsoftware.memserver.modules.users.dto.UpdateUserDto;
import com.tfxsoftware.memserver.modules.users.dto.UserResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@EnableMethodSecurity
public class UserController {

    private final PlayerService playerService;
    private final UserService userService;


    @GetMapping("/me/players")
    public ResponseEntity<List<PlayerResponse>> getMyPlayers(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(playerService.getOwnedPlayers(user));
    }

    @GetMapping("/me/players/{playerId}")
    public ResponseEntity<PlayerResponse> getMyPlayer(
            @AuthenticationPrincipal User user,
            @PathVariable UUID playerId
    ) {
        return ResponseEntity.ok(playerService.getOwnedPlayer(user, playerId));
    }

    @PatchMapping("/me/organization")
    public ResponseEntity<UserResponse> updateOrganization(@AuthenticationPrincipal User user, @RequestBody UpdateUserDto dto) {
        return ResponseEntity.ok(userService.updateOrganization(user, dto));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.mapToResponse(user));
    }
}