package it.polimi.ingsw.am02.server.network.rmi;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;
import it.polimi.ingsw.am02.common.messages.commands.*;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.lobby.UsernameResultEvent;
import it.polimi.ingsw.am02.common.network.rmi.RmiClientRemote;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerRemote;
import it.polimi.ingsw.am02.server.controller.ControllerManager;

import java.rmi.RemoteException;
import java.util.List;

public class RmiClientHandler implements RmiServerRemote, VirtualView {

    private final ControllerManager manager;
    private final RmiClientRemote clientRemoteStub;
    private String myNickname;

    public RmiClientHandler(RmiClientRemote clientRemoteStub) {
        this.manager = ControllerManager.getInstance();
        this.clientRemoteStub = clientRemoteStub;
    }

    @Override
    public void notify(Event event) {
        if (event instanceof UsernameResultEvent e && e.isValid()) {
            this.myNickname = e.username();
            System.out.println("RMI Handler associated with client: " + myNickname);
        }
        try {
            clientRemoteStub.notifyEvent(event);
        } catch (RemoteException ex) {
            System.err.println("Unable to reach the client. Notifying disconnection.");
            if (myNickname != null) {
                manager.handleDisconnection(myNickname);
            }
        }
    }

    @Override
    public void requestSetUsername(String username) throws RemoteException {
        manager.requestSetUsername(username, this);
    }

    @Override
    public void requestCreateLobby(int numPlayers) throws RemoteException {
        if (myNickname == null) return;
        manager.createLobby(numPlayers, myNickname, this);
    }

    @Override
    public void requestJoinLobby(String lobbyID) throws RemoteException {
        if (myNickname == null) return;
        manager.joinLobby(lobbyID, myNickname, this);
    }

    @Override
    public void requestSelectTotem(Totem color) throws RemoteException {
        if (myNickname == null) return;
        manager.selectTotem(myNickname, color);
    }

    @Override
    public void requestStartGame() throws RemoteException {
        if (myNickname == null) return;
        manager.routeGameCommand(myNickname, new StartGameCommand());
    }

    @Override
    public void requestLeaveLobby() throws RemoteException {
        if (myNickname == null) return;
        manager.leaveLobby(myNickname);
    }

    @Override
    public void moveTotem(String nickname, char tileID) throws RemoteException {
        if (myNickname == null) return;
        manager.routeGameCommand(myNickname, new MoveTotemCommand(myNickname, tileID));
    }

    @Override
    public void resolveActions(String nickname, List<String> selectedIDs) throws RemoteException {
        if (myNickname == null) return;
        manager.routeGameCommand(myNickname, new ResolveActionsCommand(myNickname, selectedIDs));
    }
}