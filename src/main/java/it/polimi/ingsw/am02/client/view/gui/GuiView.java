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
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Graphical User Interface (GUI) implementation of the client view.
 * This class handles the rendering and update of the GUI scenes based on
 * notifications from the lobby and game models.
 */
public class GuiView extends AbstractClientView {

    private SceneRouter sceneRouter;
    private final LobbyModel lobbyModel;
    private GameModel gameModel;

    /**
     * Forfeit countdown received while the game scene did not yet exist. Held here
     * so a later render can surface the banner that would otherwise have been
     * dropped. Only touched on the FX thread.
     */
    private Long pendingForfeitSeconds;

    /**
     * Constructs a new GuiView.
     *
     * @param lobbyModel The lobby model to observe for updates.
     */
    public GuiView(LobbyModel lobbyModel) {
        this.lobbyModel = lobbyModel;
        this.lobbyModel.addObserver(this);
    }

    /**
     * Sets the scene router for navigating between different GUI scenes.
     *
     * @param sceneRouter The SceneRouter instance.
     */
    public void setSceneRouter(SceneRouter sceneRouter) { this.sceneRouter = sceneRouter; }

    /**
     * Injects the game model into the view and starts observing it.
     *
     * @param gameModel The GameModel instance.
     */
    @Override
    public void setGameModel(GameModel gameModel) {
        this.gameModel = gameModel;
        if (this.gameModel != null) {
            this.gameModel.addObserver(this);
            refreshGameIfActive();
        }
    }

    /**
     * Runs an action against the active {@link GameScene} on the JavaFX thread,
     * skipping it if no scene is active yet.
     *
     * <p>Every game callback arrives on the network thread, but the scene is
     * created on the FX thread (inside {@code SceneRouter.switchToGameScene}'s
     * {@code Platform.runLater}). Reading {@code getGameScene()} directly from the
     * network thread therefore races that creation: around reconnection it can
     * see {@code null} and silently drop the update. Deferring the read to the FX
     * thread removes the race entirely — by FX FIFO ordering the scene-creating
     * event's task has already run, and the field is read on the same thread it is
     * written. The scene's own methods already marshal to the FX thread, so this
     * is harmless (and slightly more efficient) for the common in-game case too.
     *
     * @param action the work to perform with the live scene
     */
    private void withGameScene(Consumer<GameScene> action) {
        Platform.runLater(() -> {
            GameScene gs = sceneRouter.getGameScene();
            if (gs != null) action.accept(gs);
        });
    }

    /**
     * Pushes the latest model state to the given scene. Must be called on the FX
     * thread (see {@link #withGameScene(Consumer)}), which keeps all access to
     * {@code pendingForfeitSeconds} single-threaded and the check-then-clear
     * atomic.
     *
     * @param gs the active game scene
     */
    private void renderScene(GameScene gs) {
        if (gameModel == null) return;
        if (pendingForfeitSeconds != null) {
            long s = pendingForfeitSeconds;
            pendingForfeitSeconds = null;
            gs.showForfeitBanner(s);
        }
        gs.refreshAll(gameModel);
    }

    /**
     * Refreshes the game scene if it is currently active.
     */
    private void refreshGameIfActive() {
        withGameScene(this::renderScene);
    }

    /**
     * Handles the result of a nickname request.
     *
     * @param username The requested username.
     * @param accepted True if the nickname was accepted, false otherwise.
     * @param reason   The reason for rejection if applicable.
     */
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

    /**
     * Handles the update of the list of available lobbies.
     *
     * @param lobbies The list of available lobbies.
     */
    @Override
    public void onAvailableLobbiesUpdated(List<LobbyInfo> lobbies) {
        LobbyListScene lls = sceneRouter.getLobbyListScene();
        if (lls != null) lls.onAvailableLobbiesUpdated(lobbies);
        else if (sceneRouter.getLobbyScene() != null || sceneRouter.getGameScene() != null) sceneRouter.showGameMenuScene();
    }

