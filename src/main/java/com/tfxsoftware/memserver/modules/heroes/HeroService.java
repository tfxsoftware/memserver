package com.tfxsoftware.memserver.modules.heroes;

import com.tfxsoftware.memserver.modules.heroes.dto.HeroMetaEntryDto;
import com.tfxsoftware.memserver.modules.heroes.dto.HeroResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeroService {

    private final HeroRepository heroRepository;

    /**
     * Fetches all heroes and transforms them into DTOs using the flat structure.
     */
    @Transactional(readOnly = true)
    public List<HeroResponse> getAllHeroes() {
        return heroRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return heroRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public List<Hero> findAll() {
        return heroRepository.findAll();
    }

    /**
     * Returns all heroes grouped by role, each entry with hero id and meta strength for that role.
     * A hero appears under each role they have (primary and/or secondary) with the corresponding tier.
     */
    @Transactional(readOnly = true)
    public Map<Hero.HeroRole, List<HeroMetaEntryDto>> getHeroesByMeta() {
        List<Hero> heroes = heroRepository.findAll();
        Map<Hero.HeroRole, List<HeroMetaEntryDto>> byRole = new EnumMap<>(Hero.HeroRole.class);
        for (Hero.HeroRole role : Hero.HeroRole.values()) {
            byRole.put(role, new ArrayList<>());
        }
        for (Hero hero : heroes) {
            byRole.get(hero.getPrimaryRole()).add(HeroMetaEntryDto.builder()
                    .hero(hero.getId())
                    .metastrength(hero.getPrimaryTier())
                    .build());
            if (hero.getSecondaryRole() != null && hero.getSecondaryTier() != null) {
                byRole.get(hero.getSecondaryRole()).add(HeroMetaEntryDto.builder()
                        .hero(hero.getId())
                        .metastrength(hero.getSecondaryTier())
                        .build());
            }
        }
        byRole.values().forEach(list -> list.sort(Comparator.comparing(HeroMetaEntryDto::getMetastrength)));
        return byRole;
    }

    /**
     * Maps the Hero entity to a HeroResponse DTO.
     * This version uses the cleaner Primary/Secondary role fields.
     */
    private HeroResponse mapToResponse(Hero hero) {
        return HeroResponse.builder()
                .id(hero.getId())
                .name(hero.getName())
                .pictureUrl(hero.getPictureUrl())
                .primaryRole(hero.getPrimaryRole())
                .primaryTier(hero.getPrimaryTier())
                .secondaryRole(hero.getSecondaryRole())
                .secondaryTier(hero.getSecondaryTier())
                .archetype(hero.getArchetype())
                .build();
    }
}