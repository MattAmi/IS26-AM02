package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.view.AbstractClientView;

public class GuiView extends AbstractClientView {

    private final LobbyModel lobbyModel;
    private GameModel gameModel; // null until GameStartedEvent

    public GuiView(LobbyModel lobbyModel) {
        this.lobbyModel = lobbyModel;
        lobbyModel.addObserver(this);
    }

    public void onGameModelCreated(GameModel gameModel) {
        this.gameModel = gameModel;
        gameModel.addObserver(this);
    }
}
