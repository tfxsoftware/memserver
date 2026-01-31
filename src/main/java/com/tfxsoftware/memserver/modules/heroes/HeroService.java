package com.tfxsoftware.memserver.modules.heroes;

import org.springframework.stereotype.Service;

import com.tfxsoftware.memserver.modules.heroes.dto.HeroResponse;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
class HeroService {
    public HeroResponse getAllHeroes(){
        return new HeroResponse();
    }
}
