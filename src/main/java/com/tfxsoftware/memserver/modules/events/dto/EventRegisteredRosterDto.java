package com.tfxsoftware.memserver.modules.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegisteredRosterDto {
    private String name;
    private String ownerName;
    private BigDecimal cohesion;
    private BigDecimal morale;
    /** League only: position in standings (1-based). */
    private Integer position;
    /** League only: wins. */
    private Integer wins;
    /** League only: losses. */
    private Integer losses;
}
