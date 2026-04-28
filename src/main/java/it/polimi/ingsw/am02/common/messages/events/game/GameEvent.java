package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.messages.events.Event;

public sealed interface GameEvent extends Event
        permits BoardUpdatedEvent, CardTakenEvent, CurrentPlayerChangedEvent, EraChangedEvent, EventResolvedEvent, ExtraTurnEndedEvent, ExtraTurnStartedEvent, GameAbortedEvent, GameEndedEvent, GameSetupCompletedEvent, PhaseChangedEvent, PlayerDisconnectedEvent, PlayerLimitsInitializedEvent, PlayerLimitsUpdatedEvent, PlayerReconnectedEvent, PlayerResourceChangedEvent, TotemPlacedEvent, TotemReturnedEvent, TurnOrderEstablishedEvent {}