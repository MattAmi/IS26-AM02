package it.polimi.ingsw.am02.common.messages.events.lobby;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.messages.events.Event;
import java.util.List;

public record UpdatedLobbiesEvent(List<LobbyInfo> lobbies) implements Event {}