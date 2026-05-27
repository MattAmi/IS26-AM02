package it.polimi.ingsw.am02.server.model.listeners;

/**
 * Subscription interface for managing {@link GameObserver} registrations.
 * Implemented by {@link GameNotifier} alongside {@link GameEventEmitter}.
 *
 * <p>Separating subscription ({@code GameObserverRegistry}) from emission
 * ({@link GameEventEmitter}) prevents model classes from accidentally
 * adding or removing observers.
 */
public interface GameObserverRegistry {

    /**
     * Registers a {@link GameObserver} to receive future game-level notifications.
     *
     * @param observer the observer to add
     */
    void addObserver(GameObserver observer);

    /**
     * Unregisters a previously added {@link GameObserver}.
     * No-op if the observer is not currently registered.
     *
     * @param observer the observer to remove
     */
    void removeObserver(GameObserver observer);
}
