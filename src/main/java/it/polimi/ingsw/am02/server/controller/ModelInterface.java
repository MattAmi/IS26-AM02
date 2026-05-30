package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.server.model.listeners.GameObserver;

import java.util.List;

/**
 * Minimal abstraction of the game model exposed to the controller layer.
 * Implemented by {@link it.polimi.ingsw.am02.server.model.Game}.
 *
 * <p>Keeping the controller dependent on this interface rather than directly
 * on {@code Game} makes it possible to replace the model with a test double
 * during unit testing.
 */
public interface ModelInterface {

    /**
     * Registers a {@link it.polimi.ingsw.am02.server.model.listeners.GameObserver}
     * to receive all game-level notifications.
     *
     * @param observer the observer to add
     */
    void addGameObserver(GameObserver observer);

    /**
     * Removes a previously registered observer.
     *
     * @param observer the observer to remove
     */
    void removeGameObserver(GameObserver observer);

    /**
     * Moves the given player's totem to the specified offer tile,
     * or returns it to the turn-order tile.
     *
     * @param nickname the player performing the action
     * @param tileID   the target tile identifier ({@code 'T'} = turn-order tile)
     */
    void moveTotem(String nickname, char tileID);

    /**
     * Resolves a card-selection action for the given player.
     *
     * @param nickname the player performing the action
     * @param cardIDs  the list of card IDs the player wishes to acquire
     */
    void resolveActions(String nickname, List<String> cardIDs);

    /**
     * Starts the game FSM. Must be called exactly once after the model
     * object has been constructed and all observers have been registered.
     */
    void startFSM();
}
