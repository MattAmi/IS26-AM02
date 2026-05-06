package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.common.messages.events.Event;

public sealed interface GameEvent extends Event
        permits AutoPlayerInvokedEvent, AutoPlayerTimerStartedEvent, BoardUpdatedEvent, CardTakenEvent,
        CurrentPlayerChangedEvent, EraChangedEvent, EventResolvedEvent, ExtraTurnEndedEvent, ExtraTurnStartedEvent,
        GameAbortedEvent, GameEndedEvent, GameRecoveryFailedEvent, GameSetupCompletedEvent, PhaseChangedEvent,
        PlayerDisconnectedEvent, PlayerLimitsInitializedEvent, PlayerLimitsUpdatedEvent, PlayerReconnectedEvent,
        PlayerResourceChangedEvent, TotemPlacedEvent, TotemReturnedEvent, TurnOrderEstablishedEvent {

    default void applyTo(GameModel gameModel) {};
}