package it.polimi.ingsw.am02.common.messages.events.error;
import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record ErrorEvent(String errorMessage) implements Event {
    public void applyToGameModel(GameModel model) { model.updateError(errorMessage); }
    public void applyToLobbyModel(LobbyModel model) { model.updateError(errorMessage); }
}