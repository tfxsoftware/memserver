package com.tfxsoftware.memserver.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tfxsoftware.memserver.auth.dto.SignUpDto;
import com.tfxsoftware.memserver.users.UserService;
import com.tfxsoftware.memserver.users.User;
import com.tfxsoftware.memserver.users.dto.CreateUserDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignUpResponse register(SignUpDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        String username = dto.getUsername().trim();

        if (userService.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        if (userService.existsByUsername(username)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
        }

        String hashedPassword = passwordEncoder.encode(dto.getPassword());

        CreateUserDto createUserDto = new CreateUserDto(email, username , hashedPassword);

        try {
            User user = userService.createUser(createUserDto);
            return new SignUpResponse(user.getId().toString(), user.getEmail(), user.getUsername());
        } catch (DataIntegrityViolationException ex) {
            // rare race: DB unique constraint prevented insertion
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error while creating User!");
        }
    }
}
