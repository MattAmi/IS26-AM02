package it.polimi.ingsw.am02.common;

/** Default network endpoints shared by client and server. */
public final class NetworkDefaults {

    private NetworkDefaults() {}

    public static final String RMI_REGISTRY_NAME = "AM02-GameServer";
    public static final int DEFAULT_RMI_PORT    = 1099;
    public static final int DEFAULT_SOCKET_PORT = 1100;
}