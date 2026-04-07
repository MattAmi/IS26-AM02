package it.polimi.ingsw.am02.model;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.model.Enumerations.CharacterType;
import it.polimi.ingsw.am02.model.Enumerations.Era;
import it.polimi.ingsw.am02.model.Enumerations.InventionType;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class CharacterFactory {
    private final Map<CharacterType, Function<JsonNode, CharacterEffect>> effectRegistry;

    public CharacterFactory(){
        this.effectRegistry = new EnumMap<>(CharacterType.class);
        setUpRegistry();
    }

    private void setUpRegistry(){
        effectRegistry.put(CharacterType.INVENTOR, node -> {
            String nameInJson = node.path("invention").asText();
            InventionType invention = InventionType.valueOf(nameInJson);
            return new InventorEffect(invention);
        });

        effectRegistry.put(CharacterType.BUILDER, node ->
                new BuilderEffect(
                        node.path("buildingDiscount").asInt(),
                        node.path("prestigePoints").asInt())
        );

        effectRegistry.put(CharacterType.GATHERER, node ->
                new GathererEffect(
                        node.path("foodDiscount").asInt())
        );

        effectRegistry.put(CharacterType.ARTIST, node ->
                new NoEffect()
        );

        effectRegistry.put(CharacterType.SHAMAN, node ->
                new ShamanEffect(
                        node.path("shamanStars").asInt())
        );

        effectRegistry.put(CharacterType.HUNTER, node -> {
            if (node.path("hasGigot").asBoolean()) {
                return new HunterWithGigotEffect();
            }
            return new NoEffect();
        });
    }


    public CharacterCard createCharacter(JsonNode node){
        String cardID = node.path("cardID").asText();
        Era era = Era.valueOf(node.path("era").asText());
        CharacterType type = CharacterType.valueOf(node.path("type").asText());
        int minPlayers = node.path("minPlayers").asInt();

        CharacterEffect characterEffect = effectRegistry
                .getOrDefault(type, n -> new NoEffect())
                .apply(node);

        return new CharacterCard(cardID, era, type, minPlayers, characterEffect);
    }

}
