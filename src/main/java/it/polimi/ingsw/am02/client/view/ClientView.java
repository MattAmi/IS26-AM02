package it.polimi.ingsw.am02.client.view;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.CardType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.common.enumerations.RowPosition;

import java.util.List;
import java.util.Map;

/**
 * Observer interface implemented by all client views (TUI, GUI).
 */
public interface ClientView {

    // Lobby lifecycle
    void onUsernameResult(String username, boolean accepted, String reason);
    void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies);
    void onCurrentLobbyUpdated(LobbyInfo lobby);
    void onShowAvailableTotems();
    void onLobbyDissolved();

    // Game lifecycle
    void onGameStarted(String gameId);
    void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board);
    void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder);
    void onCurrentPlayerChanged(String nextPlayer);
    void onTurnOrderEstablished(List<String> turnOrder);

    // Totem & board
    void onTotemPlaced(String nickname, char tileID);
    void onTotemReturned(String nickname, int turnOrderPosition);
    void onOfferTilesUpdated(List<OfferTileInfo> offerTiles);
    void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount);
    void onEraChanged(List<String> newUpperRowBuildings, List<String> newLowerRowBuildings);

    // Player state
    void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower);
    void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower);
    void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue);
    void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow);

    // Events & extra turns
    void onEventResolved(String EventID, String eventName);
    void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower);
    void onExtraTurnEnded(String nickname);

    // Game end & errors
    void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings);
    void onPlayerDisconnected(String nickname);
    void onGameAborted(String lastManStanding);
    void onGameRecoveryFailed();
    void onPlayerReconnected(String nickname);
    void onError(String message);

    // Core View-Model binding
    void setGameModel(GameModel gameModel);
    void onReturnToLobby();

    // Connection
    void onConnectionLost();
    void onConnectionRestored();
    void onAutoPlayerTimerStarted(String nickname);
    void onAutoPlayerInvoked(String nickname);
}