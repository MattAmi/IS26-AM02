package it.polimi.ingsw.am02.common.network.rmi;

import it.polimi.ingsw.am02.common.messages.events.Event;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiClientRemote extends Remote {
    void notifyEvent(Event event) throws RemoteException;

    // Used by the server to check if the client is still alive
    void ping() throws RemoteException;
}