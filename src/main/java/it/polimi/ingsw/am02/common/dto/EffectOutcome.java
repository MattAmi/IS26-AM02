package it.polimi.ingsw.am02.common.dto;

import java.util.List;

public record EffectOutcome(List<ResourceDelta> resourceDeltas) {
    public static EffectOutcome empty() {
        return new EffectOutcome(List.of());
    }
    public boolean isEmpty() {
        return resourceDeltas == null || resourceDeltas.isEmpty();
    }
}
