// DONE is a mock

package it.polimi.ingsw.am02.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameRegistry {

    final Player player;
    final Game game;
    private static GameRegistry instance; //the only instance of the registry
    private Map<String, CharacterCard> characterMap;
    private Map<String, EventCard> eventMap;
    private Map<String, BuildingCard> buildingMap;
    private List<OfferTile> offerTiles;
    private List<TurnOrderTile> turnOrderTiles;


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

    //TODO: MATTEO should put the remaining loading methods here

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

    public void visitPhaseObserver(PhaseObserver effect){
        Game.attachPhaseObserver(effect);
    }

    public void visitTribuObserver(TribuObserver effect){
        Tribu.attachTribuObserver(effect);
    }

}