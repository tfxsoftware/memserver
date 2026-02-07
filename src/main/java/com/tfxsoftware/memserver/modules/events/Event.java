package com.tfxsoftware.memserver.modules.events;

import com.tfxsoftware.memserver.modules.events.league.League;
import com.tfxsoftware.memserver.modules.matches.Match;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet; // New import
import java.util.List;
import java.util.Map;
import java.util.Set; // New import
import java.util.UUID;
import com.tfxsoftware.memserver.modules.users.User;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    private String imageUrl;

    @ElementCollection(targetClass = User.Region.class) // Annotation for collection of enums
    @CollectionTable(name = "event_regions", joinColumns = @JoinColumn(name = "event_id")) // Table for regions
    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false)
    @Builder.Default
    private Set<User.Region> regions = new HashSet<>(); // Changed field to Set and initialized

    @Column(nullable = false)
    private LocalDateTime opensAt;

    private LocalDateTime startsAt;

    private LocalDateTime finishesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tier tier; // Multiplier for rewards

    @Column(nullable = false)
    private BigDecimal entryFee;

       // --- Prize Configuration ---
    @Column(nullable = false)
    private BigDecimal totalPrizePool;
    
    /**
        * Map of Rank to Prize Amount.
        * Example: { "1": 5000.00, "2": 2500.00, "3": 1000.00 }
        * This allows any event to reward any number of top players.
    */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<Integer, BigDecimal> rankPrizes;
    


    // --- Pacing Config ---
    private Integer gamesPerBlock;       
    private Integer minutesBetweenGames; //change to time so we can have more spaced games/blocks
    private Integer minutesBetweenBlocks;  

    private Integer maxPlayers;

    // --- State Tracking (Used during initial generation) ---
    @Builder.Default
    private Integer currentBlockMatchCount = 0; // WE CAN STILL HAVE BLOCKS ON LEAGUE SO ILL LEAVE THIS HERE

    // --- Type-Specific Data (Wrapped) ---
    
    /**
     * League-specific data. Only populated if type == LEAGUE.
     */
    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private League league;

    /// ill create tournament object later, mvp only requires league for now


    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventRegistration> registrations = new ArrayList<>();

    @OneToMany(mappedBy = "event")
    private List<Match> matches;

    public enum EventType { LEAGUE, TOURNAMENT, CUP }
    public enum EventStatus { CLOSED, OPEN, ONGOING, FINISHED, CANCELLED }
    public enum Tier { S, A, B, C, D }
} 
