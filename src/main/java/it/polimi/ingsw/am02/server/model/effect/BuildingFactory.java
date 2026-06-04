package it.polimi.ingsw.am02.server.model.effect;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.buildingeffects.*;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.enumerations.EventType;
import it.polimi.ingsw.am02.server.model.player.Player;

/**
 * Static factory that instantiates the correct {@link BuildingEffect} implementation
 * for a given effect type string and its JSON parameters.
 *
 * <p>Each case in the switch corresponds to one entry in {@code Buildings.JSON}.
 */
public class BuildingFactory {

    /**
     * Creates the active {@link BuildingEffect} for a building card.
     *
     * @param effectType   the effect type string from the building's JSON definition
     * @param effectParams the JSON parameters for the effect
     * @param owner        the player who owns the building
     * @param game         the current {@link Game} instance (needed for phase/event observer registration)
     * @return the instantiated {@link BuildingEffect}, or {@code null} if {@code effectType} is {@code null}
     *         or unrecognized
     */
    public static BuildingEffect createActiveEffect(String effectType, JsonNode effectParams, Player owner, Game game) {

        if (effectType == null) {
            return null;
        }

        return switch (effectType) {

            // TribuObserver effects

            case "FULL_SET_FOOD_REWARD" -> new FullSetFoodRewardEffect(
                    owner,
                    effectParams.get("foodReward").asInt()
            );

            case "INVENTOR_PAIR_FOOD_REWARD" -> new InventorPairRewardEffect(
                    owner,
                    effectParams.get("foodReward").asInt()
            );

            // EventObserver effects

            case "EVENT_CHARACTER_BONUS" -> new EventCharacterBonusEffect(
                    owner,
                    EventType.valueOf(effectParams.get("eventType").asText()),
                    CharacterType.valueOf(effectParams.get("characterType").asText()),
                    effectParams.get("foodReward").asInt(),
                    effectParams.get("prestigeReward").asInt(),
                    effectParams.get("foodDiscount").asInt()
            );

            case "EXTRA_SHAMAN_STARS" -> new ShamanicExtraIconsEffect(
                    owner,
                    effectParams.get("bonusStars").asInt()
            );

            case "SHAMANIC_IMMUNITY" -> new ShamanicImmunityEffect(owner);

            case "SHAMANIC_WIN_MULTIPLIER" -> new ShamanicWinMultiplierEffect(
                    owner,
                    effectParams.get("multiplier").asInt()
            );

            // PhaseObserver effects

            case "TURN_ORDER_FOOD_BONUS" -> new TurnOrderFoodBonusEffect(
                    owner,
                    game
            );

            case "ENDGAME_CHARACTER_PP" -> new EndGameCharacterPrestigeEffect(
                    owner,
                    CharacterType.valueOf(effectParams.get("characterType").asText()),
                    effectParams.get("bonusPP").asInt()
            );

            case "ENDGAME_FULL_SET_PP" -> new EndGameFullSetPPEffect(
                    owner,
                    effectParams.get("bonusPP").asInt()
            );

            case "PP_MULTIPLIER_PER_TYPE" -> new PrestigePointMultiplierPerTypeEffect(
                    owner,
                    CharacterType.valueOf(effectParams.get("characterType").asText()),
                    effectParams.get("multiplier").asInt()
            );

            case "ENDGAME_FLAT_PP" -> new FlatPrestigeBonusEffect(
                    owner,
                    effectParams.get("bonusPP").asInt()
            );

            case "EXTRA_TURN" -> new ExtraTurnEffect(
                    owner,
                    game,
                    effectParams.get("extraUpperPicks").asInt(),
                    effectParams.get("extraLowerPicks").asInt()
            );

            default -> {
                System.err.println("Unknown effect type: " + effectType);
                yield null;
            }
        };
    }
}