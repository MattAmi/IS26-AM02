package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.commands.Command;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.server.model.exceptions.GameRuleException;
import it.polimi.ingsw.am02.server.model.listeners.GameObserver;
import it.polimi.ingsw.am02.common.messages.*;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;
import it.polimi.ingsw.am02.common.messages.events.error.*;

import java.util.List;
import java.util.Map;

public class GameController implements GameObserver {

    private final ModelInterface model;
    private final Map<String, VirtualView> handlers;


    public GameController(ModelInterface model, Map<String, VirtualView> handlers) {
        this.model = model;
        this.handlers = handlers;
        this.model.addGameObserver(this);
    }


    //Routing methods
    private void unicast(String nickname, Event event) {
        VirtualView h = handlers.get(nickname);
        if (h != null)
            h.notify(event);
    }

    private void broadcast(Event event) {
        handlers.values().forEach(h -> h.notify(event));
    }

    private void broadcastOthers(String excludeNickname, Event event) {
        handlers.entrySet().stream()
                .filter(e -> !e.getKey().equals(excludeNickname))
                .forEach(e -> e.getValue().notify(event));
    }


    //Command handler
    // TODO: Matteo ho cercato di togliere in modo meno invasivo possibile il errore a compile time switch does not cover
    // TODO: all possible input values comunque sarebbe da rivedere visto che ne sai più di me su questa parte é dovuto a presenza sealed classes
    public void handle(Command cmd, String senderNickname) {
        switch (cmd) {
            // --- GESTITI ---
            case MoveTotemCommand c -> model.moveTotem(senderNickname, c.tileID());
            case ResolveActionsCommand c -> model.resolveActions(senderNickname, c.selectedIDs());

            // --- TODO PER IL COMPAGNO (Da implementare) ---
            case SetUsernameCommand c -> {
                System.out.println("TODO: Gestire SetUsernameCommand");
            }
            case CreateLobbyCommand c -> {
                System.out.println("TODO: Gestire CreateLobbyCommand");
            }
            case JoinLobbyCommand c -> {
                System.out.println("TODO: Gestire JoinLobbyCommand");
            }
            case SelectTotemCommand c -> {
                System.out.println("TODO: Gestire SelectTotemCommand");
            }
            case StartGameCommand c -> {
                System.out.println("TODO: Gestire StartGameCommand");
            }
            case LeaveLobbyCommand c -> {
                System.out.println("TODO: Gestire LeaveLobbyCommand");
            }
        }
    }

    public void handlePlayerDisconnected(String nickname) {
        broadcastOthers(nickname, new PlayerDisconnectedEvent(nickname));
    }


    //GameObserver implementation (event translation and dispatching)
    @Override
    public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {
        broadcast(new GameSetupCompletedEvent(turnOrder, initialFood, boardSnapshot));
    }

    @Override
    public void onPhaseChanged(PhaseType phase) {
        broadcast(new PhaseChangedEvent(phase, null, null));
    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer) {
        broadcast(new PhaseChangedEvent(phase, currentPlayer, null));
    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        broadcast(new PhaseChangedEvent(phase, currentPlayer, resolutionOrder));
    }

    @Override
    public void onCurrentPlayerChanged(String nextPlayer) {
        broadcast(new CurrentPlayerChangedEvent(nextPlayer));
    }

    @Override
    public void onTurnOrderEstablished(List<String> turnOrder) {
        broadcast(new TurnOrderEstablishedEvent(turnOrder));
    }

    @Override
    public void onEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) {
        broadcast(new EraChangedEvent(newEra, newUpperRowBuildings, newLowerRowBuildings, discardedBuildings));
    }

    @Override
    public void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) {
        broadcast(new BoardUpdatedEvent(newUpperRow, newLowerRow, discardedCards, movedToLowerRow, deckRemainingCount));
    }

    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        broadcast(new CardTakenEvent(nickname, cardID, cardType, sourceRow));
    }

    @Override
    public void onTotemPlaced(String nickname, char tileID) {
        broadcast(new TotemPlacedEvent(nickname, tileID));
    }

    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {
        broadcast(new TotemReturnedEvent(nickname, turnOrderPosition));
    }

    @Override
    public void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {
        broadcast(new PlayerLimitsInitializedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
        broadcast(new PlayerLimitsUpdatedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) {
        broadcast(new PlayerResourceChangedEvent(nickname, resource, newValue, delta));
    }

    @Override
    public void onEventResolved(String eventID, String eventName) {
        broadcast(new EventResolvedEvent(eventID, eventName));
    }

    @Override
    public void onSustainmentResolved(String nickname, int totalCharacters, int foodPaid, int foodShortage, int ppLost) {
        //TODO: Matteo chiedo conferma che deve essere tolta dalla interfaccia
    }

    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        broadcast(new ExtraTurnStartedEvent(nickname, remainingUpper, remainingLower));
    }

    @Override
    public void onExtraTurnEnded(String nickname) {
        broadcast(new ExtraTurnEndedEvent(nickname));
    }

    @Override
    public void onFinalScoreCalculated(String nickname, int ppFromBuilders, int ppFromBuildings, int ppFromInventors, int ppFromArtists, int ppFromBuildingEffects, int totalPrestigePoints, int remainingFood) {
        //TODO: Matteo mi ricordo che non servava ma ha già 1 usage quindi aspetto conferma
    }

    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        broadcast(new GameEndedEvent(winners, finalRankings));
    }
}
