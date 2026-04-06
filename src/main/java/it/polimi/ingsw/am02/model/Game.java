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

    private List<GameObserver> observers;




    public Game(String gameID, List<String> nicknames; Map<String, Totem> chosenTotems) {

        this.gameID = gameID;
        this.numPlayers = nicknames.size();

        this.players = new LinkedHashMap<>();
        for(String nickname : nicknames) {
            Totem totem = chosenTotems.get(nickname);
            this.players.put(nickname, new Player(nickname, totem));
        }

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




    public static void attachPhaseObserver(PhaseObserver effect) {
        // TODO: Husnain
    }

    public static void notifyPhaseObservers(PhaseType phase) {
        // TODO : Husnain

        // IDEA (Matteo):
       /* if (!isExtraTurnMode) {
            for (PhaseObserver observer : phaseObservers) {
                observer.onPhaseChange(phase);
            }
        }*/
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

        public abstract void onEntryActions();
    }


    // Concrete States
    private class SetUpState extends BaseState {

        public SetUpState() {
            super(PhaseType.SETUP);
        }

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

        public void onEntryActions() {}
    }

    private class ActionResolutionState extends BaseState {

        public ActionResolutionState() {
            super(PhaseType.ACTION_RESOLUTION);
        }

        public void moveTotem(String nickname, char tileID) {
            validatePlayerTurn(nickname);
            Player player = getPlayerByNickname(nickname);

            if (tileID != TURN_ORDER_TILE_ID) {
                throw new IllegalArgumentException("Invalid tile destination in ActionResolutionState");
            }

            if (gameBoard.canPlayerFinish(player)) {
                gameBoard.movePlayerToTurnOrder(player);
                transitionTo(new EndPlayerTurnState());
            } else {
                throw new IllegalStateException("Player has not fulfilled pick obligations");
            }
        }

        public void resolveActions(String nickname, List<String> selectedIDs) {
            validatePlayerTurn(nickname);
            Player player = getPlayerByNickname(nickname);

            gameBoard.processActionSelection(player, selectedIDs);

            // TODO: gestione turno ulteriore (effetto building)
        }

        public void onEntryActions() {}
    }



}

