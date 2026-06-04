package it.polimi.ingsw.am02.server.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.buildingeffects.*;
import it.polimi.ingsw.am02.server.model.effect.BuildingEffect;
import it.polimi.ingsw.am02.server.model.effect.BuildingFactory;
import it.polimi.ingsw.am02.server.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class BuildingFactoryTest {

    private Player player;
    private Game game;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        player = new Player("Bob", Totem.BLUE);
        game = Mockito.mock(Game.class);
        mapper = new ObjectMapper();
    }

    @Test
    void testCreateFullSetFoodRewardEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("foodReward", 3);
        BuildingEffect effect = BuildingFactory.createActiveEffect("FULL_SET_FOOD_REWARD", params, player, game);
        assertInstanceOf(FullSetFoodRewardEffect.class, effect);
    }

    @Test
    void testCreateInventorPairRewardEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("foodReward", 2);
        BuildingEffect effect = BuildingFactory.createActiveEffect("INVENTOR_PAIR_FOOD_REWARD", params, player, game);
        assertInstanceOf(InventorPairRewardEffect.class, effect);
    }

    @Test
    void testCreateEventCharacterBonusEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("eventType", "SUSTENANCE");
        params.put("characterType", "GATHERER");
        params.put("foodReward", 1);
        params.put("prestigeReward", 2);
        params.put("foodDiscount", 1);
        BuildingEffect effect = BuildingFactory.createActiveEffect("EVENT_CHARACTER_BONUS", params, player, game);
        assertInstanceOf(EventCharacterBonusEffect.class, effect);
    }

    @Test
    void testCreateShamanicExtraIconsEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("bonusStars", 2);
        BuildingEffect effect = BuildingFactory.createActiveEffect("EXTRA_SHAMAN_STARS", params, player, game);
        assertInstanceOf(ShamanicExtraIconsEffect.class, effect);
    }

    @Test
    void testCreateShamanicImmunityEffect() {
        ObjectNode params = mapper.createObjectNode();
        BuildingEffect effect = BuildingFactory.createActiveEffect("SHAMANIC_IMMUNITY", params, player, game);
        assertInstanceOf(ShamanicImmunityEffect.class, effect);
    }

    @Test
    void testCreateShamanicWinMultiplierEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("multiplier", 2);
        BuildingEffect effect = BuildingFactory.createActiveEffect("SHAMANIC_WIN_MULTIPLIER", params, player, game);
        assertInstanceOf(ShamanicWinMultiplierEffect.class, effect);
    }

    @Test
    void testCreateTurnOrderFoodBonusEffect() {
        ObjectNode params = mapper.createObjectNode();
        BuildingEffect effect = BuildingFactory.createActiveEffect("TURN_ORDER_FOOD_BONUS", params, player, game);
        assertInstanceOf(TurnOrderFoodBonusEffect.class, effect);
    }

    @Test
    void testCreateEndGameCharacterPrestigeEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("characterType", "HUNTER");
        params.put("bonusPP", 3);
        BuildingEffect effect = BuildingFactory.createActiveEffect("ENDGAME_CHARACTER_PP", params, player, game);
        assertInstanceOf(EndGameCharacterPrestigeEffect.class, effect);
    }

    @Test
    void testCreateEndGameFullSetPPEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("bonusPP", 6);
        BuildingEffect effect = BuildingFactory.createActiveEffect("ENDGAME_FULL_SET_PP", params, player, game);
        assertInstanceOf(EndGameFullSetPPEffect.class, effect);
    }

    @Test
    void testCreatePrestigePointMultiplierPerTypeEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("characterType", "BUILDER");
        params.put("multiplier", 2);
        BuildingEffect effect = BuildingFactory.createActiveEffect("PP_MULTIPLIER_PER_TYPE", params, player, game);
        assertInstanceOf(PrestigePointMultiplierPerTypeEffect.class, effect);
    }

    @Test
    void testCreateFlatPrestigeBonusEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("bonusPP", 25);
        BuildingEffect effect = BuildingFactory.createActiveEffect("ENDGAME_FLAT_PP", params, player, game);
        assertInstanceOf(FlatPrestigeBonusEffect.class, effect);
    }

    @Test
    void testCreateExtraTurnEffect() {
        ObjectNode params = mapper.createObjectNode();
        params.put("extraUpperPicks", 1);
        params.put("extraLowerPicks", 0);
        BuildingEffect effect = BuildingFactory.createActiveEffect("EXTRA_TURN", params, player, game);
        assertInstanceOf(ExtraTurnEffect.class, effect);
    }

    @Test
    void testCreateUnknownEffect() {
        ObjectNode params = mapper.createObjectNode();
        BuildingEffect effect = BuildingFactory.createActiveEffect("UNKNOWN_EFFECT", params, player, game);
        assertNull(effect);
    }

    @Test
    void testCreateNullEffect() {
        BuildingEffect effect = BuildingFactory.createActiveEffect(null, null, player, game);
        assertNull(effect);
    }
}
