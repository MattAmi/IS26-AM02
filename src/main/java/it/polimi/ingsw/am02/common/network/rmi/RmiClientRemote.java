package it.polimi.ingsw.am02.common.network.rmi;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * RMI remote interface exposed by the client to the server.
 * Each method mirrors a {@link it.polimi.ingsw.am02.common.interfaces.VirtualView} notification,
 * with the addition of {@link RemoteException} required by RMI.
 */
public interface RmiClientRemote extends Remote {

    // ── Lobby ────────────────────────────────────────────────────────────────

    /**
     * Notifies the client whether the requested username was accepted.
     *
     * @param username the requested username
     * @param isValid  {@code true} if the username was accepted
     * @param reason   human-readable rejection reason, or {@code null} if accepted
     * @throws RemoteException if the RMI call fails
     */
    void notifyUsernameResult(String username, boolean isValid, String reason) throws RemoteException;

    /**
     * Notifies the client that its lobby has started a game.
     *
     * @param gameId the identifier of the started game
     * @throws RemoteException if the RMI call fails
     */
    void notifyGameStarted(String gameId) throws RemoteException;

    /**
     * Pushes the current list of open lobbies to the client.
     *
     * @param lobbies snapshot of all available lobbies
     * @throws RemoteException if the RMI call fails
     */
    void notifyAvailableLobbiesUpdated(List<LobbyInfo> lobbies) throws RemoteException;

    /**
     * Pushes an updated snapshot of the lobby the client is currently in.
     *
     * @param lobby the updated lobby state
     * @throws RemoteException if the RMI call fails
     */
    void notifyCurrentLobbyUpdated(LobbyInfo lobby) throws RemoteException;

    /**
     * Notifies the client that the lobby it was in has been dissolved.
     *
     * @param lobbyID the identifier of the dissolved lobby
     * @throws RemoteException if the RMI call fails
     */
    void notifyLobbyDissolved(String lobbyID) throws RemoteException;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Notifies the client that game setup is complete and provides the initial game state.
     *
     * @param totemByPlayer  mapping from player nickname to assigned totem colour
     * @param turnOrder      initial turn order as an ordered list of nicknames
     * @param initialFood    mapping from player nickname to starting food amount
     * @param boardSnapshot  full snapshot of the initial board state
     * @throws RemoteException if the RMI call fails
     */
    void notifyGameSetupCompleted(Map<String, Totem> totemByPlayer, List<String> turnOrder,
                                  Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) throws RemoteException;

    /**
     * Notifies the client that the game FSM has transitioned to a new phase.
     *
     * @param phase           the new {@link PhaseType}
     * @param currentPlayer   nickname of the player who must act, or {@code null} if not applicable
     * @param resolutionOrder ordered list of nicknames for action resolution, or empty if not applicable
     * @throws RemoteException if the RMI call fails
     */
    void notifyPhaseChanged(PhaseType phase, String currentPlayer,
                            List<String> resolutionOrder) throws RemoteException;

    /**
     * Notifies the client that the active player has changed within the current phase.
     *
     * @param nextPlayer nickname of the new active player
     * @throws RemoteException if the RMI call fails
     */
    void notifyCurrentPlayerChanged(String nextPlayer) throws RemoteException;

    /**
     * Notifies the client of the turn order for the upcoming round.
     *
     * @param turnOrder ordered list of nicknames for the next round
     * @throws RemoteException if the RMI call fails
     */
    void notifyTurnOrderEstablished(List<String> turnOrder) throws RemoteException;

    // ── Board ─────────────────────────────────────────────────────────────────

