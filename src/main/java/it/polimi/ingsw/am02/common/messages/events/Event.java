package it.polimi.ingsw.am02.common.messages.events;

import it.polimi.ingsw.am02.common.messages.Message;

import it.polimi.ingsw.am02.common.messages.events.error.*;
import it.polimi.ingsw.am02.common.messages.events.game.*;
import it.polimi.ingsw.am02.common.messages.events.lobby.*;


public sealed interface Event extends Message
        permits BoardUpdatedEvent, CardTakenEvent, CurrentPlayerChangedEvent, EraChangedEvent,
        ErrorEvent, EventResolvedEvent, ExtraTurnEndedEvent, ExtraTurnStartedEvent,
        GameEndedEvent, GameSetupCompletedEvent,
        GameStartedEvent, LobbyDissolvedEvent, PhaseChangedEvent, PlayerDisconnectedEvent,
        PlayerLimitsInitializedEvent, PlayerLimitsUpdatedEvent, PlayerResourceChangedEvent
        , TotemPlacedEvent, TotemReturnedEvent,
        TurnOrderEstablishedEvent, UpdatedLobbiesEvent, UpdatedLobbyEvent,
        UsernameResultEvent {
}