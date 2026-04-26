package it.polimi.ingsw.am02.server.network;

import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import it.polimi.ingsw.am02.server.network.rmi.RmiServer;
import it.polimi.ingsw.am02.server.network.socket.SocketServer;

public class NetworkServerFactory {
    public static NetworkServer create(NetworkType type) {
        return switch (type) {
            case SOCKET -> new SocketServer();
            case RMI    -> new RmiServer();
        };
    }
}