    /**
     * Handles the update of the current lobby state.
     *
     * @param lobby The updated lobby information.
     */
    @Override
    public void onCurrentLobbyUpdated(LobbyInfo lobby) {
        LobbyScene ls = sceneRouter.getLobbyScene();
        if (ls == null) sceneRouter.showLobbyScene(lobby);
        else ls.updateLobbyState(lobby);
    }

    /**
     * Handles the dissolution of the current lobby.
     */
    @Override
    public void onLobbyDissolved() {
        sceneRouter.showBlockingAlert("Lobby Closed", "The lobby has been dissolved.", Alert.AlertType.INFORMATION);
        sceneRouter.showGameMenuScene();
    }

    /**
     * Handles the start of a game.
     *
     * @param gameId The unique identifier of the game.
     */
    @Override
    public void onGameStarted(String gameId) {
        lobbyModel.removeObserver(this);
        sceneRouter.switchToGameScene(gameModel);
    }

    /**
     * Handles the completion of the game setup phase.
     *
     * @param turnOrder    The initial turn order of players.
     * @param initialFood  The initial food distribution.
     * @param board        The initial board snapshot.
     */
    @Override
    public void onGameSetupCompleted(List<String> turnOrder, Map<String, Integer> initialFood, BoardSnapshot board) {
        // Decide create-vs-refresh on the FX thread so the scene field is read on
        // the same thread it is written, preventing both dropped updates and a
        // duplicate scene being created if this races a pending scene creation.
        Platform.runLater(() -> {
            GameScene gs = sceneRouter.getGameScene();
            if (gs != null) {
                gs.prepareForReplay();
                renderScene(gs);
            } else {
                sceneRouter.hideModal();
                sceneRouter.switchToGameScene(gameModel);
            }
        });
    }

    /**
     * Handles a change in the current game phase.
     *
     * @param phase           The new phase.
     * @param currentPlayer   The nickname of the current player.
     * @param resolutionOrder The order in which actions are resolved.
     */
    @Override
    public void onPhaseChanged(PhaseType phase, String currentPlayer, List<String> resolutionOrder) {
        Platform.runLater(() -> {
            GameScene gs = sceneRouter.getGameScene();
            if (gs == null) {
                sceneRouter.hideModal();
                sceneRouter.switchToGameScene(gameModel);
            } else renderScene(gs);
        });
    }

    /**
     * Handles a change in the current active player.
     *
     * @param nextPlayer The nickname of the next player.
     */
    @Override
    public void onCurrentPlayerChanged(String nextPlayer) { refreshGameIfActive(); }

    /**
     * Handles the establishment of the turn order for the round.
     *
     * @param turnOrder The established turn order.
     */
    @Override
    public void onTurnOrderEstablished(List<String> turnOrder) { refreshGameIfActive(); }

    /**
     * Handles the placement of a totem by a player.
     *
     * @param nickname The nickname of the player.
     * @param tileID   The ID of the tile where the totem was placed.
     */
    @Override
    public void onTotemPlaced(String nickname, char tileID) {
        withGameScene(gs -> {
            gs.logTotemPlaced(nickname, tileID);
            renderScene(gs);
        });
    }

    /**
     * Handles the return of a totem to the turn order track.
     *
     * @param nickname          The nickname of the player.
     * @param turnOrderPosition The position in the turn order track.
     */
    @Override
    public void onTotemReturned(String nickname, int turnOrderPosition) {
        withGameScene(gs -> {
            gs.logTotemReturned(nickname, turnOrderPosition);
            renderScene(gs);
        });
    }

    /**
     * Handles the update of the offer tiles on the board.
     *
     * @param offerTiles The list of updated offer tiles.
     */
    @Override
    public void onOfferTilesUpdated(List<OfferTileInfo> offerTiles) { refreshGameIfActive(); }

    /**
     * Handles the update of the game board.
     *
     * @param newUpperRow        The new IDs for the upper row.
     * @param newLowerRow        The new IDs for the lower row.
     * @param deckRemainingCount The number of cards remaining in the deck.
     */
    @Override
    public void onBoardUpdated(List<String> newUpperRow, List<String> newLowerRow, int deckRemainingCount) { refreshGameIfActive(); }

