package com.tfxsoftware.memserver.users;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    private String username;

    private String hashedPassword;

    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    public enum UserRole {
        USER, ADMIN
    }
}