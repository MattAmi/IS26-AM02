package it.polimi.ingsw.am02.common.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

// È la "Reception" del Server
public interface RmiServerFactory extends Remote {

    RmiServerRemote registerClient(RmiClientRemote clientCallback) throws RemoteException;

}