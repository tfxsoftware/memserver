package com.tfxsoftware.memserver.modules.players;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasteryService {

    private final PlayerRoleMasteryRepository roleMasteryRepository;
    private final PlayerHeroMasteryRepository heroMasteryRepository;

    private static final long[] EXPERIENCE_TABLE = {
        0L,          // L1 (Extremely Easy Start)
        150L, 350L, 650L, 1050L, 1500L,       // L2-L6
        2500L, 4000L, 6000L, 8500L, 11500L, 15000L, // L7-L12 (Easy)
        21000L, 29000L, 40000L, 55000L, 75000L, 100000L, // L13-L18 (Medium)
        135000L, 180000L, 240000L, 320000L, 420000L, 550000L, 750000L, // L19-L25 (Hard)
        1000000L, 1350000L, 1800000L, 2400000L, 3500000L // L26-L30 (Extremely Hard)
    };

    public int calculateLevel(long totalExperience) {
        int level = 1;
        for (int i = 1; i < EXPERIENCE_TABLE.length; i++) {
            if (totalExperience >= EXPERIENCE_TABLE[i]) {
                level = i + 1;
            } else {
                break;
            }
        }
        return level;
    }

    @Transactional
    public void addRoleExperience(Player player, HeroRole role, long amount) {
        PlayerRoleMastery roleMastery = roleMasteryRepository.findByPlayerIdAndRole(player.getId(), role)
                .orElseThrow(() -> new IllegalStateException("Role mastery not found for " + role));

        roleMastery.setExperience(roleMastery.getExperience() + amount);
        roleMastery.setLevel(calculateLevel(roleMastery.getExperience()));
        roleMasteryRepository.save(roleMastery);
        log.info("Added {} experience to role {} for player {}. New level: {}", amount, role, player.getNickname(), roleMastery.getLevel());
    }

    @Transactional
    public void addHeroExperience(Player player, UUID heroId, long amount) {
        PlayerHeroMastery heroMastery = getOrCreateHeroMastery(player, heroId);

        heroMastery.setExperience(heroMastery.getExperience() + amount);
        heroMastery.setLevel(calculateLevel(heroMastery.getExperience()));
        heroMasteryRepository.save(heroMastery);
        log.info("Added {} experience to hero {} for player {}. New level: {}", amount, heroId, player.getNickname(), heroMastery.getLevel());
    }

    @Transactional
    public PlayerHeroMastery getOrCreateHeroMastery(Player player, UUID heroId) {
        return heroMasteryRepository.findByPlayerIdAndHeroId(player.getId(), heroId)
                .orElseGet(() -> {
                    PlayerHeroMastery mastery = PlayerHeroMastery.builder()
                            .player(player)
                            .heroId(heroId)
                            .level(1)
                            .experience(0L)
                            .build();
                    return heroMasteryRepository.save(mastery);
                });
    }
}
