package com.tfxsoftware.memserver.users;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tfxsoftware.memserver.users.dto.CreateUserDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // TESTING ROUTE ONLY
    @PostMapping
    public ResponseEntity<User> registerUser(@Valid @RequestBody CreateUserDto dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }
    
}