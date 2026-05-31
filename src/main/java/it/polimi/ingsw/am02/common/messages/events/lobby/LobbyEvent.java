package it.polimi.ingsw.am02.common.messages.events.lobby;

import it.polimi.ingsw.am02.common.messages.events.Event;

/**
 * Marker interface for events related to the pre-game lobby phase.
 */
public sealed interface LobbyEvent extends Event
        permits GameStartedEvent, LobbyDissolvedEvent, UpdatedLobbiesEvent, UpdatedLobbyEvent, UsernameResultEvent {}