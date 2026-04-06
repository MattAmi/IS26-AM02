// DONE is a mock

package it.polimi.ingsw.am02.model;

import java.util.ArrayList;
import java.util.List;

public class GameRegistry {
    final Player player;
    final Game game;

    public GameRegistry(Player player, Game game) {
        this.player = player;
        this.game = game;
    }

    public static GameRegistry getInstance() {
    }

    public void visitPhaseObserver(PhaseObserver effect){
        Game.attachPhaseObserver(effect);
    }

    public void visitTribuObserver(TribuObserver effect){
        Tribu.attachTribuObserver(effect);
    }

    public List <String> getAllBuildingsIDs() {
    }

    public BuildingCard getBuilding(String id){

    }

    public boolean isCharacter(String cardID) {
    }

    public boolean isEvent(String cardID) {
    }

    public boolean isBuilding(String cardID) {
    }

    public EventCard getEvent(String cardID) {
    }

    public CharacterCard getCharacter(String cardID) {
    }
}