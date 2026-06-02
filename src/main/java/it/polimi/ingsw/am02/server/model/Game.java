package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.BoardSnapshot;
import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.PlayerFinalScore;
import it.polimi.ingsw.am02.server.controller.ModelInterface;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.exceptions.InvalidMoveException;
import it.polimi.ingsw.am02.server.model.exceptions.NotYourTurnException;
import it.polimi.ingsw.am02.server.model.exceptions.PickObligationNotFulfilledException;
import it.polimi.ingsw.am02.server.model.exceptions.PlayerNotFoundException;
import it.polimi.ingsw.am02.server.model.listeners.GameNotifier;
import it.polimi.ingsw.am02.server.model.listeners.GameObserver;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.player.Tribu;

import java.util.*;

import static java.util.Collections.shuffle;

/**
 * Central domain object representing a single game session.
 *
 * <p>{@code Game} orchestrates the full game lifecycle via an internal State Machine
 * ({@link GameState} / {@link BaseState} and concrete inner states). It holds the authoritative
 * list of {@link Player}s, delegates board operations to {@link GameBoard}, and broadcasts
 * all observable changes through {@link GameNotifier}.
 *
 * <p>The FSM is started by calling {@link #startFSM()}. From that point on, players interact
 * exclusively through {@link #moveTotem(String, char)} and {@link #resolveActions(String, List)},
 * which are forwarded to the current state.
 */
public class Game implements ModelInterface {

    private final String gameID;
    private final int numPlayers;
    private final Map<String, Player> players;
    private GameBoard gameBoard;
    private GameState currentState;
    private String currentPlayerNickname;
    private List<String> turnOrder;
    private int completedRounds;
    private static final int NUM_ROUNDS = 10;

    private final List<PhaseObserver> phaseObservers;

    private boolean isExtraTurnMode;
    private String extraTurnPlayerNickname;
    private int extraTurnUpperPicks;
    private int extraTurnLowerPicks;

    private final GameNotifier notifier;
    private final Random gameRandom;

    /**
     * Creates a new Game with a seeded random source.
     * Players are created from the provided nickname list using the chosen totems.
     *
     * @param gameID      unique identifier for this game session
     * @param nicknames   ordered list of player nicknames (determines initial insertion order)
     * @param chosenTotems mapping from nickname to the totem each player has chosen
     * @param seed        seed for the internal {@link Random}, ensuring reproducible shuffles
     */
    public Game(String gameID, List<String> nicknames, Map<String, Totem> chosenTotems, long seed) {

        this.gameID = gameID;
        this.numPlayers = nicknames.size();

        this.players = new LinkedHashMap<>();
        for(String nickname : nicknames) {
            Totem totem = chosenTotems.get(nickname);
            this.players.put(nickname, new Player(nickname, totem));
        }

        this.completedRounds = 0;
        this.phaseObservers = new ArrayList<>();

        this.isExtraTurnMode = false;
        this.extraTurnPlayerNickname = null;
        this.extraTurnUpperPicks = 0;
        this.extraTurnLowerPicks = 0;

        this.notifier = new GameNotifier();
        this.gameRandom = new Random(seed);
    }


    /**
     * Moves a player's totem to an offer tile or back to the turn-order tile,
     * depending on the current game state.
     *
     * @param nickname the player issuing the move
     * @param tileID   the target tile character identifier (e.g. {@code 'A'}, {@code 'B'}, or {@code 'T'} for turn-order)
     * @throws InvalidMoveException  if totem movement is not allowed in the current state
     * @throws NotYourTurnException  if it is not this player's turn
     */
    public void moveTotem(String nickname, char tileID) {
        currentState.moveTotem(nickname, tileID);
    }

    /**
     * Resolves card-selection actions for the current player.
     *
     * @param nickname    the player issuing the action
     * @param selectedIDs list of card IDs the player wishes to acquire this step
     * @throws InvalidMoveException if action resolution is not allowed in the current state
     * @throws NotYourTurnException if it is not this player's turn
     */
    public void resolveActions(String nickname, List<String> selectedIDs) { currentState.resolveActions(nickname, selectedIDs); }

    /**
     * Registers a {@link GameObserver} to receive all game-level notifications
     * (phase changes, resource updates, game end, etc.).
     *
     * @param observer the observer to add
     */
    public void addGameObserver(GameObserver observer) {
        notifier.addObserver(observer);
    }

