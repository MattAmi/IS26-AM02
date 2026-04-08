package it.polimi.ingsw.am02.model;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.model.BuildingEffects.*;
import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.EventType;

public class BuildingFactory {

    public static BuildingEffect createActiveEffect(String effectType, JsonNode effectParams, Tribu tribu, Player player, Game game) {

        if (effectType == null) {
            return null;
        }

        return switch (effectType) {

            // TribuObserver effects

            case "ON_SET_OF_SIX_FOOD" -> new FullSetFoodRewardEffect(
                    tribu,
                    effectParams.get("foodReward").asInt()
            );

            case "ON_INVENTOR_PAIR_FOOD" -> new InventorPairRewardEffect(
                    tribu,
                    effectParams.get("foodReward").asInt()
            );

            // EventObserver effects

            case "CAVE_PAINTING_FOOD_PER_ARTIST", "HUNT_EVENT_EXTRA_BONUS" ->
                    new EventCharacterBonusEffect(
                            EventType.valueOf(effectParams.get("eventType").asText()),
                            CharacterType.valueOf(effectParams.get("targetCharacter").asText()),
                            effectParams.get("foodReward").asInt(),
                            effectParams.get("prestigeReward").asInt(),
                            effectParams.get("foodDiscount").asInt(),
                            tribu
                    );

            case "IMMUNITY_SHAMAN_MINORITY" -> new ShamanicImmunityEffect(tribu);

            case "EXTRA_SHAMAN_STARS" -> new ShamanicExtraIconsEffect(
                    tribu,
                    effectParams.get("bonusStars").asInt()
            );

            case "DOUBLE_PP_SHAMAN_MAJORITY" -> new ShamanicWinMultiplierEffect(
                    tribu,
                    game,
                    effectParams.get("multiplier").asInt()
            );

            // PhaseObserver effects

            case "BONUS_FOOD_TURN_ORDER" -> new TurnOrderFoodBonusEffect(
                    player,
                    game
            );

            case "ENDGAME_PP_PER_TYPE" -> new PrestigePointMultiplierPerTypeEffect(
                    effectParams.get("ppPerCharachter").asInt(),
                    CharacterType.valueOf(effectParams.get("characterType").asText()),
                    tribu
            );

            case "ENDGAME_PP_SET_OF_SIX" -> new EndGameFullSetPPEffect(
                    effectParams.get("bonusPP").asInt(),
                    tribu
            );

            case "FLAT_ENDGAME_PP" -> new FlatPrestigeBonusEffect(
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
