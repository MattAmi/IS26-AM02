package it.polimi.ingsw.am02.server.model.card;
import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingsw.am02.server.model.effect.*;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.Era;
import it.polimi.ingsw.am02.server.model.enumerations.InventionType;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory that deserializes a {@link CharacterCard} (with the appropriate {@link CharacterEffect})
 * from a Jackson {@link JsonNode}.
 *
 * <p>Uses an internal registry that maps each {@link CharacterType} to a lambda
 * responsible for constructing the corresponding effect from JSON parameters.
 */
public class CharacterFactory {

    private final Map<CharacterType, Function<JsonNode, CharacterEffect>> effectRegistry;

    /** Constructs the factory and populates the internal effect registry. */
    public CharacterFactory() {
        this.effectRegistry = new EnumMap<>(CharacterType.class);
        setUpRegistry();
    }

    private void setUpRegistry() {
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

    /**
     * Creates a {@link CharacterCard} from the given JSON node.
     *
     * @param node the JSON object representing one character card entry
     * @return the fully constructed {@link CharacterCard}
     */
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
