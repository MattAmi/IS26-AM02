package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.server.model.listeners.GameObserver;
import it.polimi.ingsw.am02.server.network.ClientHandler;

import java.util.Map;

public class GameController implements GameObserver {

    private ModelInterface model;
    private final Map<String, ClientHandler> handlers;


}
