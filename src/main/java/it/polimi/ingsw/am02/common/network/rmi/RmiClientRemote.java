package it.polimi.ingsw.am02.common.network.rmi;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
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

    // Lifecycle
    void notifyGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot boardSnapshot) throws RemoteException;
    void notifyPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) throws RemoteException;
    void notifyCurrentPlayerChanged(String nextPlayer) throws RemoteException;
    void notifyTurnOrderEstablished(List<String> turnOrder) throws RemoteException;

    // Board
    void notifyBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, List<String> discardedCards, List<String> movedToLowerRow, int deckRemainingCount) throws RemoteException;
    void notifyEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings, List<String> discardedBuildings) throws RemoteException;

    // Player actions
    void notifyTotemPlaced(String nickname, char tileID) throws RemoteException;
    void notifyTotemReturned(String nickname, int turnOrderPosition) throws RemoteException;
    void notifyCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) throws RemoteException;
    void notifyPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) throws RemoteException;
    void notifyPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) throws RemoteException;
    void notifyPlayerResourceChanged(String nickname, ResourceType resource, int newValue, int delta) throws RemoteException;

    // Events / turns
    void notifyEventResolved(String eventID, String eventName) throws RemoteException;
    void notifyExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) throws RemoteException;
    void notifyExtraTurnEnded(String nickname) throws RemoteException;

    //  Game end
    void notifyGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) throws RemoteException;

    // Connection (transient)
    void notifyError(String message) throws RemoteException;
    void notifyPlayerDisconnected(String nickname) throws RemoteException;
    void notifyPlayerReconnected(String nickname) throws RemoteException;
    void notifyAutoPlayerTimerStarted(String nickname) throws RemoteException;
    void notifyAutoPlayerInvoked(String nickname) throws RemoteException;
    void notifyGameAborted(String winner) throws RemoteException;
    void notifyGameRecoveryFailed() throws RemoteException;

    // Heartbeat
    void ping() throws RemoteException;
}