// DONE is a mock

package it.polimi.ingsw.am02.server.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am02.common.ResourcePaths;
import it.polimi.ingsw.am02.server.model.card.*;
import it.polimi.ingsw.am02.server.model.tile.OfferTile;
import it.polimi.ingsw.am02.server.model.tile.OfferTilesFactory;
import it.polimi.ingsw.am02.server.model.tile.TurnOrderTile;
import it.polimi.ingsw.am02.server.model.tile.TurnOrderTilesFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton registry that loads and provides access to all static game data:
 * character cards, event cards, building cards, offer tiles, and turn-order tiles.
 *
 * <p>All data is loaded from JSON resource files at class-initialization time.
 * Use {@link #getInstance()} to obtain the single shared instance.
 */
public class GameRegistry {

    private static final GameRegistry INSTANCE = new GameRegistry(); //the only instance of the registry

    private final Map<String, CharacterCard> characterMap;
    private final Map<String, EventCard> eventMap;
    private final Map<String, BuildingCard> buildingMap;
    private final List<OfferTile> offerTiles;
    private final List<TurnOrderTile> turnOrderTiles;

    private final ObjectMapper mapper;

    private GameRegistry() {
        this.mapper = new ObjectMapper();
        this.characterMap = new HashMap<>();
        this.eventMap = new HashMap<>();
        this.buildingMap = new HashMap<>();
        this.offerTiles = new ArrayList<>();
        this.turnOrderTiles = new ArrayList<>();

        loadCharacters(ResourcePaths.CHARACTERS);
        loadEvents(ResourcePaths.EVENTS);
        loadBuildings(ResourcePaths.BUILDINGS);
        loadOfferTiles(ResourcePaths.OFFER_TILES);
        loadTurnOrderTiles(ResourcePaths.TURN_ORDER_TILES);
    }

    /** @return the singleton instance of the registry */
    public static GameRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Loads character cards from the given JSON resource path, replacing any previously loaded characters.
     * Normally called once during construction; exposed for testing with custom data files.
     *
     * @param charactersPath classpath-relative path to the JSON file (e.g. {@code "/it/.../Characters.json"})
     */
    public void loadCharacters(String charactersPath) {
        try {
            List<JsonNode> nodes = parseJsonToList(charactersPath);
            CharacterFactory characterFactory = new CharacterFactory();

            for(JsonNode node : nodes) {
                CharacterCard characterCard = characterFactory.createCharacter(node);
                characterMap.put(characterCard.getID(), characterCard);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads event cards from the given JSON resource path.
     *
     * @param eventsPath classpath-relative path to the JSON file
     */
    public void loadEvents(String eventsPath) {
        try {
            List<JsonNode> nodes = parseJsonToList(eventsPath);
            EventFactory eventFactory = new EventFactory();

            for(JsonNode node : nodes) {
                EventCard eventCard = eventFactory.createEvent(node);
                eventMap.put(eventCard.getID(), eventCard);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads building cards from the given JSON resource path.
     *
     * @param buildingsPath classpath-relative path to the JSON file
     */
    public void loadBuildings(String buildingsPath) {
        try {
            List<JsonNode> nodes = parseJsonToList(buildingsPath);
            BuildingCardFactory buildingCardFactory = new BuildingCardFactory();

            for (JsonNode node : nodes) {
                BuildingCard buildingCard = buildingCardFactory.createBuilding(node);
                buildingMap.put(buildingCard.getCardID(), buildingCard);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads offer tile definitions from the given JSON resource path.
     *
     * @param offerTilesPath classpath-relative path to the JSON file
     */
    public void loadOfferTiles(String offerTilesPath) {
        try {
            List<JsonNode> nodes = parseJsonToList(offerTilesPath);
            OfferTilesFactory offerTilesFactory = new OfferTilesFactory();
            for (JsonNode node : nodes) {
                OfferTile offerTile = offerTilesFactory.createOfferTile(node);
                offerTiles.add(offerTile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads turn-order tile definitions from the given JSON resource path.
     *
     * @param turnOrderTilesPath classpath-relative path to the JSON file
     */
    public void loadTurnOrderTiles(String turnOrderTilesPath) {
        try {
            List<JsonNode> nodes = parseJsonToList(turnOrderTilesPath);
            TurnOrderTilesFactory turnOrderTilesFactory = new TurnOrderTilesFactory();
            for (JsonNode node : nodes) {
                TurnOrderTile turnOrderTile = turnOrderTilesFactory.createTurnOrderTile(node);
                turnOrderTiles.add(turnOrderTile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<JsonNode> parseJsonToList(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null)
                throw new IOException("Resource not found: " + resourcePath);

            JsonNode rootNode = mapper.readTree(is);
            List<JsonNode> nodes = new ArrayList<>();
            if (rootNode.isArray()) rootNode.forEach(nodes::add);
            return nodes;
        }
    }

    /**
     * @param cardID the card ID to check
     * @return {@code true} if the ID corresponds to a character card
     */
    public boolean isCharacter(String cardID) {
        return characterMap.containsKey(cardID);
    }

    /**
     * @param cardID the card ID to check
     * @return {@code true} if the ID corresponds to an event card
     */
    public boolean isEvent(String cardID) {
        return eventMap.containsKey(cardID);
    }

    /**
     * @param cardID the card ID to check
     * @return {@code true} if the ID corresponds to a building card
     */
    public boolean isBuilding(String cardID) {
        return buildingMap.containsKey(cardID);
    }

    /**
     * @param cardID the character card ID
     * @return the corresponding {@link CharacterCard}, or {@code null} if not found
     */
    public CharacterCard getCharacter(String cardID) {
        return characterMap.get(cardID);
    }

    /**
     * @param cardID the event card ID
     * @return the corresponding {@link EventCard}, or {@code null} if not found
     */
    public EventCard getEvent(String cardID) {
        return eventMap.get(cardID);
    }

    /**
     * @param cardID the building card ID
     * @return the corresponding {@link BuildingCard}, or {@code null} if not found
     */
    public BuildingCard getBuilding(String cardID) {
        return buildingMap.get(cardID);
    }

    /**
     * Returns all offer tiles valid for the given player count
     * (i.e. tiles whose {@code minPlayers} is {@code <= numPlayers}).
     *
     * @param numPlayers the number of players in the current game
     * @return a list of qualifying {@link OfferTile} templates
     */
    public List<OfferTile> getOfferTiles(int numPlayers) {
        List<OfferTile> myOfferTiles = new ArrayList<>();
        for (OfferTile offerTile : offerTiles){
            if(offerTile.getMinPlayers() <= numPlayers){
                //take it
                myOfferTiles.add(offerTile);
            }
        }
        return myOfferTiles;
    }

    /**
     * Returns the turn-order tile template configured for exactly the given player count.
     *
     * @param numPlayers the number of players
     * @return the matching {@link TurnOrderTile} template, or {@code null} if none matches
     */
    public TurnOrderTile getTurnOrderTile(int numPlayers){

        for(TurnOrderTile turnOrderTile : turnOrderTiles){
            if(numPlayers == turnOrderTile.getNumPlayers())
                return turnOrderTile;
        }
        return null; //we should never get to this point
    }

    /** @return a new list containing all registered character card IDs */
    public List<String> getAllCharactersIDs() {
        return new ArrayList<>(characterMap.keySet());
    }

    /** @return a new list containing all registered event card IDs */
    public List<String> getAllEventsIDs() {
        return new ArrayList<>(eventMap.keySet());
    }

    /** @return a new list containing all registered building card IDs */
    public List<String> getAllBuildingsIDs() {
        return new ArrayList<>(buildingMap.keySet());
    }

}