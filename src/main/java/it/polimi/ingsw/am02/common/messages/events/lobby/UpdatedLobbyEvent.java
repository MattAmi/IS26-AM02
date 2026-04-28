package it.polimi.ingsw.am02.common.messages.events.lobby;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.messages.events.Event;

public record UpdatedLobbyEvent(LobbyInfo lobby) implements Event, LobbyEvent {}