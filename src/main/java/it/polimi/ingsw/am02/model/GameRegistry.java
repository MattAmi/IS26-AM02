// DONE is a mock

package it.polimi.ingsw.am02.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameRegistry {

    final Player player; // TODO NO!
    final Game game; // TODO NO!
    private static GameRegistry instance; //the only instance of the registry
    private Map<String, CharacterCard> characterMap;
    private Map<String, EventCard> eventMap;
    private Map<String, BuildingCard> buildingMap;
    private List<OfferTile> offerTiles;
    private List<TurnOrderTile> turnOrderTiles;
    private final ObjectMapper mapper = new ObjectMapper();


    // TODO Husnain: C'è problema grosso: il costruttore del registry deve essere PRIVATE (pattern singleton)
    public GameRegistry(Player player, Game game) {
        this.player = player;
        this.game = game;
    }

    public static GameRegistry getInstance() {
        // Primo controllo (senza blocco) per migliorare le performance
        if (instance == null) {
            //Sincronizza il blocco solo la prima volta che si crea l'istanza
            synchronized (GameRegistry.class) {
                //Secondo controllo nel caso un altro thread l'abbia creata nel frattempo
                if (instance == null) {
                    instance = new GameRegistry();
                }
            }
        }
        return instance;
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

    public void loadOfferTiles(String offerTilesPath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(offerTilesPath));

            List<JsonNode> nodes = new ArrayList<>();
            if (rootNode.isArray()) {
                rootNode.forEach(nodes::add);
            }

            OfferTilesFactory offerTilesFactory = new OfferTilesFactory();
            for(JsonNode node : nodes) {
                OfferTile offerTile = offerTilesFactory.createOfferTile(node);
                offerTiles.add(offerTile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadTurnOrderTiles(String turnOrderTilesPath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(turnOrderTilesPath));

            List<JsonNode> nodes = new ArrayList<>();
            if (rootNode.isArray()) {
                rootNode.forEach(nodes::add);
            }

            TurnOrderTilesFactory turnOrderTilesFactory = new TurnOrderTilesFactory();
            for(JsonNode node : nodes) {
                TurnOrderTile turnOrderTile = turnOrderTilesFactory.createTurnOrderTile(node);
                turnOrderTiles.add(turnOrderTile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<JsonNode> parseJsonToList(String path) throws IOException {
        JsonNode rootNode = mapper.readTree(new File(path));
        List<JsonNode> nodes = new ArrayList<>();
        if (rootNode.isArray()) {
            rootNode.forEach(nodes::add);
        }
        return nodes;
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
            if(numPlayers >= offerTile.getMinPlayers()){
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
        return characterMap.keySet()
                .stream()
                .collect(Collectors.toList());
    }

    public List<String> getAllEventIDs() {
        return eventMap.keySet()
                .stream()
                .collect(Collectors.toList());
    }

    public List<String> getAllBuildingIDs() {
        return buildingMap.keySet()
                .stream()
                .collect(Collectors.toList());
    }

}