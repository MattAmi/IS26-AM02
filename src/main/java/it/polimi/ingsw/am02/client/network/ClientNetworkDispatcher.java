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
 * Unified handler for incoming server notifications.
 * Translates VirtualView calls into updates on local models.
 */
public class ClientNetworkDispatcher implements VirtualView {

    private final ClientController clientController;
    private final LobbyModel lobbyModel;

    // Cache to handle automatic transition and reconnection
    private String activeNickname;
    private String activeGameId;

    public ClientNetworkDispatcher(ClientController clientController, LobbyModel lobbyModel) {
        this.clientController = clientController;
        this.lobbyModel = lobbyModel;
    }

    public void updateActiveNickname(String nickname) {
        this.activeNickname = nickname;
    }

    public void updateActiveGameId(String gameId) {
        this.activeGameId = gameId;
    }

    public void updateInternalState(String nick, String gId) {
        this.activeNickname = nick;
        this.activeGameId = gId;
    }

    private void ensureGameModel() {
        if (clientController.getGameModel() == null && activeNickname != null) {
            // This call performs the observer switch: Lobby -> Game
            clientController.onGameModelRequired(activeNickname);

            if (activeGameId != null && clientController.getGameModel() != null) {
                clientController.getGameModel().setGameId(activeGameId);
            }
        }
    }
    // --- LOBBY NOTIFICATIONS ---

    @Override
    public void notifyUsernameResult(String username, boolean isValid, String reason) {
        if (isValid) this.activeNickname = username;
        lobbyModel.updateUsernameResult(username, isValid, reason);
    }

    @Override
    public void notifyGameStarted(String gameId) {
        this.activeGameId = gameId;
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameStarted(gameId);
        }
    }

    @Override
    public void notifyAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        lobbyModel.updateAvailableLobbies(lobbies);
    }

    @Override
    public void notifyCurrentLobbyUpdated(LobbyInfo lobby) {
        lobbyModel.updateCurrentLobby(lobby);
    }

    @Override
    public void notifyLobbyDissolved(String lobbyID) {
        lobbyModel.updateLobbyDissolved();
    }

    // --- LIFECYCLE NOTIFICATIONS ---

    @Override
    public void notifyGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameSetupCompleted(turnOrder, initialFood, boardSnapshot);
        }
    }

    @Override
    public void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateCurrentPhase(phase, currentPlayer, resolutionOrder);
        }
    }

    @Override
    public void notifyCurrentPlayerChanged(String nextPlayer) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateCurrentPlayer(nextPlayer);
        }
    }

    @Override
    public void notifyTurnOrderEstablished(List<String> turnOrder) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateTurnOrder(turnOrder);
        }
    }

    // --- BOARD NOTIFICATIONS ---

    @Override
    public void notifyBoardUpdated(List<String> upper, List<String> lower, List<String> disc, List<String> moved, int deck) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateBoard(upper, lower, deck);
        }
    }

    @Override
    public void notifyEraChanged(Era era, List<String> upper, List<String> lower, List<String> disc) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateEra(upper, lower);
        }
    }

    // --- PLAYER ACTIONS NOTIFICATIONS ---

    @Override
    public void notifyTotemPlaced(String nickname, char tileID) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateTotemPlaced(nickname, tileID);
        }
    }

    @Override
    public void notifyTotemReturned(String nick, int pos) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateTotemReturned(nick, pos);
        }
    }

    @Override
    public void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateCardTaken(nickname, cardID, cardType, sourceRow);
        }
    }

    @Override
    public void notifyPlayerLimitsInitialized(String nick, int u, int l) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerLimitsInitialized(nick, u, l);
        }
    }

    @Override
    public void notifyPlayerLimitsUpdated(String nick, int u, int l) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerLimits(nick, u, l);
        }
    }

    @Override
    public void notifyPlayerResourceChanged(String nick, ResourceType r, int v, int d) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerResource(nick, r, v);
        }
    }

    // --- EVENTS / TURNS NOTIFICATIONS ---

    @Override
    public void notifyEventResolved(String id, String name) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateEventResolved(id, name);
        }
    }

    @Override
    public void notifyExtraTurnStarted(String nick, int u, int l) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateExtraTurnStarted(nick, u, l);
        }
    }

    @Override
    public void notifyExtraTurnEnded(String nick) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateExtraTurnEnded(nick);
        }
    }

    // --- GAME END NOTIFICATIONS ---

    @Override
    public void notifyGameEnded(List<String> w, List<PlayerFinalScore> r) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameEnded(w, r);
        }
    }

    // --- CONNECTION NOTIFICATIONS (Transient) ---

    @Override
    public void notifyError(String message) {
        // If we're not in a game, forward the error to the lobby
        if (clientController.getGameModel() == null) {
            this.activeGameId = null;
            lobbyModel.updateError(message);
        } else {
            clientController.getGameModel().updateError(message);
        }
    }

    @Override
    public void notifyPlayerDisconnected(String nick) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerDisconnected(nick);
        }
    }

    @Override
    public void notifyPlayerReconnected(String nick) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updatePlayerReconnected(nick);
        }
    }

    @Override
    public void notifyAutoPlayerTimerStarted(String nick) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateAutoPlayerTimerStarted(nick);
        }
    }

    @Override
    public void notifyAutoPlayerInvoked(String nick) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateAutoPlayerInvoked(nick);
        }
    }

    @Override
    public void notifyGameAborted(String winner) {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameAborted(winner);
        }
    }

    @Override
    public void notifyGameRecoveryFailed() {
        ensureGameModel();
        if (clientController.getGameModel() != null) {
            clientController.getGameModel().updateGameRecoveryFailed();
        }
    }
}