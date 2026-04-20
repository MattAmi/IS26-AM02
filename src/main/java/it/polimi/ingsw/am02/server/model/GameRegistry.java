// DONE is a mock

package it.polimi.ingsw.am02.server.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRegistry {

    private static final GameRegistry INSTANCE = new GameRegistry(); //the only instance of the registry

    private final Map<String, CharacterCard> characterMap;
    private final Map<String, EventCard> eventMap;
    private final Map<String, BuildingCard> buildingMap;
    private final List<OfferTile> offerTiles;
    private final List<TurnOrderTile> turnOrderTiles;

    private final ObjectMapper mapper;

    private static final String CHARACTERS_PATH = "/it/polimi/ingsw/am02/JSON/Characters.JSON";
    private static final String EVENTS_PATH = "/it/polimi/ingsw/am02/JSON/Events.JSON";
    private static final String BUILDINGS_PATH = "/it/polimi/ingsw/am02/JSON/Buildings.JSON";
    private static final String OFFER_TILES_PATH = "/it/polimi/ingsw/am02/JSON/OfferTiles.JSON";
    private static final String TURN_ORDER_TILES_PATH = "/it/polimi/ingsw/am02/JSON/TurnOrderTiles.JSON";


    private GameRegistry() {
        this.mapper = new ObjectMapper();
        this.characterMap = new HashMap<>();
        this.eventMap = new HashMap<>();
        this.buildingMap = new HashMap<>();
        this.offerTiles = new ArrayList<>();
        this.turnOrderTiles = new ArrayList<>();

        loadCharacters(CHARACTERS_PATH);
        loadEvents(EVENTS_PATH);
        loadBuildings(BUILDINGS_PATH);
        loadOfferTiles(OFFER_TILES_PATH);
        loadTurnOrderTiles(TURN_ORDER_TILES_PATH);
    }

    public static GameRegistry getInstance() {
        return INSTANCE;
    }

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

    public boolean isCharacter(String cardID) {
        return characterMap.containsKey(cardID);
    }

    public boolean isEvent(String cardID) {
        return eventMap.containsKey(cardID);
    }

    public boolean isBuilding(String cardID) {
        return buildingMap.containsKey(cardID);
    }

    public CharacterCard getCharacter(String cardID) {
        return characterMap.get(cardID);
    }

    public EventCard getEvent(String cardID) {
        return eventMap.get(cardID);
    }

    public BuildingCard getBuilding(String cardID) {
        return buildingMap.get(cardID);
    }

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

    public TurnOrderTile getTurnOrderTile(int numPlayers){

        for(TurnOrderTile turnOrderTile : turnOrderTiles){
            if(numPlayers == turnOrderTile.getNumPlayers())
                return turnOrderTile;
        }
        return null; //we should never get to this point
    }

    public List<String> getAllCharactersIDs() {
        return new ArrayList<>(characterMap.keySet());
    }

    public List<String> getAllEventsIDs() {
        return new ArrayList<>(eventMap.keySet());
    }

    public List<String> getAllBuildingsIDs() {
        return new ArrayList<>(buildingMap.keySet());
    }

}