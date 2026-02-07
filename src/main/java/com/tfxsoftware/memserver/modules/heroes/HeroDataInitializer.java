package com.tfxsoftware.memserver.modules.heroes;

import com.tfxsoftware.memserver.modules.heroes.Hero.HeroArchetype;
import com.tfxsoftware.memserver.modules.heroes.Hero.MetaTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeroDataInitializer implements CommandLineRunner {

    private final HeroRepository heroRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Synchronizing Hero pool with Archetypes (22 Heroes)...");

        List<Hero> seedHeroes = List.of(
            // --- MID ---
            createHero("Luxana", "MID", MetaTier.S, "SUPPORT", MetaTier.B, HeroArchetype.MAGE),
            createHero("Ignis", "MID", MetaTier.A, null, null, HeroArchetype.MAGE),
            createHero("Vortex", "MID", MetaTier.S, "JUNGLE", MetaTier.C, HeroArchetype.ASSASSIN),
            createHero("Aurelia", "MID", MetaTier.A, null, null, HeroArchetype.MAGE),
            createHero("Zenith", "MID", MetaTier.B, null, null, HeroArchetype.MAGE),
            
            // --- JUNGLE ---
            createHero("Storm Spirit", "JUNGLE", MetaTier.S, "TOP", MetaTier.D, HeroArchetype.ASSASSIN),
            createHero("Shadow", "JUNGLE", MetaTier.A, null, null, HeroArchetype.ASSASSIN),
            createHero("Fenris", "JUNGLE", MetaTier.S, "TOP", MetaTier.B, HeroArchetype.BRUISER),
            createHero("Kraken", "JUNGLE", MetaTier.B, "SUPPORT", MetaTier.D, HeroArchetype.TANK),
            createHero("Rengar", "JUNGLE", MetaTier.A, "TOP", MetaTier.C, HeroArchetype.ASSASSIN),

            // --- ADC ---
            createHero("Vail", "CARRY", MetaTier.S, null, null, HeroArchetype.MARKSMAN),
            createHero("Bolt", "CARRY", MetaTier.B, null, null, HeroArchetype.MARKSMAN),
            createHero("Cinder", "CARRY", MetaTier.A, "MID", MetaTier.C, HeroArchetype.MARKSMAN),
            createHero("Riptide", "CARRY", MetaTier.S, null, null, HeroArchetype.MARKSMAN),
            createHero("Jinx", "CARRY", MetaTier.B, null, null, HeroArchetype.MARKSMAN),

            // --- TOP ---
            createHero("IronClad", "TOP", MetaTier.A, "JUNGLE", MetaTier.B, HeroArchetype.TANK),
            createHero("Goliath", "TOP", MetaTier.S, null, null, HeroArchetype.BRUISER),
            createHero("Atlas", "TOP", MetaTier.A, "SUPPORT", MetaTier.B, HeroArchetype.TANK),
            createHero("Katarina", "TOP", MetaTier.S, "MID", MetaTier.C, HeroArchetype.ASSASSIN),

            // --- SUPPORT ---
            createHero("Seraphina", "SUPPORT", MetaTier.S, null, null, HeroArchetype.ENCHANTER),
            createHero("Thorn", "SUPPORT", MetaTier.A, "TOP", MetaTier.D, HeroArchetype.TANK),
            createHero("Echo", "SUPPORT", MetaTier.B, "MID", MetaTier.D, HeroArchetype.ENCHANTER)
        );

        seedHeroes.forEach(this::upsertHero);
        log.info("Hero synchronization complete. Total: {}", heroRepository.count());
    }

    private void upsertHero(Hero seed) {
        heroRepository.findByName(seed.getName()).ifPresentOrElse(
            existing -> {
                existing.setPictureUrl(seed.getPictureUrl());
                existing.setPrimaryRole(seed.getPrimaryRole());
                existing.setPrimaryTier(seed.getPrimaryTier());
                existing.setSecondaryRole(seed.getSecondaryRole());
                existing.setSecondaryTier(seed.getSecondaryTier());
                existing.setArchetype(seed.getArchetype());
                heroRepository.save(existing);
            },
            () -> heroRepository.save(seed)
        );
    }

    private Hero createHero(String name, String pR, MetaTier pT, String sR, MetaTier sT, HeroArchetype arch) {
        return Hero.builder()
                .name(name)
                .pictureUrl("https://api.dicebear.com/7.x/pixel-art/svg?seed=" + name)
                .primaryRole(Hero.HeroRole.valueOf(pR))
                .primaryTier(pT)
                .secondaryRole(sR != null ? Hero.HeroRole.valueOf(sR) : null)
                .secondaryTier(sT)
                .archetype(arch)
                .build();
    }
}