    /**
     * Removes a previously registered {@link GameObserver}.
     *
     * @param observer the observer to remove
     */
    public void removeGameObserver(GameObserver observer) {
        notifier.removeObserver(observer);
    }

    /**
     * Starts the FSM by entering the {@code SetUpState}, which initializes the board,
     * randomizes the turn order, and immediately transitions into totem placement.
     * Must be called exactly once after the game object is constructed.
     */
    public void startFSM() { transitionTo(new SetUpState()); }


    // Helper methods
      Player getPlayerByNickname(String nickname) {
        Player p = players.get(nickname);
        if (p == null)
            throw new PlayerNotFoundException(nickname);
        return p;
    }

    private void validatePlayerTurn(String nickname) {
        if(!nickname.equals(currentPlayerNickname)) {
            throw new NotYourTurnException(nickname);
        }
    }

    private void nextPlayer() {
        int currentPlayerIndex = turnOrder.indexOf(currentPlayerNickname);
        int nextIndex = (currentPlayerIndex + 1) % turnOrder.size();
        currentPlayerNickname = turnOrder.get(nextIndex);
    }

    private void initializeBoard() { // Initializes GameBoard
        gameBoard = new GameBoard(numPlayers, notifier, gameRandom);
    }

    private void randomizeInitialTurnOrder() {
        // (a) Shuffles the nickname list
        turnOrder = new ArrayList<>(players.keySet());
        Collections.shuffle(turnOrder, gameRandom);

        // (b) Sets the first player that has right to play
        currentPlayerNickname = turnOrder.getFirst();

        // (c) Builds a List<Player> using the shuffled turnOrder list
        List<Player> orderedPlayers = new ArrayList<>();
        for(String nickname : turnOrder) {
            orderedPlayers.add(players.get(nickname));
        }

        // (d) Delegates the turn order setup to GameBoard
        gameBoard.setUpInitialTurnOrder(orderedPlayers);
    }


    private void transitionTo(GameState nextState) {
        this.currentState = nextState;
        currentState.onEntry();
    }

    private boolean checkAllTotemsPlaced() {
        return gameBoard.areAllTotemsPlaced();
    }

    private void setUpActionResolutionTurnOrder() {
        turnOrder.clear();

        List<Player> playersInResolutionOrder = gameBoard.getPlayersInResolutionOrder();
        for(Player player : playersInResolutionOrder) {
            turnOrder.add(player.getNickname());
        }

        currentPlayerNickname = turnOrder.getFirst();
        Player currentPlayer = getPlayerByNickname(currentPlayerNickname);


        // Notify observers about the transition to action resolution with current turn order
        notifier.notifyPhaseChanged(PhaseType.ACTION_RESOLUTION, currentPlayerNickname, List.copyOf(turnOrder));

        gameBoard.initializePlayerLimits(currentPlayer);

        transitionTo(new ActionResolutionState());
    }

    private void executeEndTurnRewards(Player player) {
        gameBoard.applyTurnOrderRewards(player);
    }

    private boolean checkAllTotemsReturned() {
        return numPlayers == gameBoard.getPlayersOnTurnOrderCount();
    }

    private boolean areRoundEventsToResolve() {
        return gameBoard.hasRoundEvents();
    }

    private void executeRoundEventsResolution() {
        gameBoard.resolveRoundEvents(players.values().stream().toList());
    }

    private void executeNewRoundPreparation() {
        completedRounds++;
        gameBoard.prepareNewRound(numPlayers);
    }

    private boolean areEraChangesToResolve() {
        return gameBoard.hasEraChanged();
    }

    private boolean isGameOverCondition() {
        return completedRounds > NUM_ROUNDS;
    }

    private void setUpPlacementOrder() {
        List<Player> orderedPlayers = gameBoard.getPlayersInPlacementOrder();
        turnOrder.clear();

        for(Player player : orderedPlayers) {
            turnOrder.add(player.getNickname());
        }

        currentPlayerNickname = turnOrder.getFirst();
    }

    private void executeNewEraPreparation() {
        gameBoard.updateRowsForNewEra();
    }

    private boolean areFinalEventsToResolve() {
        return gameBoard.hasFinalEvents();
    }

    private void executeFinalEventsResolution() {
        gameBoard.resolveFinalEvents(players.values().stream().toList());
    }

