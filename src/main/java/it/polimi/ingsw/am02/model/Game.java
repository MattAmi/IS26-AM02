package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.PhaseType;
import it.polimi.ingsw.am02.model.Enumerations.Totem;

import java.util.*;

import static java.util.Collections.shuffle;

public class Game {

    private final String gameID;
    private final int numPlayers;
    private final Map<String, Player> players;
    private GameBoard gameBoard;
    private GameState currentState;
    private String currentPlayerNickname;
    private List<String> turnOrder;

    private List<PhaseObserver> phaseObservers;

    private boolean isExtraTurnMode;
    private String extraTurnPlayerNickname;
    private int extraTurnUpperPicks;
    private int extraTurnLowerPicks;

    private List<GameObserver> observers;




    public Game(String gameID, List<String> nicknames, Map<String, Totem> chosenTotems) {

        this.gameID = gameID;
        this.numPlayers = nicknames.size();

        this.players = new LinkedHashMap<>();
        for(String nickname : nicknames) {
            Totem totem = chosenTotems.get(nickname);
            this.players.put(nickname, new Player(nickname, totem));
        }

        this.phaseObservers = new ArrayList<>();

        this.isExtraTurnMode = false;
        this.extraTurnPlayerNickname = null;
        this.extraTurnUpperPicks = 0;
        this.extraTurnLowerPicks = 0;

        this.observers = new ArrayList<>(); // Per parte di rete

        transitionTo(new SetUpState());
    }


    // Interface methods
    public void moveTotem(String nickname, char tileID) {
        currentState.moveTotem(nickname, tileID);
    }

    public void resolveActions(String nickname, List<String> selectedIDs) {
        currentState.resolveActions(nickname, selectedIDs);
    }



    // Helper methods
    private Player getPlayerByNickname(String nickname) {
        Player p = players.get(nickname);
        if (p == null)
            throw new NoSuchPlayerException(); // TO DO
        return p;
    }

    private void validatePlayerTurn(String nickname) {
        if(!nickname.equals(currentPlayerNickname)) {
            throw new NotYourTurnException(); // TO DO
        }
    }

    private void nextPlayer() {
        int currentPlayerIndex = turnOrder.indexOf(currentPlayerNickname);
        int nextIndex = (currentPlayerIndex + 1) % turnOrder.size();
        currentPlayerNickname = turnOrder.get(nextIndex);
        // notifyObservers(); TO DO QUANDO FAREMO OBSERVER
    }



    private void initializeBoard() { // Initializes GameBoard
        gameBoard = new GameBoard(numPlayers);
    }

    private void randomizeInitialTurnOrder() {
        // (a) Shuffles the nickname list
        turnOrder = new ArrayList<>(players.keySet());
        shuffle(turnOrder);

        // (b) Sets the first player that has right to play
        currentPlayerNickname = turnOrder.get(0);

        // (c) Builds a List<Player> using the suffled turnOrder list
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

        currentPlayerNickname = turnOrder.get(0);
        Player currentPlayer = getPlayerByNickname(currentPlayerNickname);

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
        gameBoard.resolveRoundEvents(players.values());
    }

    private void executeNewRoundPreparation() {
        gameBoard.prepareNewRound(numPlayers);
    }

    private boolean areEraChangesToResolve() {
        return gameBoard.hasEraChanged();
    }

    private boolean isGameOverCondition() {
        return gameBoard.isTribuDeckEmpty();
    }

    private void setUpPlacementOrder() {
        List<Player> orderedPlayers = gameBoard.getPlayersInPlacementOrder();
        turnOrder.clear();

        for(Player player : orderedPlayers) {
            turnOrder.add(player.getNickname());
        }

        currentPlayerNickname = turnOrder.get(0);
    }

    private void executeNewEraPreparation() {
        gameBoard.updateRowsForNewEra();
    }

    private boolean areFinalEventsToResolve() {
        return gameBoard.hasFinalEvents();
    }

    private void executeFinalEventsResolution() {
        gameBoard.resolveFinalEvents(players.values());
    }

    private List<String> determineWinner() {
        // TODO: Matteo

        return
    }

