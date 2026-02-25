package com.tfxsoftware.memserver.modules.heroes.dto;

import com.tfxsoftware.memserver.modules.heroes.Hero.MetaTier;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class HeroMetaEntryDto {
    private UUID hero;
    private MetaTier metastrength;
}
