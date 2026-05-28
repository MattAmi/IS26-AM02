package it.polimi.ingsw.am02.common.enumerations;

/**
 * Enumerates every resource tracked per player and reported in
 * {@link it.polimi.ingsw.am02.common.dto.ResourceDelta} notifications.
 *
 * <ul>
 *   <li>{@link #PRESTIGE_POINTS}   — the main victory-point currency</li>
 *   <li>{@link #FOOD}              — used to sustain the tribe and purchase buildings</li>
 *   <li>{@link #SHAMAN_STARS}      — used in Shamanic Ritual event scoring</li>
 *   <li>{@link #FOOD_DISCOUNT}     — cumulative reduction to food costs (Gatherers, some buildings)</li>
 *   <li>{@link #BUILDING_DISCOUNT} — cumulative reduction to building purchase costs (Builders)</li>
 *   <li>{@link #PP_BUILDERS}       — prestige points credited specifically to Builder characters</li>
 *   <li>{@link #PP_BUILDINGS}      — prestige points credited to owned buildings</li>
 * </ul>
 */
public enum ResourceType {
    PRESTIGE_POINTS, FOOD, SHAMAN_STARS, FOOD_DISCOUNT, BUILDING_DISCOUNT, PP_BUILDERS, PP_BUILDINGS
}