    private void calculateFinalScores() {
        // TODO: Matteo
    }


    public void enqueueExtraTurn(String nickname, int extraUpperPicks, int extraLowerPicks) {
        this.extraTurnPlayerNickname = nickname;
        this.extraTurnUpperPicks = extraUpperPicks;
        this.extraTurnLowerPicks = extraLowerPicks;
    }



    public void attachPhaseObserver(PhaseObserver effect) {
        // TODO: Husnain
    }

    public void notifyPhaseObservers(PhaseType phase) {
      if (!isExtraTurnMode) {
            for (PhaseObserver phaseObserver : phaseObservers) {
                phaseObserver.onPhaseChange(phase);
            }
        }
    }

    public GameBoard getGameBoard() {
        return this.gameBoard;
    }


    // State Pattern with "Inner Classes"
    private interface GameState {
        public void moveTotem(String nickname, char tileID);
        public void resolveActions(String nickname, List<String> selectedIDs);
        public void onEntry();
    }

    private abstract class BaseState implements GameState {

        private final PhaseType phaseType;

        BaseState(PhaseType phase) {
            this.phaseType = phase;
        }

        public void moveTotem(String nickname, char tileID) {
            throw new InvalidActionException(); // lancio eccezione siccome non è possibile fare questa mossa in questo momento
        }

        public void resolveActions(String nickname, List<String> selectedIDs) {
            throw new InvalidActionException(); // lancio eccezione siccome non è possibile fare questa mossa in questo momento
        }

        public final void onEntry() {
            notifyPhaseObservers(phaseType);
            onEntryActions();
        }

        public void onEntryActions() {};
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

            transitionTo(new TotemPlacementState());
        };
    }


    private class TotemPlacementState extends BaseState {

        public TotemPlacementState() {
            super(PhaseType.TOTEM_PLACEMENT);
        }

        public void moveTotem(String nickname, char tileID) {
            validatePlayerTurn(nickname);
            Player player = getPlayerByNickname(nickname);
            gameBoard.movePlayerToOffer(player, tileID);
            
            if(checkAllTotemsPlaced()) {
                setUpActionResolutionTurnOrder();
            } else {
                nextPlayer();
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

            if (tileID != TURN_ORDER_TILE_ID) {
                throw new IllegalArgumentException("Invalid tile destination in ActionResolutionState"); //TO DO
            }

            if (isExtraTurnMode) {
                isExtraTurnMode = false;
                extraTurnPlayerNickname = null;
                extraTurnUpperPicks = 0;
                extraTurnLowerPicks = 0;

                if (areRoundEventsToResolve()) {
                    transitionTo(new EventResolutionState());
                } else {
                    transitionTo(new NewRoundState());
                }
            } else if (gameBoard.canPlayerFinish(player)) {
                gameBoard.movePlayerToTurnOrder(player);
                transitionTo(new EndPlayerTurnState());
            } else {
                throw new IllegalStateException("Player has not fulfilled pick obligations"); // TO DO
            }
        }

        public void resolveActions(String nickname, List<String> selectedIDs) {
            validatePlayerTurn(nickname);
            Player player = getPlayerByNickname(nickname);

            if (isExtraTurnMode) {
                gameBoard.processExtraActionSelection(player, selectedIDs);
            } else {
                gameBoard.processActionSelection(player, selectedIDs);
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

            if (extraTurnPlayerNickname != null) {
                isExtraTurnMode = true;
                currentPlayerNickname = extraTurnPlayerNickname;
                gameBoard.initializeExtraPlayerLimits(getPlayerByNickname(extraTurnPlayerNickname), extraTurnUpperPicks, extraTurnLowerPicks);

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
                transitionTo(new NewEraState());
            } else if (isGameOverCondition()) {
                if (areFinalEventsToResolve()) {
                    transitionTo(new FinalEventsResolutionState());
                } else {
                    transitionTo(new FinalScoringState());
                }
            }  else {
                setUpPlacementOrder();
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
            calculateFinalScores();
            List<String> winners = determineWinner();

            // TODO: Matteo
        }
    }



}

