package it.polimi.ingsw.am02.common.interfaces;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.util.List;
import java.util.Map;


/**
 * Server-side abstraction of a connected client.
 * Implemented by {@link it.polimi.ingsw.am02.server.network.rmi.RmiClientHandler}
 * and {@link it.polimi.ingsw.am02.server.network.socket.SocketClientHandler}.
 *
 * <p>{@link it.polimi.ingsw.am02.server.controller.GameController} holds one
 * {@code VirtualView} per player and calls these methods to push notifications;
 * each implementation serializes the call for its own transport.
 *
 * <p>The static constant {@link #NO_OP} (and factory method {@link #noOp()})
 * provide a silent no-operation implementation used during server-side game
 * replay, so that the model can fire notifications without any real clients
 * being connected.
 */
public interface VirtualView {

    /** Silent no-operation implementation used during server-side replay. */
    final class NoOp implements VirtualView {
        private NoOp() {}
        @Override public void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer, List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {}
        @Override public void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {}
        @Override public void notifyCurrentPlayerChanged(String nextPlayer) {}
        @Override public void notifyTurnOrderEstablished(List<String> turnOrder) {}
        @Override public void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) {}
        @Override public void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) {}
        @Override public void notifyTotemPlaced(String nickname, char tileID) {}
        @Override public void notifyTotemReturned(String nickname, int turnOrderPosition) {}
        @Override public void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {}
        @Override public void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {}
        @Override public void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {}
        @Override public void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) {}
        @Override public void notifyEventResolved(String eventID, String eventName) {}
        @Override public void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {}
        @Override public void notifyExtraTurnEnded(String nickname) {}
        @Override public void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {}
        @Override public void notifyError(String message) {}
        @Override public void notifyPlayerDisconnected(String nickname) {}
        @Override public void notifyPlayerReconnected(String nickname) {}
        @Override public void notifyAutoPlayerTimerStarted(String nickname) {}
        @Override public void notifyAutoPlayerInvoked(String nickname) {}
        @Override public void notifyGameAborted(String winner) {}
        @Override public void notifyGameRecoveryFailed() {}
        @Override public void notifyUsernameResult(String username, boolean isValid, String reason) {}
        @Override public void notifyGameStarted(String gameId) {}
        @Override public void notifyAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {}
        @Override public void notifyCurrentLobbyUpdated(LobbyInfo lobby) {}
        @Override public void notifyLobbyDissolved(String lobbyID) {}
    }


    /** Shared singleton no-op instance. */
    VirtualView NO_OP = new NoOp();

    /** @return the shared no-op instance */
    static VirtualView noOp() { return NO_OP; }

    // Lobby
    /**
     * Notifies the client of the result of a username request.
     *
     * @param username the requested username
     * @param isValid  {@code true} if accepted, {@code false} if rejected
     * @param reason   rejection reason, or {@code null} if accepted
     */
    void notifyUsernameResult(String username, boolean isValid, String reason);

    /**
     * Notifies the client that their lobby's game has started.
     *
     * @param gameId the identifier of the started game
     */
    void notifyGameStarted(String gameId);

    /**
     * Pushes an updated list of all available lobbies to a pre-lobby client.
     *
     * @param lobbies the current list of open lobbies
     */
    void notifyAvailableLobbiesUpdated(List<LobbyInfo> lobbies);

    /**
     * Pushes the updated state of the lobby this client is currently in.
     *
     * @param lobby the updated lobby info
     */
    void notifyCurrentLobbyUpdated(LobbyInfo lobby);

    /**
     * Notifies the client that their current lobby has been dissolved.
     *
     * @param lobbyID the identifier of the dissolved lobby
     */
    void notifyLobbyDissolved(String lobbyID);

    // Lifecycle

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onGameSetupCompleted */
    void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer, List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot);

    /**
     * Notifies the client of a phase transition.
     *
     * @param phase           the new game phase
     * @param currentPlayer   the active player's nickname, or {@code null}
     * @param resolutionOrder the full ordered nickname list, or {@code null}
     */
    void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onCurrentPlayerChanged */
    void notifyCurrentPlayerChanged(String nextPlayer);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onTurnOrderEstablished */
    void notifyTurnOrderEstablished(List<String> turnOrder);

    // Board

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onBoardUpdated */
    void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onEraChanged */
    void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings);

    // Player actions

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onTotemPlaced */
    void notifyTotemPlaced(String nickname, char tileID);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onTotemReturned */
    void notifyTotemReturned(String nickname, int turnOrderPosition);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onCardTaken */
    void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onPlayerLimitsInitialized */
    void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onPlayerLimitsUpdated */
    void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onPlayerResourceChanged */
    void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta);

    // Events / turns

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onEventResolved */
    void notifyEventResolved(String eventID, String eventName);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onExtraTurnStarted */
    void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower);

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onExtraTurnEnded */
    void notifyExtraTurnEnded(String nickname);

    // Game end

    /** @see it.polimi.ingsw.am02.server.model.listeners.GameObserver#onGameEnded */
    void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings);

    // Connection (transient)

    /**
     * Sends an error message to this client only (not stored in global history).
     *
     * @param message the human-readable error description
     */void notifyError(String message);

    /**
     * Notifies all connected players that a specific player has disconnected.
     *
     * @param nickname the disconnected player's nickname
     */void notifyPlayerDisconnected(String nickname);

    /**
     * Notifies all connected players that a specific player has reconnected.
     *
     * @param nickname the reconnected player's nickname
     */void notifyPlayerReconnected(String nickname);

    /**
     * Notifies connected players that the AutoPlayer grace timer has started
     * for a disconnected player.
     *
     * @param nickname the disconnected player whose timer is running
     */void notifyAutoPlayerTimerStarted(String nickname);

    /**
     * Notifies connected players that the AutoPlayer is about to act
     * on behalf of a disconnected player.
     *
     * @param nickname the disconnected player being substituted
     */void notifyAutoPlayerInvoked(String nickname);

    /**
     * Notifies all players that the game was aborted due to forfeit.
     *
     * @param winner the nickname of the surviving player who wins by forfeit
     */void notifyGameAborted(String winner);

    /**
     * Notifies all players that the server could not complete game recovery
     * (e.g. no players reconnected within the recovery window).
     */
    void notifyGameRecoveryFailed();
}
