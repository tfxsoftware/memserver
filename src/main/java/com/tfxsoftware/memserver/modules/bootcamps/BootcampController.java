package com.tfxsoftware.memserver.modules.bootcamps;

import com.tfxsoftware.memserver.modules.bootcamps.dto.ActiveBootcampResponseDto;
import com.tfxsoftware.memserver.modules.bootcamps.dto.CreateBootcampSessionDto;
import com.tfxsoftware.memserver.modules.users.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bootcamps")
@RequiredArgsConstructor
public class BootcampController {

    private final BootcampService bootcampService;

    @GetMapping("/me")
    public ResponseEntity<List<ActiveBootcampResponseDto>> getMyActiveBootcamps(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bootcampService.getActiveBootcamps(user));
    }

    @PostMapping("/{rosterId}/start")
    public ResponseEntity<Void> startBootcamp(
            @AuthenticationPrincipal User user,
            @PathVariable UUID rosterId,
            @RequestBody @Valid CreateBootcampSessionDto dto) {
        bootcampService.startBootcamp(user, rosterId, dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{rosterId}")
    public ResponseEntity<Void> updateBootcamp(
            @AuthenticationPrincipal User user,
            @PathVariable UUID rosterId,
            @RequestBody @Valid CreateBootcampSessionDto dto) {
        bootcampService.updateBootcamp(user, rosterId, dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{rosterId}/stop")
    public ResponseEntity<Void> stopBootcamp(
            @AuthenticationPrincipal User user,
            @PathVariable UUID rosterId) {
        bootcampService.stopBootcamp(user, rosterId);
        return ResponseEntity.ok().build();
    }
}
