package it.polimi.ingsw.am02.client.network;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.common.interfaces.VirtualServer;

public interface ServerProxy extends VirtualServer {
    void connect() throws Exception;
    void disconnect();
    boolean isConnected();
    void setClientController(ClientController controller); // ← aggiungi
}