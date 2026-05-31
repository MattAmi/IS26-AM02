package it.polimi.ingsw.am02.common.enumerations;

/**
 * Enumerates the two supported network transport protocols.
 * Selected by the client at startup and used by
 * {@link it.polimi.ingsw.am02.client.network.ServerProxyFactory}
 * to instantiate the appropriate
 * {@link it.polimi.ingsw.am02.client.network.ServerProxy} implementation.
 *
 * <ul>
 *   <li>{@link #SOCKET} — JSON-over-TCP transport ({@code SocketServerProxy})</li>
 *   <li>{@link #RMI}    — Java RMI transport ({@code RmiServerProxy})</li>
 * </ul>
 */
public enum NetworkType {
    SOCKET,
    RMI
}