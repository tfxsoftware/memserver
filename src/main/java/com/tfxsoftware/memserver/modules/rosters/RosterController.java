package com.tfxsoftware.memserver.modules.rosters;

import com.tfxsoftware.memserver.modules.rosters.dto.CreateRosterDto;
import com.tfxsoftware.memserver.modules.rosters.dto.RosterResponse;
import com.tfxsoftware.memserver.modules.rosters.dto.UpdateRosterDto;
import com.tfxsoftware.memserver.modules.users.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rosters")
@RequiredArgsConstructor
public class RosterController {

    private final RosterService rosterService;

    @PostMapping()
    public ResponseEntity<RosterResponse> createRoster(@AuthenticationPrincipal User user, @RequestBody @Valid CreateRosterDto dto) {
        return ResponseEntity.ok(rosterService.createRoster(user, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RosterResponse> updateRoster(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateRosterDto dto) {
        return ResponseEntity.ok(rosterService.updateRoster(user, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoster(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        rosterService.deleteRoster(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<List<RosterResponse>> getMyRosters(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(rosterService.getMyRosters(user));
    }
}