    /**
     * Handles a change in the game era.
     *
     * @param newEra               The new era.
     * @param newUpperRowBuildings The new buildings for the upper row.
     * @param newLowerRowBuildings The new buildings for the lower row.
     */
    @Override
    public void onEraChanged(Era newEra, List<String> newUpperRowBuildings, List<String> newLowerRowBuildings) {
        withGameScene(gs -> gs.showNewEraAnimation(newEra, newUpperRowBuildings, newLowerRowBuildings));
    }

    /**
     * Handles the initialization of pick limits for a player.
     *
     * @param nickname       The nickname of the player.
     * @param remainingUpper The remaining picks from the upper row.
     * @param remainingLower The remaining picks from the lower row.
     */
    @Override
    public void onPlayerLimitsInitialized(String nickname, int remainingUpper, int remainingLower) { refreshGameIfActive(); }

    /**
     * Handles the update of pick limits for a player.
     *
     * @param nickname       The nickname of the player.
     * @param remainingUpper The remaining picks from the upper row.
     * @param remainingLower The remaining picks from the lower row.
     */
    @Override
    public void onPlayerLimitsUpdated(String nickname, int remainingUpper, int remainingLower) { refreshGameIfActive(); }

    /**
     * Handles the acquisition of a card by a player.
     *
     * @param nickname  The nickname of the player.
     * @param cardID    The ID of the acquired card.
     * @param cardType  The type of the acquired card.
     * @param sourceRow The row from which the card was taken.
     */
    @Override
    public void onCardTaken(String nickname, String cardID, CardType cardType, RowPosition sourceRow) {
        withGameScene(gs -> {
            gs.logCardTaken(nickname, cardID);
            gs.animateCardTaken(nickname, cardID, cardType, sourceRow);
        });
    }

    /**
     * Handles the start of an extra turn for a player.
     *
     * @param nickname       The nickname of the player.
     * @param remainingUpper The remaining picks from the upper row.
     * @param remainingLower The remaining picks from the lower row.
     */
    @Override
    public void onExtraTurnStarted(String nickname, int remainingUpper, int remainingLower) {
        withGameScene(gs -> {
            gs.logExtraTurnStarted(nickname, remainingUpper, remainingLower);
            renderScene(gs);
        });
    }

    /**
     * Handles the end of an extra turn for a player.
     *
     * @param nickname The nickname of the player.
     */
    @Override
    public void onExtraTurnEnded(String nickname) {
        withGameScene(gs -> {
            gs.logExtraTurnEnded(nickname);
            renderScene(gs);
        });
    }

    /**
     * Handles the normal end of the game.
     *
     * @param winners       The list of winning players' nicknames.
     * @param finalRankings The final score rankings.
     */
    @Override
    public void onGameEnded(List<String> winners, List<PlayerFinalScore> finalRankings) {
        sceneRouter.showGameOverPopup("=== GAME OVER ===", winners, finalRankings);
    }

    /**
     * Handles the disconnection of a player.
     *
     * @param nickname The nickname of the disconnected player.
     */
    @Override
    public void onPlayerDisconnected(String nickname) {
        withGameScene(gs -> gs.setPlayerOffline(nickname));
        sceneRouter.showPlayerDisconnectedPopup(nickname);
    }

    /**
     * Handles the aborting of the game.
     *
     * @param lastManStanding The nickname of the last remaining player.
     */
    @Override
    public void onGameAborted(String lastManStanding) {
        sceneRouter.showGameOverPopup("Game Aborted", "Game ended. Last standing: " + lastManStanding);
    }

    // onGameRecoveryFailed is intentionally not overridden: it is a defensive
    // server-side fallback (see GameController) that only fires when the global
    // timer expires with no active player — a "should never happen" terminal
    // state in which no client is connected to display anything. The GUI
    // inherits the no-op default from AbstractClientView. The full pipeline is
    // kept for a future view that may want to surface this state.

