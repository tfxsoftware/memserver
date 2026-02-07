package com.tfxsoftware.memserver.modules.users;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tfxsoftware.memserver.modules.players.Player;
import com.tfxsoftware.memserver.modules.rosters.Roster;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    private String hashedPassword;

    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Region region;

    private String organizationName;

    private String organizationImageUrl;

    public enum Region {
        SOUTH_AMERICA, NORTH_AMERICA, EUROPE, CIS, ASIA
    }
    public enum UserRole {
        USER, ADMIN
    }

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private List<Roster> rosters;

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private List<Player> ownedPlayers;

    // --- UserDetails Implementation Methods ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Converts your enum into a format Spring Security understands (e.g., "ROLE_USER")
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return hashedPassword;
    }

    @Override
    public String getUsername() {
        return email; //this is stupid but we need this in order for jwt to work (if we wnat login with email)
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}