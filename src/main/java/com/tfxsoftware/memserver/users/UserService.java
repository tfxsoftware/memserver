package com.tfxsoftware.memserver.users;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.tfxsoftware.memserver.users.User.UserRole;
import com.tfxsoftware.memserver.users.dto.CreateUserDto;

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
        
        return userRepository.save(user);
    }

}
