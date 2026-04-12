package it.polimi.ingsw.am02.model;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.model.BuildingEffects.*;
import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.EventType;

public class BuildingFactory {

    public static BuildingEffect createActiveEffect(String effectType, JsonNode effectParams,
                                                    Tribu tribu, Player player, Game game) {
        if (effectType == null) {
            return null;
        }

        return switch (effectType) {

            // TribuObserver effects

            case "FULL_SET_FOOD_REWARD" -> new FullSetFoodRewardEffect(
                    tribu,
                    effectParams.get("foodReward").asInt()
            );

            case "INVENTOR_PAIR_FOOD_REWARD" -> new InventorPairRewardEffect(
                    tribu,
                    effectParams.get("foodReward").asInt()
            );

            // EventObserver effects

            case "EVENT_CHARACTER_BONUS" -> new EventCharacterBonusEffect(
                    EventType.valueOf(effectParams.get("eventType").asText()),
                    CharacterType.valueOf(effectParams.get("characterType").asText()),
                    effectParams.get("foodReward").asInt(),
                    effectParams.get("prestigeReward").asInt(),
                    effectParams.get("foodDiscount").asInt(),
                    tribu
            );

            case "EXTRA_SHAMAN_STARS" -> new ShamanicExtraIconsEffect(
                    tribu,
                    effectParams.get("bonusStars").asInt()
            );

            case "SHAMANIC_IMMUNITY" -> new ShamanicImmunityEffect(tribu);

            case "SHAMANIC_WIN_MULTIPLIER" -> new ShamanicWinMultiplierEffect(
                    tribu,
                    game,
                    effectParams.get("multiplier").asInt()
            );

            // PhaseObserver effects

            case "TURN_ORDER_FOOD_BONUS" -> new TurnOrderFoodBonusEffect(
                    player,
                    game
            );

            case "ENDGAME_CHARACTER_PP" -> new EndGameCharacterPrestigeEffect(
                    tribu,
                    CharacterType.valueOf(effectParams.get("characterType").asText()),
                    effectParams.get("bonusPP").asInt()
                        );

            case "ENDGAME_FULL_SET_PP" -> new EndGameFullSetPPEffect(
                    effectParams.get("bonusPP").asInt(),
                    tribu
            );

            case "PP_MULTIPLIER_PER_TYPE" -> new PrestigePointMultiplierPerTypeEffect(
                    effectParams.get("multiplier").asInt(),
                    CharacterType.valueOf(effectParams.get("characterType").asText()),
                    tribu
            );

            case "ENDGAME_FLAT_PP" -> new FlatPrestigeBonusEffect(
                    tribu,
                    effectParams.get("bonusPP").asInt()
            );

            case "EXTRA_TURN" -> new ExtraTurnEffect(
                    player,
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