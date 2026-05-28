package it.polimi.ingsw.am02.server.model.enumerations;

/**
 * Enumerates the ten invention types that Inventor characters can contribute.
 * At game end, the number of distinct invention types in a tribe determines
 * the Inventor prestige-point bonus.
 *
 * <p>Pairs of identical invention types also trigger food bonuses from
 * {@link it.polimi.ingsw.am02.server.model.buildingeffects.InventorPairRewardEffect}.
 */
public enum InventionType {
    FLINT,ARROWHEAD,FISHHOOK,NECKLACE,BOWL,ROPE,STATUE,BONE_FLUTE,HIDE,BREAD
}