    private List<String> determineWinner() {

        int maxPP = players.values().stream()
                .mapToInt(p -> p.getTribu().getPrestigePoints())
                .max()
                .orElse(0);

        List<Player> candidates = players.values().stream()
                .filter(p -> p.getTribu().getPrestigePoints() == maxPP)
                .toList();

        if (candidates.size() == 1) {
            return List.of(candidates.getFirst().getNickname());
        }

        int maxFood = candidates.stream()
                .mapToInt(p -> p.getTribu().getFoodPoints())
                .max()
                .orElse(0);

        candidates = candidates.stream()
                .filter(p -> p.getTribu().getFoodPoints() == maxFood)
                .toList();

        return candidates.stream()
                .map(Player::getNickname)
                .toList();
    }

    private List<PlayerFinalScore> calculateFinalScores() {
        List<PlayerFinalScore> finalScoresList = new ArrayList<>();

        for (Player player : players.values()) {
            Tribu tribu = player.getTribu();
            String nickname = player.getNickname();

            int ppBuilders = tribu.getPPBuilders();
            int ppBuildings = tribu.getTotalPPBuildings();

            int inventors = tribu.getCharacterCount(CharacterType.INVENTOR);
            int inventionTypes = tribu.getNumOfDifferentInventionTypes();
            int ppInventors = inventors * inventionTypes;

            int artists = tribu.getCharacterCount(CharacterType.ARTIST);
            int ppArtists = (artists / 2) * 10;

            int ppToAdd = ppBuilders + ppBuildings + ppInventors + ppArtists;
            tribu.addPrestigePoints(ppToAdd);

            PlayerFinalScore scoreDto = new PlayerFinalScore(
                    nickname,
                    tribu.getPrestigePoints(),
                    ppBuilders,
                    ppBuildings,
                    ppInventors,
                    ppArtists
            );

            finalScoresList.add(scoreDto);
        }

        return finalScoresList;
    }

    /**
     * Enqueues an extra turn for the given player.
     * The extra turn is activated at the beginning of the next {@code EndRoundState}.
     *
     * @param nickname        the player who will receive the extra turn
     * @param extraUpperPicks the number of additional upper-row picks allowed
     * @param extraLowerPicks the number of additional lower-row picks allowed
     */
    public void enqueueExtraTurn(String nickname, int extraUpperPicks, int extraLowerPicks) {
        this.extraTurnPlayerNickname = nickname;
        this.extraTurnUpperPicks = extraUpperPicks;
        this.extraTurnLowerPicks = extraLowerPicks;
    }

    /**
     * Attaches a {@link PhaseObserver} that is notified whenever the game transitions
     * between phases (setup, totem placement, action resolution, etc.).
     *
     * @param effect the observer to attach
     */
    public void attachPhaseObserver(PhaseObserver effect) {
        phaseObservers.add(effect);
    }

    private void notifyPhaseObservers(PhaseType phase) {
        if (!isExtraTurnMode) {
            for (PhaseObserver phaseObserver : phaseObservers) {
                EffectOutcome outcome = phaseObserver.onPhaseChange(phase);
                notifier.emitOutcome(outcome);
            }
        }
    }

    /**
     * @return the {@link GameBoard} for this game session
     */
    public GameBoard getGameBoard() {
        return this.gameBoard;
    }

    /**
     * Grants an extra food bonus from the turn-order tile to the current player,
     * triggered by a building effect that observes tribe changes.
     *
     * @param tribu the tribe whose building triggered the bonus;
     *              the bonus is applied only if it belongs to the current player
     */
    public void triggerTurnOrderExtraFood(Tribu tribu) {

        Player current = players.get(currentPlayerNickname);

        if (current.getTribu() == tribu) {
            gameBoard.applyExtraTurnOrderBonus(current);
        }
    }


    // State Pattern with "Inner Classes"
    private interface GameState {
        void moveTotem(String nickname, char tileID);
        void resolveActions(String nickname, List<String> selectedIDs);
        void onEntry();
    }

    private abstract class BaseState implements GameState {

        private final PhaseType phaseType;

        BaseState(PhaseType phase) {
            this.phaseType = phase;
        }

        public void moveTotem(String nickname, char tileID) {
            throw new InvalidMoveException("Moving Totem is not allowed in the current game state.");
        }

