package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.server.model.enumerations.InventionType;

/**
 * Character effect for the Inventor archetype.
 * Registers one invention of a specific {@link InventionType} in the tribe.
 * Inventor prestige points are calculated at the end of the game.
 */
public class InventorEffect implements CharacterEffect {

    private final InventionType invention;

    /**
     * @param invention the type of invention contributed by this Inventor character
     */
    public InventorEffect(InventionType invention) {
        this.invention = invention;
    }

    /**
     * Adds one invention of the configured type to the player's tribe.
     * Does not produce any immediate resource delta.
     *
     * @param player the player who drew this character
     * @return an empty {@link EffectOutcome}
     */
    @Override
    public EffectOutcome applyEffect(Player player) {

        player.getTribu().addInventionType(invention);
        return EffectOutcome.empty();
    }

}

