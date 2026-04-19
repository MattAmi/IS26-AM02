package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;
import java.util.List;

public record EffectOutcome(List<ResourceDelta> resourceDeltas) implements Serializable {
    public static EffectOutcome empty() {
        return new EffectOutcome(List.of());
    }
    public boolean isEmpty() {
        return resourceDeltas == null || resourceDeltas.isEmpty();
    }
}