        public void resolveActions(String nickname, List<String> selectedIDs) {
            throw new InvalidMoveException("Resolving actions is not allowed in the current game state.");
        }

        public final void onEntry() {
            notifyPhaseObservers(phaseType);
            onEntryActions();
        }

        public void onEntryActions() {}
    }


    // Concrete States
    private class SetUpState extends BaseState {

        public SetUpState() {
            super(PhaseType.SETUP);
        }

        @Override
        public void onEntryActions() {
            initializeBoard();
            randomizeInitialTurnOrder();
            completedRounds = 1;

            // Notify game observers that the setup is complete
            BoardSnapshot snapshot = gameBoard.buildSnapshot();
            Map<String, Integer> initialFood = new LinkedHashMap<>();
            Map<String, Totem> totemByPlayer = new LinkedHashMap<>();
            for (String nick : turnOrder) {
                Player p = getPlayerByNickname(nick);
                initialFood.put(nick, p.getTribu().getFoodPoints());
                totemByPlayer.put(nick, p.getTotem());
            }

            notifier.notifyGameSetupCompleted(Map.copyOf(totemByPlayer), List.copyOf(turnOrder), Map.copyOf(initialFood), snapshot);

            transitionTo(new TotemPlacementState());
        }
    }


    private class TotemPlacementState extends BaseState {

        public TotemPlacementState() {
            super(PhaseType.TOTEM_PLACEMENT);
        }

        // Notify observers of phase change to totem placement
        @Override
        public void onEntryActions() {
            notifier.notifyPhaseChanged(PhaseType.TOTEM_PLACEMENT, currentPlayerNickname);
        }

        public void moveTotem(String nickname, char tileID) {
            validatePlayerTurn(nickname);
            Player player = getPlayerByNickname(nickname);
            gameBoard.movePlayerToOffer(player, tileID);
            
            if(checkAllTotemsPlaced()) {
                setUpActionResolutionTurnOrder();
            } else {
                nextPlayer();

                // Notify observers that the current player has changed
                notifier.notifyCurrentPlayerChanged(currentPlayerNickname);
            }
        }
    }


    private class ActionResolutionState extends BaseState {

        public ActionResolutionState() {
            super(PhaseType.ACTION_RESOLUTION);
        }

        public void moveTotem(String nickname, char tileID) {
            validatePlayerTurn(nickname);
            Player player = getPlayerByNickname(nickname);

            if (tileID != 'T') {
                throw new InvalidMoveException("Totem must return to the TurnOrderTile (destination 'T').");
            }

            if (isExtraTurnMode) {
                String endedNickname = extraTurnPlayerNickname;

                isExtraTurnMode = false;
                extraTurnPlayerNickname = null;
                extraTurnUpperPicks = 0;
                extraTurnLowerPicks = 0;
                gameBoard.clearExtraTurn();

                // Notify observers that the extra turn has ended
                notifier.notifyExtraTurnEnded(endedNickname);

                if (areRoundEventsToResolve()) {
                    transitionTo(new EventResolutionState());
                } else {
                    transitionTo(new NewRoundState());
                }
            } else if (gameBoard.canPlayerFinish(player)) {
                gameBoard.movePlayerToTurnOrder(player);
                transitionTo(new EndPlayerTurnState());
            } else {
                throw new PickObligationNotFulfilledException();
            }
        }

        public void resolveActions(String nickname, List<String> selectedIDs) {
            validatePlayerTurn(nickname);
            Player player = getPlayerByNickname(nickname);

            if (isExtraTurnMode) {
                gameBoard.processExtraActionSelection(player, selectedIDs, Game.this);
            } else {
                gameBoard.processActionSelection(player, selectedIDs, Game.this);
            }


        }
    }


    private class EndPlayerTurnState extends BaseState {

        public EndPlayerTurnState() {
            super(PhaseType.END_PLAYER_TURN);
        }

        @Override
        public void onEntryActions() {
            Player player = getPlayerByNickname(currentPlayerNickname);
            executeEndTurnRewards(player);

            if(checkAllTotemsReturned()) {
                transitionTo(new EndRoundState());
            } else {
                nextPlayer();

                // Notify observers that the current player has changed
                notifier.notifyCurrentPlayerChanged(currentPlayerNickname);
                gameBoard.initializePlayerLimits(getPlayerByNickname(currentPlayerNickname));

                transitionTo(new ActionResolutionState());
            }
        }
    }


