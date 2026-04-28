package it.polimi.ingsw.am02.server.network.rmi;

import it.polimi.ingsw.am02.common.enumerations.Totem;
import it.polimi.ingsw.am02.common.messages.commands.MoveTotemCommand;
import it.polimi.ingsw.am02.common.messages.commands.ReconnectCommand;
import it.polimi.ingsw.am02.common.messages.commands.ResolveActionsCommand;
import it.polimi.ingsw.am02.common.messages.commands.StartGameCommand;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.lobby.UsernameResultEvent;
import it.polimi.ingsw.am02.common.network.rmi.RmiClientRemote;
import it.polimi.ingsw.am02.common.network.rmi.RmiServerRemote;
import it.polimi.ingsw.am02.server.controller.ControllerManager;
import it.polimi.ingsw.am02.server.network.ClientHandler;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.*;

public class RmiClientHandler implements RmiServerRemote, ClientHandler {

    private final ControllerManager manager;
    private final RmiClientRemote clientRemoteStub;
    private final String clientId;
    private String myNickname = null;

    // --- ASYNCHRONY COMPONENTS ---
    // Inbound: Thread pool for incoming client commands (prevents blocking RMI threads)
    private final ExecutorService inboundExecutor = Executors.newSingleThreadExecutor();

    // Outbound: Queue and Worker for outgoing server events (prevents Tactical Freeze)
    private final BlockingQueue<Event> outboundQueue = new LinkedBlockingQueue<>();
    private final Thread outboundWorker;

    private volatile boolean running = true;

    public RmiClientHandler(RmiClientRemote clientRemoteStub) {
        this.manager = ControllerManager.getInstance();
        this.clientRemoteStub = clientRemoteStub;

        // Register connection and get the unique session ID immediately
        this.clientId = manager.handleClientConnected(this);

        // Start the outbound worker thread (The "Postman")
        this.outboundWorker = new Thread(this::processOutboundQueue);
        this.outboundWorker.start();
    }

    // ==========================================================
    // VIRTUAL VIEW (Called by Server Controller -> Outbound)
    // ==========================================================

    @Override
    public void notify(Event event) {
        if (!running) return;

        // Cattura il nickname dal server quando il login ha successo!
        if (event instanceof UsernameResultEvent e && e.isValid()) {
            this.myNickname = e.username();
        }

        outboundQueue.offer(event);
    }

    @Override
    public void disconnect() {
        this.running = false;
        this.inboundExecutor.shutdownNow();
        this.outboundWorker.interrupt();
        manager.handleDisconnection(this.clientId);
    }

    private void processOutboundQueue() {
        while (running) {
            try {
                // Wait for an event, but wake up every 2 seconds if queue is empty
                Event event = outboundQueue.poll(2, TimeUnit.SECONDS);

                if (event != null) {
                    clientRemoteStub.notifyEvent(event);
                } else {
                    // 2 seconds passed with no traffic. Ping the client!
                    clientRemoteStub.ping();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RemoteException ex) {
                System.err.println("[RMI Handler] Client " + clientId + " is DEAD. Disconnecting.");
                disconnect(); // Instantly removes the player and triggers AutoPlayer logic
                break;
            }
        }
    }

    // ==========================================================
    // RMI SERVER REMOTE (Called by Client Proxy -> Inbound)
    // ==========================================================

    @Override
    public void requestSetUsername(String username) throws RemoteException {
        inboundExecutor.submit(() -> manager.requestSetUsernameInLobby(clientId, username));
    }

    @Override
    public void requestCreateLobby(int numPlayers) throws RemoteException {
        inboundExecutor.submit(() -> manager.createLobby(clientId, numPlayers));
    }

    @Override
    public void requestJoinLobby(String lobbyID) throws RemoteException {
        inboundExecutor.submit(() -> manager.joinLobby(clientId, lobbyID));
    }

    @Override
    public void requestSelectTotem(Totem color) throws RemoteException {
        inboundExecutor.submit(() -> manager.selectTotem(clientId, color));
    }

    @Override
    public void requestStartGame() throws RemoteException {
        inboundExecutor.submit(() -> manager.routeGameCommand(clientId, new StartGameCommand()));
    }

    @Override
    public void requestLeaveLobby() throws RemoteException {
        inboundExecutor.submit(() -> manager.leaveLobby(clientId));
    }

    @Override
    public void moveTotem(char tileID) throws RemoteException {
        // Usa myNickname invece di ""
        inboundExecutor.submit(() -> manager.routeGameCommand(clientId, new MoveTotemCommand(myNickname, tileID)));
    }

    @Override
    public void resolveActions(List<String> selectedIDs) throws RemoteException {
        // Usa myNickname invece di ""
        inboundExecutor.submit(() -> manager.routeGameCommand(clientId, new ResolveActionsCommand(myNickname, selectedIDs)));
    }

    @Override
    public void requestReconnect(String nickname, String gameId) throws RemoteException {
        inboundExecutor.submit(() ->
                manager.handleReconnectRequest(this.clientId, this, new ReconnectCommand(gameId, nickname)) // Matteo's command might still take (gameId, nickname) internally, check his class if needed, but this aligns the RMI call.
        );
    }
}