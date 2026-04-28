package it.polimi.ingsw.am02.client.network.rmi;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.tui.TuiView; // Import specifico
import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.game.GameEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.GameStartedEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.LobbyEvent;
import it.polimi.ingsw.am02.common.network.rmi.RmiClientRemote;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerFactory;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerRemote;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RmiServerProxy extends UnicastRemoteObject implements ServerProxy, RmiClientRemote {
    private final String host;
    private final int port;
    private final LobbyModel lobbyModel;
    private final ClientView clientView;
    private GameModel gameModel;
    private RmiServerRemote serverStub;
    private boolean connected = false;

    public RmiServerProxy(String host, int port, LobbyModel lobbyModel, ClientView clientView) throws RemoteException {
        super();
        this.host = host;
        this.port = port;
        this.lobbyModel = lobbyModel;
        this.clientView = clientView;
    }

    @Override
    public void connect() throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        RmiServerFactory factory = (RmiServerFactory) registry.lookup("AM02-GameServer");
        this.serverStub = factory.registerClient(this);
        this.connected = true;
    }

    @Override
    public void disconnect() {
        this.connected = false;
        try { UnicastRemoteObject.unexportObject(this, true); } catch (Exception ignored) {}
    }

    @Override public boolean isConnected() { return connected; }
    @Override public void ping() throws RemoteException {}

    @Override
    public void notifyEvent(Event event) throws RemoteException {
        // --- 1. GESTIONE EVENTI LOBBY ---
        if (event instanceof LobbyEvent lobbyEvent) {
            lobbyModel.apply(lobbyEvent);

            // Se la partita inizia normalmente, creiamo il GameModel
            if (event instanceof GameStartedEvent e && gameModel == null) {
                initGameModel(lobbyModel.getMyNickname());
                gameModel.apply(e); // Necessario per settare il GameID nel model
            }
        }

        // --- 2. GESTIONE EVENTI PARTITA (GameEvent) ---
        else if (event instanceof GameEvent gameEvent) {
            if (gameModel == null) {
                /* * CASO RICONNESSIONE: Se riceviamo un GameEvent ma il gameModel è null,
                 * significa che siamo appena rientrati in una partita in corso.
                 * Dobbiamo inizializzare il modello immediatamente per non perdere l'evento.
                 */
                String myNick = lobbyModel.getMyNickname();

                // Nota: Se lobbyModel.getMyNickname() fosse null (perché il client è appena rinato),
                // dovresti usare una variabile 'activeNickname' salvata nel Proxy durante la richiesta di reconnect.
                initGameModel(myNick);

                System.out.println("[RMI Proxy] GameModel inizializzato durante la riconnessione per: " + myNick);
            }

            // Inoltriamo l'evento al GameModel che aggiornerà i dati e notificherà la TUI
            gameModel.apply(gameEvent);
        }
    }

    private void initGameModel(String nickname) {
        this.gameModel = new GameModel(nickname);

        // INTEGRAZIONE TUI BRO:
        if (clientView instanceof TuiView tui) {
            tui.onGameModelCreated(this.gameModel);
        }

        // Notifichiamo anche il controller se necessario
        this.clientView.setGameModel(this.gameModel);
    }

    // --- Metodi di invio (Outbound) ---
    @Override public void requestSetUsername(String u) { try { serverStub.requestSetUsername(u); } catch (RemoteException e) { connected = false; } }
    @Override public void requestCreateLobby(int n) { try { serverStub.requestCreateLobby(n); } catch (RemoteException e) { connected = false; } }
    @Override public void requestJoinLobby(String id) { try { serverStub.requestJoinLobby(id); } catch (RemoteException e) { connected = false; } }
    @Override public void requestSelectTotem(Totem t) { try { serverStub.requestSelectTotem(t); } catch (RemoteException e) { connected = false; } }

    @Override
    public void requestStartGame() {

    }

    @Override public void requestLeaveLobby() { try { serverStub.requestLeaveLobby(); } catch (RemoteException e) { connected = false; } }
    @Override public void moveTotem(char t) { try { serverStub.moveTotem(t); } catch (RemoteException e) { connected = false; } }
    @Override public void resolveActions(List<String> ids) { try { serverStub.resolveActions(ids); } catch (RemoteException e) { connected = false; } }
    @Override public void requestReconnect(String n, String g) { try { serverStub.requestReconnect(n, g); } catch (RemoteException e) { connected = false; } }
}