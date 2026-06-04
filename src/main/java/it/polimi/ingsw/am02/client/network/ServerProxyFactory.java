package it.polimi.ingsw.am02.client.network;

import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy;
import it.polimi.ingsw.am02.client.network.socket.SocketServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;

/**
 * Factory for creating {@link ServerProxy} instances.
 *
 * <p>Selects the concrete implementation — RMI or Socket — based on the
 * {@link NetworkType} chosen by the player at startup, shielding the rest
 * of the client code from the construction details of each proxy.
 *
 * @see it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy
 * @see it.polimi.ingsw.am02.client.network.socket.SocketServerProxy
 */
public class ServerProxyFactory {

    /** Not instantiable — utility class. */
    private ServerProxyFactory() {}

    /**
     * Creates and returns an unconnected {@link ServerProxy} for the requested
     * network technology.
     *
     * <p>The returned proxy must be wired ({@link ServerProxy#setClientController})
     * and then started ({@link ServerProxy#connect()}) before use.
     *
     * @param type       the network technology ({@link NetworkType#RMI} or
     *                   {@link NetworkType#SOCKET})
     * @param host       the server hostname or IP address
     * @param port       the server port number
     * @param lobbyModel the shared lobby model that receives pre-game notifications
     * @param view       the client view used to report connection-state changes
     * @return a new, unconnected {@link ServerProxy}
     * @throws Exception if RMI proxy construction fails (e.g. {@link java.rmi.RemoteException})
     */
    public static ServerProxy create(NetworkType type, String host, int port,
                                     LobbyModel lobbyModel, ClientView view) throws Exception {
        return switch (type) {
            case SOCKET -> new SocketServerProxy(host, port, lobbyModel, view);
            case RMI    -> new RmiServerProxy(host, port, lobbyModel, view);
        };
    }
}