package it.polimi.ingsw.am02.server.network;

import it.polimi.ingsw.am02.common.interfaces.VirtualView;


public interface ClientHandler extends VirtualView {
    void disconnect();
}