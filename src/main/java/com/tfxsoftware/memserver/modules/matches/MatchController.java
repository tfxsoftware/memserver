package com.tfxsoftware.memserver.modules.matches;

import com.tfxsoftware.memserver.modules.matches.dto.CreateMatchDto;
import com.tfxsoftware.memserver.modules.matches.dto.MatchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse create(@RequestBody @Valid CreateMatchDto dto) {
        return matchService.create(dto);
    }
}
