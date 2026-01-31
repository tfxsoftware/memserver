package com.tfxsoftware.memserver.users;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@EnableMethodSecurity
public class UserController {

    //KEEPING THIS HERE FOR NOW -> TODO: DELETE LATER
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal User currentUser) {
        // If the JWT filter worked, 'currentUser' will be populated with your DB user
        return ResponseEntity.ok(Map.of(
            "message", "JWT is working!",
            "email", currentUser.getEmail(),
            "role", currentUser.getRole(),
            "balance", currentUser.getBalance()
        ));
    }
    
}