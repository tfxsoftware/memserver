package com.tfxsoftware.memserver.modules.heroes;

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
        log.info("Synchronizing Hero pool in modules folder (Flat Structure)...");

        List<Hero> seedHeroes = List.of(
            // MID
            createHero("Luxana", "MID", Hero.MetaTier.S, "SUPPORT", Hero.MetaTier.B),
            createHero("Ignis", "MID", Hero.MetaTier.A, null, null),
            createHero("Vortex", "MID", Hero.MetaTier.S, "JUNGLE", Hero.MetaTier.C),
            createHero("Aurelia", "MID", Hero.MetaTier.A, null, null),
            createHero("Zenith", "MID", Hero.MetaTier.B, null, null),
            
            // JUNGLE
            createHero("Storm Spirit", "JUNGLE", Hero.MetaTier.S, "TOP", Hero.MetaTier.D),
            createHero("Shadow", "JUNGLE", Hero.MetaTier.A, null, null),
            createHero("Fenris", "JUNGLE", Hero.MetaTier.S, "TOP", Hero.MetaTier.B),
            createHero("Kraken", "JUNGLE", Hero.MetaTier.B, "SUPPORT", Hero.MetaTier.D),
            createHero("Rengar", "JUNGLE", Hero.MetaTier.A, "TOP", Hero.MetaTier.C),

            // ADC
            createHero("Vail", "ADC", Hero.MetaTier.S, null, null),
            createHero("Bolt", "ADC", Hero.MetaTier.B, null, null),
            createHero("Cinder", "ADC", Hero.MetaTier.A, "MID", Hero.MetaTier.C),
            createHero("Riptide", "ADC", Hero.MetaTier.S, null, null),
            createHero("Ghost", "ADC", Hero.MetaTier.A, null, null),

            // TOP
            createHero("IronClad", "TOP", Hero.MetaTier.A, "JUNGLE", Hero.MetaTier.B),
            createHero("Goliath", "TOP", Hero.MetaTier.S, null, null),
            createHero("Atlas", "TOP", Hero.MetaTier.A, "SUPPORT", Hero.MetaTier.B),
            createHero("Katarina", "TOP", Hero.MetaTier.S, "MID", Hero.MetaTier.C),

            // SUPPORT
            createHero("Seraphina", "SUPPORT", Hero.MetaTier.S, null, null),
            createHero("Thorn", "SUPPORT", Hero.MetaTier.A, "TOP", Hero.MetaTier.D),
            createHero("Echo", "SUPPORT", Hero.MetaTier.B, "MID", Hero.MetaTier.D)
        );

        seedHeroes.forEach(this::upsertHero);
        log.info("Hero synchronization complete. Total: {}", heroRepository.count());
    }

    private void upsertHero(Hero seedHero) {
        heroRepository.findByName(seedHero.getName()).ifPresentOrElse(
            existing -> {
                existing.setPictureUrl(seedHero.getPictureUrl());
                existing.setPrimaryRole(seedHero.getPrimaryRole());
                existing.setPrimaryTier(seedHero.getPrimaryTier());
                existing.setSecondaryRole(seedHero.getSecondaryRole());
                existing.setSecondaryTier(seedHero.getSecondaryTier());
                heroRepository.save(existing);
            },
            () -> heroRepository.save(seedHero)
        );
    }

    private Hero createHero(String name, String pRole, Hero.MetaTier pTier, String sRole, Hero.MetaTier sTier) {
        return Hero.builder()
                .name(name)
                .pictureUrl("https://api.dicebear.com/7.x/pixel-art/svg?seed=" + name)
                .primaryRole(Hero.HeroRole.valueOf(pRole))
                .primaryTier(pTier)
                .secondaryRole(sRole != null ? Hero.HeroRole.valueOf(sRole) : null)
                .secondaryTier(sTier)
                .build();
    }
}