package it.polimi.ingsw.am02.client.network;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;

import java.util.List;
import java.util.Map;

/**
 * Unified entry point for all inbound server notifications on the client side.
 *
 * <p>Implements {@link VirtualView} so that it can be used both as the target of
 * direct RMI calls (via {@link it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy})
 * and as the argument of {@code Event#apply(VirtualView)} (Socket path).
 * In both cases every {@code notifyXxx} call ends up here and is forwarded to
 * either {@link LobbyModel} or the {@link it.polimi.ingsw.am02.client.model.GameModel}
 * owned by the {@link ClientController}, depending on the current game phase.
 *
 * <p>The dispatcher also tracks {@link #activeNickname} and {@link #activeGameId}
 * to support lazy {@link it.polimi.ingsw.am02.client.model.GameModel} creation
 * ({@link #ensureGameModel()}) and session restoration after reconnection.
 */
public class ClientNetworkDispatcher implements VirtualView {

    private final ClientController clientController;
    private final LobbyModel lobbyModel;

    /**
     * Optional callback invoked the first time {@link #notifyGameStarted} is called.
     * Used by {@link it.polimi.ingsw.am02.client.network.socket.SocketServerProxy}
     * to keep its {@code activeGameId} field in sync.
     */
    private java.util.function.Consumer<String> onGameStarted = id -> {};

    /** Last validated nickname; used to bootstrap the GameModel on reconnection. */
    private String activeNickname;

    /** Game ID currently active; set on {@link #notifyGameStarted}. */
    private String activeGameId;

    /**
     * Creates a dispatcher wired to the given controller and lobby model.
     *
     * @param clientController the controller that owns the active client model
     * @param lobbyModel       the lobby model that receives pre-game notifications
     */
    public ClientNetworkDispatcher(ClientController clientController, LobbyModel lobbyModel) {
        this.clientController = clientController;
        this.lobbyModel = lobbyModel;
    }

    /**
     * Registers a callback that is invoked once, when {@link #notifyGameStarted}
     * is first received.
     *
     * <p>Intended for the Socket proxy, which needs to cache the game ID on its own.
     *
     * @param callback a consumer that receives the game ID string
     */
    public void setOnGameStarted(java.util.function.Consumer<String> callback) {
        this.onGameStarted = callback;
    }

    /**
     * Updates the cached active nickname.
     * Called by the proxy after a successful reconnection handshake.
     *
     * @param nickname the player's registered nickname
     */
    public void updateActiveNickname(String nickname) {
        this.activeNickname = nickname;
    }

    /**
     * Updates the cached active game ID.
     * Called by the proxy after a successful reconnection handshake.
     *
     * @param gameId the ID of the game the player has rejoined
     */
    public void updateActiveGameId(String gameId) {
        this.activeGameId = gameId;
    }

    /**
     * Lazily creates the {@link it.polimi.ingsw.am02.client.model.GameModel} if it
     * does not yet exist and {@link #activeNickname} is available.
     *
     * <p>This handles the case where a game-phase notification arrives before the
     * UI has explicitly triggered the Lobby→Game context switch (e.g. during
     * reconnection drain).
     */
    private void ensureGameModel() {
        if (clientController.getGameModel() == null && activeNickname != null) {
            clientController.onGameModelRequired(activeNickname);

            if (activeGameId != null && clientController.getGameModel() != null) {
                clientController.getGameModel().setGameId(activeGameId);
            }
        }
    }

    // ------------------------------------------------------------------
    // Lobby notifications
    // ------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void notifyUsernameResult(String username, boolean isValid, String reason) {
        if (isValid) this.activeNickname = username;
        lobbyModel.updateUsernameResult(username, isValid, reason);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Also fires the {@link #onGameStarted} callback and ensures the
     * {@link it.polimi.ingsw.am02.client.model.GameModel} is created before
     * forwarding the event.
     */
    @Override
    public void notifyGameStarted(String gameId) {
        this.activeGameId = gameId;
        onGameStarted.accept(gameId);
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameStarted(gameId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        lobbyModel.updateAvailableLobbies(lobbies);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyCurrentLobbyUpdated(LobbyInfo lobby) {
        lobbyModel.updateCurrentLobby(lobby);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyLobbyDissolved(String lobbyID) {
        lobbyModel.updateLobbyDissolved();
    }

    // ------------------------------------------------------------------
    // Lifecycle notifications
    // ------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer,
                                         List<String> turnOrder,
                                         Map<String, Integer> initialFood,
                                         BoardSnapshot boardSnapshot) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameSetupCompleted(totemByPlayer, turnOrder, initialFood, boardSnapshot);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateCurrentPhase(phase, currentPlayer, resolutionOrder);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyCurrentPlayerChanged(String nextPlayer) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateCurrentPlayer(nextPlayer);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTurnOrderEstablished(List<String> turnOrder) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateTurnOrder(turnOrder);
        }
    }

    // ------------------------------------------------------------------
    // Board notifications
    // ------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void notifyBoardUpdated(List<String> upper, List<String> lower,
                                   List<String> disc, List<String> moved, int deck) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateBoard(upper, lower, deck);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEraChanged(Era era, List<String> upper,
                                 List<String> lower, List<String> disc) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateEra(era, upper, lower);
        }
    }

    // ------------------------------------------------------------------
    // Player action notifications
    // ------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void notifyTotemPlaced(String nickname, char tileID) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateTotemPlaced(nickname, tileID);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyTotemReturned(String nick, int pos) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateTotemReturned(nick, pos);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyCardTaken(String nickname, String cardID,
                                CardType cardType, RowPosition sourceRow) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateCardTaken(nickname, cardID, cardType, sourceRow);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerLimitsInitialized(String nick, int u, int l) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerLimitsInitialized(nick, u, l);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerLimitsUpdated(String nick, int u, int l) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerLimits(nick, u, l);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerResourceChanged(String nick, ResourceType r, int v, int d) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerResource(nick, r, v);
        }
    }

    // ------------------------------------------------------------------
    // Event / turn notifications
    // ------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void notifyEventResolved(String id, String name) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateEventResolved(id, name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyExtraTurnStarted(String nick, int u, int l) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateExtraTurnStarted(nick, u, l);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyExtraTurnEnded(String nick) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateExtraTurnEnded(nick);
        }
    }

    // ------------------------------------------------------------------
    // Game-end notifications
    // ------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void notifyGameEnded(List<String> w, List<PlayerFinalScore> r) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameEnded(w, r);
        }
    }

    // ------------------------------------------------------------------
    // Connection / error notifications
    // ------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>If no {@link it.polimi.ingsw.am02.client.model.GameModel} is active,
     * the error is forwarded to the {@link LobbyModel} instead and
     * {@link #activeGameId} is cleared to prevent stale rejoin attempts.
     */
    @Override
    public void notifyError(String message) {
        if (clientController.getGameModel() == null) {
            this.activeGameId = null;
            lobbyModel.updateError(message);
        } else {
            clientController.getGameModel().updateError(message);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerDisconnected(String nick) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerDisconnected(nick);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPlayerReconnected(String nick) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerReconnected(nick);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyAutoPlayerTimerStarted(String nick, long seconds) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateAutoPlayerTimerStarted(nick, seconds);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyAutoPlayerInvoked(String nick) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateAutoPlayerInvoked(nick);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGameAborted(String winner) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameAborted(winner);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGameRecoveryFailed() {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameRecoveryFailed();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGlobalTimerStarted(long seconds) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGlobalTimerStarted(seconds);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyGlobalTimerCancelled() {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGlobalTimerCancelled();
        }
    }
}