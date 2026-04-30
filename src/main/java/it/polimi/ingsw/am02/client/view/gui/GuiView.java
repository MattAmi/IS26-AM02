package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.view.AbstractClientView;
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

    @Override
    public void onUsernameResult(String username, boolean accepted, String reason) {
        super.onUsernameResult(username, accepted, reason);
    }

    @Override
    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        super.onAvailableLobbiesUpdated(lobbies);
    }

    @Override
    public void onCurrentLobbyUpdated(LobbyInfo lobby) {
        super.onCurrentLobbyUpdated(lobby);
    }

    @Override
    public void onLobbyDissolved() {
        super.onLobbyDissolved();
    }

    @Override
    public void onGameStarted(String gameId) {
        super.onGameStarted(gameId);
    }

    @Override
    public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board) {
        super.onGameSetupCompleted(turnOrder, initialFood, board);
    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        super.onPhaseChanged(phase, currentPlayer, resolutionOrder);
    }

    @Override
    public void onCurrentPlayerChanged(String nextPlayer) {
        super.onCurrentPlayerChanged(nextPlayer);
    }

    @Override
    public void onTurnOrderEstablished(List<String> turnOrder) {
        super.onTurnOrderEstablished(turnOrder);
    }

    @Override
    public void onTotemPlaced(String nickname, char tileID) {
        super.onTotemPlaced(nickname, tileID);
    }

    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {
        super.onTotemReturned(nickname, turnOrderPosition);
    }

    @Override
    public void onOfferTilesUpdated(List<OfferTileInfo> offerTiles) {
        super.onOfferTilesUpdated(offerTiles);
    }

    @Override
    public void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount) {
        super.onBoardUpdated(newUpperRow, newLowerRow, deckRemainingCount);
    }

    @Override
    public void onEraChanged(List<String> newUpperRowBuildings, List<String> newLowerRowBuildings) {
        super.onEraChanged(newUpperRowBuildings, newLowerRowBuildings);
    }

    @Override
    public void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) {
        super.onPlayerLimitsInitialized(nickname, remainingUpper, remainingLower);
    }

    @Override
    public void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) {
        super.onPlayerLimitsUpdated(nickname, remainingUpper, remainingLower);
    }

    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue) {
        super.onPlayerResourceChanged(nickname, resource, newValue);
    }

    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        super.onCardTaken(nickname, cardID, cardType, sourceRow);
    }

    @Override
    public void onEventResolved(String eventID, String eventName) {
        super.onEventResolved(eventID, eventName);
    }

    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        super.onExtraTurnStarted(nickname, remainingUpper, remainingLower);
    }

    @Override
    public void onExtraTurnEnded(String nickname) {
        super.onExtraTurnEnded(nickname);
    }

    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        super.onGameEnded(winners, finalRankings);
    }

    @Override
    public void onPlayerDisconnected(String nickname) {
        super.onPlayerDisconnected(nickname);
    }

    @Override
    public void onGameAborted(String lastManStanding) {
        super.onGameAborted(lastManStanding);
    }

    @Override
    public void onGameRecoveryFailed() {
        super.onGameRecoveryFailed();
    }

    @Override
    public void onPlayerReconnected(String nickname) {
        super.onPlayerReconnected(nickname);
    }

    @Override
    public void onError(String message) {
        super.onError(message);
    }

}
