package it.polimi.ingsw.am02.client.view.gui;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.view.AbstractClientView;
import it.polimi.ingsw.am02.client.view.gui.scenes.GameScene;
import it.polimi.ingsw.am02.client.view.gui.scenes.LobbyListScene;
import it.polimi.ingsw.am02.client.view.gui.scenes.LobbyScene;
import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.dto.OfferTileInfo;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.common.enumerations.*;
import javafx.scene.control.Alert;

import java.util.List;
import java.util.Map;

public class GuiView extends AbstractClientView {

    private SceneRouter sceneRouter;
    private final LobbyModel lobbyModel;
    private GameModel gameModel;

    public GuiView(LobbyModel lobbyModel) {
        this.lobbyModel = lobbyModel;
        this.lobbyModel.addObserver(this);
    }

    public void setSceneRouter(SceneRouter sceneRouter) { this.sceneRouter = sceneRouter; }

    @Override
    public void setGameModel(GameModel gameModel) {
        this.gameModel = gameModel;
        if (this.gameModel != null) {
            this.gameModel.addObserver(this);
            if (sceneRouter.getGameScene() != null) sceneRouter.getGameScene().refreshAll(gameModel);
        }
    }

    private void refreshGameIfActive() {
        if (sceneRouter.getGameScene() != null && gameModel != null) {
            sceneRouter.getGameScene().refreshAll(gameModel);
        }
    }

    @Override
    public void onUsernameResult(String username, boolean accepted, String reason) {
        if (!accepted) {
            sceneRouter.showToast("Nickname Rejected", reason, Alert.AlertType.WARNING);
            LobbyScene ls = sceneRouter.getLobbyScene();
            if (ls != null) ls.onNicknameRejected();
        } else {
            LobbyScene ls = sceneRouter.getLobbyScene();
            if (ls != null) ls.onNicknameAccepted(username);
            else sceneRouter.showIntroScene();
        }
    }

    @Override
    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        LobbyListScene lls = sceneRouter.getLobbyListScene();
        if (lls != null) lls.onAvailableLobbiesUpdated(lobbies);
        else if (sceneRouter.getLobbyScene() != null || sceneRouter.getGameScene() != null) sceneRouter.showGameMenuScene();
    }

    @Override
    public void onCurrentLobbyUpdated(LobbyInfo lobby) {
        LobbyScene ls = sceneRouter.getLobbyScene();
        if (ls == null) sceneRouter.showLobbyScene(lobby);
        else ls.updateLobbyState(lobby);
    }

    @Override
    public void onLobbyDissolved() {
        sceneRouter.showBlockingAlert("Lobby Closed", "The lobby has been dissolved.", Alert.AlertType.INFORMATION);
        sceneRouter.showGameMenuScene();
    }

    @Override
    public void onGameStarted(String gameId) {
        lobbyModel.removeObserver(this);
        sceneRouter.switchToGameScene(gameModel);
    }

    @Override
    public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board) {
        if (sceneRouter.getGameScene() != null) {
            sceneRouter.getGameScene().prepareForReplay();
        }

        if (sceneRouter.getGameScene() == null) {
            sceneRouter.hideModal();
            sceneRouter.switchToGameScene(gameModel);
        } else refreshGameIfActive();
    }

    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        if (sceneRouter.getGameScene() == null) {
            sceneRouter.hideModal();
            sceneRouter.switchToGameScene(gameModel);
        } else refreshGameIfActive();
    }

    @Override
    public void onCurrentPlayerChanged(String nextPlayer) { refreshGameIfActive(); }
    @Override
    public void onTurnOrderEstablished(List<String> turnOrder) { refreshGameIfActive(); }
    @Override
    public void onTotemPlaced(String nickname, char tileID) { refreshGameIfActive(); }
    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) { refreshGameIfActive(); }
    @Override
    public void onOfferTilesUpdated(List<OfferTileInfo> offerTiles) { refreshGameIfActive(); }
    @Override
    public void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount) { refreshGameIfActive(); }
    @Override
    public void onEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings) {
        GameScene gs = sceneRouter.getGameScene();
        if (gs != null) {
            // Questo comando fa fare +1 alla variabile currentEra e poi fa il refresh!
            gs.showNewEraAnimation(newEra, newUpperRowBuildings, newLowerRowBuildings);
        } else {
            refreshGameIfActive();
        }
    }
    @Override
    public void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) { refreshGameIfActive(); }
    @Override
    public void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) { refreshGameIfActive(); }
    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue) { refreshGameIfActive(); }

    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        GameScene gs = sceneRouter.getGameScene();
        if (gs != null) gs.animateCardTaken(nickname, cardID, cardType, sourceRow);
    }

    @Override
    public void onEventResolved(String eventID, String eventName) { refreshGameIfActive(); }
    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) { refreshGameIfActive(); }
    @Override
    public void onExtraTurnEnded(String nickname) { refreshGameIfActive(); }

    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        sceneRouter.showBlockingAlert("Game Over", "Winners: " + winners, Alert.AlertType.INFORMATION);
    }

    @Override
    public void onPlayerDisconnected(String nickname) {
        GameScene gs = sceneRouter.getGameScene();
        if (gs != null) gs.setPlayerOffline(nickname);
        sceneRouter.showPlayerDisconnectedPopup(nickname);
    }

    @Override
    public void onGameAborted(String lastManStanding) {
        sceneRouter.showBlockingAlert("Aborted", "Game ended. Last standing: " + lastManStanding, Alert.AlertType.INFORMATION);
        sceneRouter.showGameMenuScene();
    }

    @Override
    public void onGameRecoveryFailed() {
        sceneRouter.showBlockingAlert("Error", "Recovery failed", Alert.AlertType.ERROR);
        sceneRouter.showGameMenuScene();
    }

    @Override
    public void onPlayerReconnected(String nickname) {
        GameScene gs = sceneRouter.getGameScene();
        if (gs != null) gs.setPlayerOnline(nickname);
    }

    @Override
    public void onError(String message) {
        sceneRouter.showToast("Errore", message, Alert.AlertType.WARNING);
        LobbyScene ls = sceneRouter.getLobbyScene();
        if (ls != null) ls.onNicknameRejected();
    }

    @Override
    public void onShowAvailableTotems() { }

    @Override
    public void onConnectionLost() { sceneRouter.showConnectionLost(); }

    @Override
    public void onConnectionRestored() {
        sceneRouter.hideModal();
        sceneRouter.showToast("Connected", "You're back online!", Alert.AlertType.INFORMATION);
    }

    @Override
    public void onReturnToLobby() { sceneRouter.showGameMenuScene(); }
}