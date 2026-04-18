package it.polimi.ingsw.am02.common.messages.events;

import it.polimi.ingsw.am02.common.messages.Message;
// imports dei record concreti...

/**
 * Marker for all server → client messages. Every concrete event corresponds
 * to exactly one notification method of {@code GameObserver} (plus lobby events
 * and the generic {@link ErrorEvent}).
 *
 * <p>Events describe a delta of the game state. The client applies each event
 * to its local replica of the model.
 */
public sealed interface Event extends Message
        permits UsernameResultEvent, UpdatedLobbiesEvent, UpdatedLobbyEvent,
        LobbyDissolvedEvent, GameStartedEvent,
        GameSetupCompletedEvent, PhaseChangedEvent,
        TotemPlacedEvent, CurrentPlayerChangedEvent,
        PlayerLimitsInitializedEvent, PlayerLimitsUpdatedEvent,
        CardTakenEvent, TotemReturnedEvent, PlayerResourceChangedEvent,
        BoardUpdatedEvent, TurnOrderEstablishedEvent,
        EraChangedEvent, EventResolvedEvent, SustainmentResolvedEvent,
        ExtraTurnStartedEvent, ExtraTurnEndedEvent,
        FinalScoreCalculatedEvent, GameEndedEvent,
        ErrorEvent {
}
