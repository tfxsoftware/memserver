package com.tfxsoftware.memserver.modules.users;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.tfxsoftware.memserver.modules.users.User.UserRole;
import com.tfxsoftware.memserver.modules.users.dto.CreateUserDto;

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

        return userRepository.save(user);
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