    /**
     * Handles the reconnection of a player.
     *
     * @param nickname The nickname of the reconnected player.
     */
    @Override
    public void onPlayerReconnected(String nickname) {
        withGameScene(gs -> {
            gs.setPlayerOnline(nickname);
            gs.logPlayerReconnected(nickname);
        });
    }

    /**
     * Handles an error message from the server.
     *
     * @param message The error message.
     */
    @Override
    public void onError(String message) {
        sceneRouter.showToast("Error", message, Alert.AlertType.WARNING);
        LobbyScene ls = sceneRouter.getLobbyScene();
        if (ls != null) ls.onNicknameRejected();
    }

    /**
     * Handles a change in a player's resource value.
     *
     * @param nickname The nickname of the player.
     * @param resource The type of resource.
     * @param newValue The new value of the resource.
     */
    @Override
    public void onPlayerResourceChanged(String nickname, ResourceType resource, int newValue) {
        withGameScene(gs -> {
            gs.logResourceChanged(nickname, resource, newValue);
            renderScene(gs);
        });
    }

    /**
     * Handles the resolution of an event.
     *
     * @param eventID   The ID of the event card.
     * @param eventName The name of the event.
     */
    @Override
    public void onEventResolved(String eventID, String eventName) {
        withGameScene(gs -> {
            gs.logEventResolved(eventName);
            renderScene(gs);
        });
    }

    /**
     * Handles the start of the auto-player timer for a player.
     *
     * @param nickname The nickname of the player.
     */
    @Override
    public void onAutoPlayerTimerStarted(String nickname, long seconds) {
        withGameScene(gs -> gs.logAutoPlayerTimerStarted(nickname, seconds));
    }

    /**
     * Handles the invocation of the auto-player for a player.
     *
     * @param nickname The nickname of the player.
     */
    @Override
    public void onAutoPlayerInvoked(String nickname) {
        withGameScene(gs -> gs.logAutoPlayerInvoked(nickname));
    }

    /**
     * Handles the loss of connection to the server.
     *
     * <p>Marshalled onto the FX thread on purpose. The scene is created inside a
     * {@code Platform.runLater} (see {@code SceneRouter.switchToGameScene}), so on
     * reconnection — where the server arms the global timer right after the replay
     * drain — reading {@code getGameScene()} straight from the network thread
     * races that still-pending FX task and intermittently sees {@code null},
     * silently dropping the banner. Deferring here lets FX FIFO ordering guarantee
     * the scene-creating event's task has already run. If the scene still is not
     * available (e.g. an out-of-order transport), the value is remembered and
     * flushed by {@link #refreshGameIfActive()} on the next render.
     */
    @Override
    public void onGlobalTimerStarted(long seconds) {
        Platform.runLater(() -> {
            // Reset first so the direct-show path and the pending fallback are
            // mutually exclusive: the banner can be raised by exactly one of them,
            // never by both (which would otherwise restart the countdown on the
            // next render).
            pendingForfeitSeconds = null;
            GameScene gs = sceneRouter.getGameScene();
            if (gs != null) gs.showForfeitBanner(seconds);
            else pendingForfeitSeconds = seconds;
        });
    }

    @Override
    public void onGlobalTimerCancelled() {
        Platform.runLater(() -> {
            pendingForfeitSeconds = null;
            GameScene gs = sceneRouter.getGameScene();
            if (gs != null) gs.dismissForfeitBanner();
        });
    }

    @Override
    public void onConnectionLost() { sceneRouter.showConnectionLost(); }

    /**
     * Handles the restoration of connection to the server.
     */
    @Override
    public void onConnectionRestored() {
        sceneRouter.hideModal();
        sceneRouter.showToast("Connected", "You're back online!", Alert.AlertType.INFORMATION);
    }

    /**
     * Handles the return to the lobby screen.
     */
    @Override
    public void onReturnToLobby() { sceneRouter.showGameMenuScene(); }
}