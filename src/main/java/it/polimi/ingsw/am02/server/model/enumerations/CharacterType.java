package it.polimi.ingsw.am02.server.model.enumerations;

/**
 * Enumerates the six character archetypes available in the game.
 * Each type corresponds to a distinct set of abilities and effects:
 *
 * <ul>
 *   <li>{@link #INVENTOR} — contributes invention tokens and Inventor end-game scoring</li>
 *   <li>{@link #BUILDER}  — grants building discounts and Builder prestige points</li>
 *   <li>{@link #GATHERER} — reduces food cost during Sustenance events</li>
 *   <li>{@link #ARTIST}   — affects Cave Paintings event scoring</li>
 *   <li>{@link #SHAMAN}   — adds shaman stars used in Shamanic Ritual events</li>
 *   <li>{@link #HUNTER}   — provides food and prestige during Hunt events</li>
 * </ul>
 */
public enum CharacterType {
    INVENTOR,BUILDER,GATHERER,ARTIST,SHAMAN,HUNTER
}
