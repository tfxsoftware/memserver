package com.tfxsoftware.memserver.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfxsoftware.memserver.auth.dto.SignUpDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> register(@RequestBody @Valid SignUpDto dto) {
        SignUpResponse response = authService.register(dto);
        // Use 201 Created for successful signups
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}
