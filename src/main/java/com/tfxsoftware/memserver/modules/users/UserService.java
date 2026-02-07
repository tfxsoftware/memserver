package com.tfxsoftware.memserver.modules.users;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.tfxsoftware.memserver.modules.users.User.UserRole;
import com.tfxsoftware.memserver.modules.users.dto.CreateUserDto;
import com.tfxsoftware.memserver.modules.users.dto.UpdateUserDto;
import com.tfxsoftware.memserver.modules.users.dto.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final BigDecimal DEFAULT_BALANCE = new BigDecimal("10000");
    private final UserRepository userRepository;

    public User createUser(CreateUserDto userDto){
        User user = new User();
        
        user.setEmail(userDto.getEmail());
        user.setUsername(userDto.getUsername());
        user.setHashedPassword(userDto.getHashedPassword());
        user.setBalance(DEFAULT_BALANCE);
        user.setRole(UserRole.USER);
        user.setRegion(userDto.getRegion());
        user.setOrganizationName(userDto.getOrganizationName());
        user.setOrganizationImageUrl(userDto.getOrganizationImageUrl());

        return userRepository.save(user);
    }

    public UserResponse updateOrganization(User currentUser, UpdateUserDto dto) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (dto.getOrganizationName() != null) {
            user.setOrganizationName(dto.getOrganizationName());
        }
        if (dto.getOrganizationImageUrl() != null) {
            user.setOrganizationImageUrl(dto.getOrganizationImageUrl());
        }

        return mapToResponse(userRepository.save(user));
    }

    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .balance(user.getBalance())
                .role(user.getRole())
                .region(user.getRegion())
                .organizationName(user.getOrganizationName())
                .organizationImageUrl(user.getOrganizationImageUrl())
                .build();
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public boolean existsByEmail(String email) {   
        return userRepository.existsByEmailIgnoreCase(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

}
