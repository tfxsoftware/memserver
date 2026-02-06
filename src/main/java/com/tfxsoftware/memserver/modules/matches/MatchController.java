package com.tfxsoftware.memserver.modules.matches;

import com.tfxsoftware.memserver.modules.matches.dto.*;
import com.tfxsoftware.memserver.modules.users.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@EnableMethodSecurity
public class MatchController {

    private final MatchService matchService;
    private final MatchEngineService matchEngineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MatchResponse create(@RequestBody @Valid CreateMatchDto dto) {
        return matchService.create(dto);
    }

    @PatchMapping("/{id}/draft")
    public MatchResponse updateDraft(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateMatchDraftDto dto,
            @AuthenticationPrincipal User user
    ) {
        return matchService.updateDraft(id, dto, user);
    }

    @PostMapping("engine/test/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void simulateGame(@PathVariable UUID id){
        matchEngineService.simulateMatch(id);
    }

    @GetMapping("/my-schedule")
    public List<UserMatchScheduleResponse> getMySchedule(@AuthenticationPrincipal User user) {
        return matchService.getMyScheduledMatches(user);
    }

    @GetMapping("/my-history")
    public Page<UserMatchHistoryResponse> getMyHistory(@AuthenticationPrincipal User user, Pageable pageable) {
        return matchService.getMyMatchHistory(user, pageable);
    }
}
