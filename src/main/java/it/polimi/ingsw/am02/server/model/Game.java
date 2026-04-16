package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.server.controller.ModelInterface;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.server.model.exceptions.InvalidMoveException;
import it.polimi.ingsw.am02.server.model.exceptions.NotYourTurnException;
import it.polimi.ingsw.am02.server.model.exceptions.PickObligationNotFulfilledException;
import it.polimi.ingsw.am02.server.model.exceptions.PlayerNotFoundException;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

import java.util.*;

import static java.util.Collections.shuffle;

public class Game implements ModelInterface {

    private final String gameID;
    private final int numPlayers;
    private final Map<String, Player> players;
    private GameBoard gameBoard;
    private GameState currentState;
    private String currentPlayerNickname;
    private List<String> turnOrder;

    private final List<PhaseObserver> phaseObservers;

    private boolean isExtraTurnMode;
    private String extraTurnPlayerNickname;
    private int extraTurnUpperPicks;
    private int extraTurnLowerPicks;

    // private final List<GameObserver> gameObservers; // TODO


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

        // this.gameObservers = new ArrayList<>(); // Per parte di rete

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

    private void calculateFinalScores() {
        for (Player player : players.values()) {
            Tribu tribu = player.getTribu();

            int inventors = tribu.getCharacterCount(CharacterType.INVENTOR);
            int inventionTypes = tribu.getNumOfDifferentInventionTypes();

            int artists = tribu.getCharacterCount(CharacterType.ARTIST);

            int ppToAdd = tribu.getPPBuilders() + tribu.getTotalPPBuildings() + inventors * inventionTypes + (artists / 2) * 10;

            tribu.addPrestigePoints(ppToAdd);

        }
    }

    public void enqueueExtraTurn(String nickname, int extraUpperPicks, int extraLowerPicks) {
        this.extraTurnPlayerNickname = nickname;
        this.extraTurnUpperPicks = extraUpperPicks;
        this.extraTurnLowerPicks = extraLowerPicks;
    }

    public void attachPhaseObserver(PhaseObserver effect) {
        phaseObservers.add(effect);
    }

    private void notifyPhaseObservers(PhaseType phase) {
      if (!isExtraTurnMode) {
            for (PhaseObserver phaseObserver : phaseObservers) {
                phaseObserver.onPhaseChange(phase);
            }
        }
    }

    public GameBoard getGameBoard() {
        return this.gameBoard;
    }

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
            throw new InvalidMoveException("moving Totem is not allowed in the current game state.");
        }

        public void resolveActions(String nickname, List<String> selectedIDs) {
            throw new InvalidMoveException("resolving actions is not allowed in the current game state.");
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

            transitionTo(new TotemPlacementState());
        }
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

            if (tileID != 'T') {
                throw new InvalidMoveException("Totem must return to the TurnOrderTile (destination 'T').");
            }

            if (isExtraTurnMode) {
                isExtraTurnMode = false;
                extraTurnPlayerNickname = null;
                extraTurnUpperPicks = 0;
                extraTurnLowerPicks = 0;
                gameBoard.clearExtraTurn();

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
                gameBoard.initializeExtraPlayerLimits(
                        getPlayerByNickname(extraTurnPlayerNickname),
                        extraTurnUpperPicks,
                        extraTurnLowerPicks);

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

            for(String winner : winners) {
                Player winnerPlayer = getPlayerByNickname(winner);
                winnerPlayer.setAsWinner(true);
            }

            // TODO: notifica verso la view del/dei vincitori
        }
    }


    // FOR TESTING
    String getCurrentPlayerNickname() { return currentPlayerNickname; } // FOR TESTING
    List<String> getTurnOrder() { return turnOrder; } // FOR TESTING
}

