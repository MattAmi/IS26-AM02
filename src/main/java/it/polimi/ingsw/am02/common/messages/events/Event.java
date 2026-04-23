package it.polimi.ingsw.am02.common.messages.events;

import it.polimi.ingsw.am02.common.messages.Message;

import it.polimi.ingsw.am02.common.messages.events.error.*;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;


public sealed interface Event extends Message
        permits ErrorEvent, BoardUpdatedEvent, CardTakenEvent, CurrentPlayerChangedEvent,
        EraChangedEvent, EventResolvedEvent, ExtraTurnEndedEvent, ExtraTurnStartedEvent,
        GameAbortedEvent, GameEndedEvent, GameSetupCompletedEvent, PhaseChangedEvent,
        PlayerDisconnectedEvent, PlayerLimitsInitializedEvent, PlayerLimitsUpdatedEvent,
        PlayerReconnectedEvent, PlayerResourceChangedEvent, TotemPlacedEvent, TotemReturnedEvent,
        TurnOrderEstablishedEvent, GameStartedEvent, LobbyDissolvedEvent, UpdatedLobbiesEvent,
        UpdatedLobbyEvent, UsernameResultEvent {
}