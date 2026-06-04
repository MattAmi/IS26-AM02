package it.polimi.ingsw.am02.common.messages.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.Message;
import it.polimi.ingsw.am02.common.messages.events.error.*;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;

/**
 * Sealed interface for all server-to-client events.
 * Each concrete record carries its payload as record components and
 * implements {@link #apply} to dispatch itself to the correct
 * {@link it.polimi.ingsw.am02.common.interfaces.VirtualView} method,
 * eliminating switch logic in client-side transport handlers.
 *
 * <p>Subtypes are grouped into {@link LobbyEvent}, {@link GameEvent},
 * {@link it.polimi.ingsw.am02.common.messages.events.error.ErrorEvent},
 * and {@link it.polimi.ingsw.am02.common.messages.events.game.PingEvent}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BoardUpdatedEvent.class,           name = "BoardUpdated"),
        @JsonSubTypes.Type(value = CardTakenEvent.class,              name = "CardTaken"),
        @JsonSubTypes.Type(value = CurrentPlayerChangedEvent.class,   name = "CurrentPlayerChanged"),
        @JsonSubTypes.Type(value = EraChangedEvent.class,             name = "EraChanged"),
        @JsonSubTypes.Type(value = ErrorEvent.class,                  name = "Error"),
        @JsonSubTypes.Type(value = EventResolvedEvent.class,          name = "EventResolved"),
        @JsonSubTypes.Type(value = ExtraTurnEndedEvent.class,         name = "ExtraTurnEnded"),
        @JsonSubTypes.Type(value = ExtraTurnStartedEvent.class,       name = "ExtraTurnStarted"),
        @JsonSubTypes.Type(value = GameEndedEvent.class,              name = "GameEnded"),
        @JsonSubTypes.Type(value = GameSetupCompletedEvent.class,     name = "GameSetupCompleted"),
        @JsonSubTypes.Type(value = GameStartedEvent.class,            name = "GameStarted"),
        @JsonSubTypes.Type(value = GlobalTimerStartedEvent.class,     name = "GlobalTimerStarted"),
        @JsonSubTypes.Type(value = GlobalTimerCancelledEvent.class,   name = "GlobalTimerCancelled"),
        @JsonSubTypes.Type(value = LobbyDissolvedEvent.class,         name = "LobbyDissolved"),
        @JsonSubTypes.Type(value = PhaseChangedEvent.class,           name = "PhaseChanged"),
        @JsonSubTypes.Type(value = PlayerDisconnectedEvent.class,     name = "PlayerDisconnected"),
        @JsonSubTypes.Type(value = PlayerReconnectedEvent.class,      name = "PlayerReconnected"),
        @JsonSubTypes.Type(value = PlayerLimitsInitializedEvent.class, name = "PlayerLimitsInitialized"),
        @JsonSubTypes.Type(value = PlayerLimitsUpdatedEvent.class,    name = "PlayerLimitsUpdated"),
        @JsonSubTypes.Type(value = PlayerResourceChangedEvent.class,  name = "PlayerResourceChanged"),
        @JsonSubTypes.Type(value = TotemPlacedEvent.class,            name = "TotemPlaced"),
        @JsonSubTypes.Type(value = TotemReturnedEvent.class,          name = "TotemReturned"),
        @JsonSubTypes.Type(value = TurnOrderEstablishedEvent.class,   name = "TurnOrderEstablished"),
        @JsonSubTypes.Type(value = UpdatedLobbiesEvent.class,         name = "UpdatedLobbies"),
        @JsonSubTypes.Type(value = UpdatedLobbyEvent.class,           name = "UpdatedLobby"),
        @JsonSubTypes.Type(value = UsernameResultEvent.class,         name = "UsernameResult"),
        @JsonSubTypes.Type(value = PingEvent.class,                   name = "Ping"),
        @JsonSubTypes.Type(value = GameAbortedEvent.class,            name = "GameAborted"),
        @JsonSubTypes.Type(value = GameRecoveryFailedEvent.class,     name = "GameRecoveryFailed"),
        @JsonSubTypes.Type(value = AutoPlayerTimerStartedEvent.class, name = "AutoPlayerTimerStarted"),
        @JsonSubTypes.Type(value = AutoPlayerInvokedEvent.class,      name = "AutoPlayerInvoked")
})
public sealed interface Event extends Message
        permits LobbyEvent, GameEvent, ErrorEvent, PingEvent {

    /**
     * Applies this event to the given view, calling the appropriate
     * {@link it.polimi.ingsw.am02.common.interfaces.VirtualView} notification method.
     *
     * @param view the client-side view to notify
     */
    void apply(VirtualView view);
}