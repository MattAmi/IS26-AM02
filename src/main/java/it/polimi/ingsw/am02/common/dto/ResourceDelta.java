package it.polimi.ingsw.am02.common.dto;

import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import java.io.Serializable;

/**
 * Represents a single resource change for one player.
 * Produced by effects and events; aggregated into an {@link EffectOutcome}
 * and emitted to clients via
 * {@link it.polimi.ingsw.am02.common.interfaces.VirtualView#notifyPlayerResourceChanged}.
 *
 * @param playerNickname the nickname of the player whose resource changed
 * @param resource       the type of resource that changed
 * @param newValue       the new absolute value of the resource after the change
 * @param delta          the signed amount of the change (positive = gain, negative = loss)
 */
public record ResourceDelta(
        String playerNickname,
        ResourceType resource,
        int newValue,
        int delta
) implements Serializable{}
