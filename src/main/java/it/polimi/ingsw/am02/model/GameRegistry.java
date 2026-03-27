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

    public void visitPhaseObserver(PhaseObserver effect){
        Game.attachPhaseObserver(effect);
    }

    public void visitTribuObserver(TribuObserver effect){
        Tribu.attachTribuObserver(effect);
    }
}