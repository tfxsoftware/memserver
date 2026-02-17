package com.tfxsoftware.memserver.modules.players.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryLevelExpDto {
    private int level;
    private long experience;
}