    /**
     * Notifies the client of a board state change after card row updates.
     *
     * @param newUpperRow        card IDs now in the upper row
     * @param newLowerRow        card IDs now in the lower row
     * @param discardedCards     card IDs discarded during this update
     * @param movedToLowerRow    card IDs moved from the upper to the lower row
     * @param deckRemainingCount number of cards remaining in the deck
     * @throws RemoteException if the RMI call fails
     */
    void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow,
                            List<String> discardedCards, List<String> movedToLowerRow,
                            int deckRemainingCount) throws RemoteException;

    /**
     * Notifies the client that a new era has begun and the building rows have changed.
     *
     * @param newEra               the new {@link Era}
     * @param newUpperRowBuildings building card IDs now in the upper row
     * @param newLowerRowBuildings building card IDs now in the lower row
     * @param discardedBuildings   building card IDs discarded at era transition
     * @throws RemoteException if the RMI call fails
     */
    void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings,
                          List<String> newLowerRowBuildings,
                          List<String> discardedBuildings) throws RemoteException;

    // ── Player actions ────────────────────────────────────────────────────────

    /**
     * Notifies the client that a player has placed their totem on an offer tile.
     *
     * @param nickname the player who placed the totem
     * @param tileID   the letter identifying the target offer tile
     * @throws RemoteException if the RMI call fails
     */
    void notifyTotemPlaced(String nickname, char tileID) throws RemoteException;

    /**
     * Notifies the client that a player's totem has returned to the turn order tile.
     *
     * @param nickname          the player whose totem returned
     * @param turnOrderPosition the 0-based position claimed on the turn order tile
     * @throws RemoteException if the RMI call fails
     */
    void notifyTotemReturned(String nickname, int turnOrderPosition) throws RemoteException;

    /**
     * Notifies the client that a player has taken a card from a row.
     *
     * @param nickname  the player who took the card
     * @param cardID    identifier of the taken card
     * @param cardType  type of the taken card
     * @param sourceRow the row ({@link RowPosition}) the card was taken from
     * @throws RemoteException if the RMI call fails
     */
    void notifyCardTaken(String nickname, String cardID, CardType cardType,
                         RowPosition sourceRow) throws RemoteException;

    /**
     * Notifies the client of the initial pick limits for a player at the start of their turn.
     *
     * @param nickname       the player whose limits are initialized
     * @param remainingUpper cards still pickable from the upper row
     * @param remainingLower cards still pickable from the lower row
     * @throws RemoteException if the RMI call fails
     */
    void notifyPlayerLimitsInitialized(String nickname, int remainingUpper,
                                       int remainingLower) throws RemoteException;

    /**
     * Notifies the client that a player's remaining pick limits have changed.
     *
     * @param nickname       the player whose limits changed
     * @param remainingUpper cards still pickable from the upper row
     * @param remainingLower cards still pickable from the lower row
     * @throws RemoteException if the RMI call fails
     */
    void notifyPlayerLimitsUpdated(String nickname, int remainingUpper,
                                   int remainingLower) throws RemoteException;

    /**
     * Notifies the client that a player's resource amount has changed.
     *
     * @param nickname  the player whose resource changed
     * @param resource  the {@link ResourceType} that changed
     * @param newValue  the new absolute value
     * @param delta     the signed change ({@code newValue - oldValue})
     * @throws RemoteException if the RMI call fails
     */
    void notifyPlayerResourceChanged(String nickname, ResourceType resource,
                                     int newValue, int delta) throws RemoteException;

    // ── Events / turns ────────────────────────────────────────────────────────

    /**
     * Notifies the client that a game event card has been resolved.
     *
     * @param eventID   identifier of the resolved event card
     * @param eventName display name of the event
     * @throws RemoteException if the RMI call fails
     */
    void notifyEventResolved(String eventID, String eventName) throws RemoteException;

    /**
     * Notifies the client that a player has started an extra turn.
     *
     * @param nickname       the player taking the extra turn
     * @param remainingUpper cards still pickable from the upper row
     * @param remainingLower cards still pickable from the lower row
     * @throws RemoteException if the RMI call fails
     */
    void notifyExtraTurnStarted(String nickname, int remainingUpper,
                                int remainingLower) throws RemoteException;

    /**
     * Notifies the client that a player's extra turn has ended.
     *
     * @param nickname the player whose extra turn ended
     * @throws RemoteException if the RMI call fails
     */
    void notifyExtraTurnEnded(String nickname) throws RemoteException;

    // ── Game end ──────────────────────────────────────────────────────────────

    /**
     * Notifies the client that the game has ended with final scores.
     *
     * @param winners       nicknames of the winning player(s)
     * @param finalRankings full ranked list of player scores
     * @throws RemoteException if the RMI call fails
     */
    void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) throws RemoteException;

    // ── Connection (transient) ────────────────────────────────────────────────

    /**
     * Sends an error message to the client.
     *
     * @param message human-readable description of the error
     * @throws RemoteException if the RMI call fails
     */
    void notifyError(String message) throws RemoteException;

    /**
     * Notifies the client that another player has disconnected.
     *
     * @param nickname the disconnected player's nickname
     * @throws RemoteException if the RMI call fails
     */
    void notifyPlayerDisconnected(String nickname) throws RemoteException;

    /**
     * Notifies the client that a previously disconnected player has reconnected.
     *
     * @param nickname the reconnected player's nickname
     * @throws RemoteException if the RMI call fails
     */
    void notifyPlayerReconnected(String nickname) throws RemoteException;

    /**
     * Notifies the client that the auto-player timer has started for a disconnected player.
     *
     * @param nickname the disconnected player whose turn the auto-player will handle
     * @param seconds  duration of the timer in seconds
     * @throws RemoteException if the RMI call fails
     */
    void notifyAutoPlayerTimerStarted(String nickname, long seconds) throws RemoteException;

    /**
     * Notifies the client that the auto-player has acted on behalf of a disconnected player.
     *
     * @param nickname the player on whose behalf the auto-player acted
     * @throws RemoteException if the RMI call fails
     */
    void notifyAutoPlayerInvoked(String nickname) throws RemoteException;

    /**
     * Notifies the client that the game has been aborted due to insufficient active players.
     *
     * @param winner nickname of the player declared winner by default, or {@code null} if none
     * @throws RemoteException if the RMI call fails
     */
    void notifyGameAborted(String winner) throws RemoteException;

    /**
     * Notifies the client that the server failed to recover the game state after a crash.
     *
     * @throws RemoteException if the RMI call fails
     */
    void notifyGameRecoveryFailed() throws RemoteException;

    /**
     * Notifies the client that the global suspension timer has started.
     *
     * @param seconds duration of the timer in seconds
     * @throws RemoteException if the RMI call fails
     */
    void notifyGlobalTimerStarted(long seconds) throws RemoteException;

    /**
     * Notifies the client that the global suspension timer has been cancelled.
     *
     * @throws RemoteException if the RMI call fails
     */
    void notifyGlobalTimerCancelled() throws RemoteException;

    // ── Heartbeat ─────────────────────────────────────────────────────────────

    /**
     * Heartbeat call sent periodically by the server to verify the client is still reachable.
     *
     * @throws RemoteException if the RMI call fails
     */
    void ping() throws RemoteException;
}