    private class EndRoundState extends BaseState {

        public EndRoundState() {
            super(PhaseType.END_ROUND);
        }

        @Override
        public void onEntryActions() {

            // Notify observers the end-of-round phase transition
            notifier.notifyPhaseChanged(PhaseType.END_ROUND);

            if (extraTurnPlayerNickname != null) {
                isExtraTurnMode = true;
                currentPlayerNickname = extraTurnPlayerNickname;
                gameBoard.initializeExtraPlayerLimits(
                        getPlayerByNickname(extraTurnPlayerNickname),
                        extraTurnUpperPicks,
                        extraTurnLowerPicks);

                // Notify observers that an extra turn has started for a player
                notifier.notifyPhaseChanged(PhaseType.ACTION_RESOLUTION, currentPlayerNickname);
                notifier.notifyExtraTurnStarted(extraTurnPlayerNickname, extraTurnUpperPicks, extraTurnLowerPicks);

                transitionTo(new ActionResolutionState());

            } else if(areRoundEventsToResolve()) {
                transitionTo(new EventResolutionState());
            } else {
                transitionTo(new NewRoundState());
            }
        }

    }


    private class EventResolutionState extends BaseState {

        public EventResolutionState() {
            super(PhaseType.EVENT_RESOLUTION);
        }

        @Override
        public void onEntryActions() {
            executeRoundEventsResolution();

            transitionTo(new NewRoundState());
        }
    }



    private class NewRoundState extends BaseState {

        public NewRoundState() {
            super(PhaseType.NEW_ROUND);
        }

        @Override
        public void onEntryActions() {
            executeNewRoundPreparation();

            if(areEraChangesToResolve()) {
                setUpPlacementOrder();

                // Notify observers that the turn order has been established
                notifier.notifyTurnOrderEstablished(List.copyOf(turnOrder));

                transitionTo(new NewEraState());

            } else if (isGameOverCondition()) {
                if (areFinalEventsToResolve()) {
                    transitionTo(new FinalEventsResolutionState());
                } else {
                    transitionTo(new FinalScoringState());
                }
            }  else {
                setUpPlacementOrder();

                // Notify observers that the turn order has been established
                notifier.notifyTurnOrderEstablished(List.copyOf(turnOrder));

                transitionTo(new TotemPlacementState());
            }
        }
    }



    private class NewEraState extends BaseState {

        public NewEraState() {
            super(PhaseType.NEW_ERA);
        }

        @Override
        public void onEntryActions() {

            // Notify observers that a new era has begun
            notifier.notifyPhaseChanged(PhaseType.NEW_ERA);

            executeNewEraPreparation();

            transitionTo(new TotemPlacementState());
        }
    }



    private class FinalEventsResolutionState extends BaseState {

        public FinalEventsResolutionState() {
            super(PhaseType.FINAL_EVENT_RESOLUTION);
        }

        @Override
        public void onEntryActions() {
            executeFinalEventsResolution();

            transitionTo(new FinalScoringState());
        }
    }


    private class FinalScoringState extends BaseState {

        public FinalScoringState() {
            super(PhaseType.END_GAME);
        }

        @Override
        public void onEntryActions() {
            List<PlayerFinalScore> finalScores = calculateFinalScores();

            List<String> winners = determineWinner();

            for(String winner : winners) {
                Player winnerPlayer = getPlayerByNickname(winner);
                winnerPlayer.setAsWinner(true);
            }

            notifier.notifyGameEnded(winners, finalScores);
        }
    }


    // FOR TESTING
    String getCurrentPlayerNickname() { return currentPlayerNickname; } // FOR TESTING
    List<String> getTurnOrder() { return turnOrder; } // FOR TESTING

    /**
     * FOR TESTING ONLY.
     * Instantly grants a building card to a player, bypassing deck draw,
     * board availability, and food cost requirements.
     * Call after {@code startFSM()} has completed setup.
     *
     * @param nickname the player to receive the building
     * @param cardID   the building card ID to grant (e.g. "B_020")
     */
    public void injectBuildingForTesting(String nickname, String cardID) { // FOR TESTING
        Player player = getPlayerByNickname(nickname);
        player.getTribu().insertBuilding(cardID, player, this);
    }
}

