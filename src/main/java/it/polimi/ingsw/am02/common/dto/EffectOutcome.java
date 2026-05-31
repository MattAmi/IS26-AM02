package it.polimi.ingsw.am02.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Aggregates the resource changes produced by a single card effect,
 * event resolution, or building effect callback.
 *
 * <p>Multiple {@code EffectOutcome}s produced in the same step are
 * merged and emitted via
 * {@link it.polimi.ingsw.am02.server.model.listeners.GameEventEmitter#emitOutcome(EffectOutcome)}.
 *
 * @param resourceDeltas the list of per-player, per-resource deltas; never {@code null}
 */
public record EffectOutcome(List<ResourceDelta> resourceDeltas) implements Serializable {

    /**
     * @return a singleton empty outcome (no resource changes), safe to return from no-op effects
     */
    public static EffectOutcome empty() {
        return new EffectOutcome(List.of());
    }

    /**
     * @return {@code true} if this outcome contains no resource deltas
     */
    public boolean isEmpty() {
        return resourceDeltas == null || resourceDeltas.isEmpty();
    }
}
