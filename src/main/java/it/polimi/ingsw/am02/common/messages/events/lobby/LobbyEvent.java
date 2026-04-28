package it.polimi.ingsw.am02.common.messages.events.lobby;

import it.polimi.ingsw.am02.common.messages.events.Event;

public sealed interface LobbyEvent extends Event
        permits UsernameResultEvent, UpdatedLobbiesEvent, UpdatedLobbyEvent,
        LobbyDissolvedEvent, GameStartedEvent {}