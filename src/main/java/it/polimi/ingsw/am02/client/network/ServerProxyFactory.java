package it.polimi.ingsw.am02.client.network;

import it.polimi.ingsw.am02.client.model.ClientModel;
import it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy;
import it.polimi.ingsw.am02.client.network.socket.SocketServerProxy;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;

public class ServerProxyFactory {
    public static ServerProxy create(NetworkType type, String host, int port, ClientModel clientModel) throws Exception {
        return switch (type) {
            case SOCKET -> new SocketServerProxy(host, port, clientModel);
            case RMI    -> new RmiServerProxy(host, port, clientModel);
        };
    }
}