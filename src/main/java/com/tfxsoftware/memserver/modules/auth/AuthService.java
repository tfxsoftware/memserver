package com.tfxsoftware.memserver.modules.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tfxsoftware.memserver.modules.auth.dto.SignInDto;
import com.tfxsoftware.memserver.modules.auth.dto.SignInResponse;
import com.tfxsoftware.memserver.modules.auth.dto.SignUpDto;
import com.tfxsoftware.memserver.modules.users.User;
import com.tfxsoftware.memserver.modules.users.UserService;
import com.tfxsoftware.memserver.modules.users.dto.CreateUserDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void signUp(SignUpDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        String username = dto.getUsername().trim();

        if (userService.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        if (userService.existsByUsername(username)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
        }

        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        
        CreateUserDto createUserDto = new CreateUserDto();
        createUserDto.setEmail(email);
        createUserDto.setUsername(username);
        createUserDto.setHashedPassword(hashedPassword);
        createUserDto.setRegion(dto.getRegion());
        createUserDto.setOrganizationName(dto.getOrganizationName());
        createUserDto.setOrganizationImageUrl(dto.getOrganizationImageUrl());

        try {
            userService.createUser(createUserDto);
        } catch (Exception ex) {
            // Log the actual exception for debugging
            System.err.println("[ERROR] Failed to create user: " + ex.getMessage());
            ex.printStackTrace();
            
            if (ex instanceof ResponseStatusException) {
                throw (ResponseStatusException) ex;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage());
        }
    }

public SignInResponse signIn(SignInDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        
        // 1. Find user (This handles the 404/401 logic we discussed)
        User user;
        try {
            user = userService.getUserByEmail(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // 2. Check Password
        if (!passwordEncoder.matches(dto.getPassword(), user.getHashedPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // 3. Generate Token
        String token = jwtService.generateToken(user.getEmail());

        return new SignInResponse(token);
    }
}
