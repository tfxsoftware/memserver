package com.tfxsoftware.memserver.modules.heroes;

import com.tfxsoftware.memserver.modules.heroes.dto.HeroResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
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