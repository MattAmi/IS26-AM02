package it.polimi.ingsw.am02.client.controller;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Reads keyboard input from the user and translates it into commands
 * sent to the server via {@link ServerProxy}. Reads {@link LobbyModel}
 * and {@link GameModel} only to retrieve contextual data needed to build
 * the command (e.g. resolving a lobby index to a lobby ID).
 *
 * <p>This class is TUI-specific: it owns a blocking stdin loop.
 * In the GUI, button click handlers invoke {@link ServerProxy} methods
 * directly and this class is not instantiated.</p>
 */
public class ClientController {

    private final ServerProxy proxy;
    private final LobbyModel lobbyModel;
    private GameModel gameModel; // null until game starts
    private final ClientView view;

    public ClientController(ServerProxy proxy, LobbyModel lobbyModel, ClientView view) {
        this.proxy = proxy;
        this.lobbyModel = lobbyModel;
        this.view = view;
    }

    /** Called by ServerProxy when GameModel is created (on GameStartedEvent). */
    public void setGameModel(GameModel gameModel) {
        this.gameModel = gameModel;
    }
    /** Blocking loop: reads commands from stdin until the process exits. */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            String[] args = input.split("\\s+");
            String cmd = args[0].toLowerCase();
            try {
                dispatch(cmd, args);
            } catch (IndexOutOfBoundsException ex) {
                view.onError("Invalid index.");
            } catch (IllegalArgumentException ex) {
                view.onError("Invalid argument: " + ex.getMessage());
            } catch (Exception ex) {
                view.onError("System error: " + ex.getMessage());
            }
        }
    }

    private void dispatch(String cmd, String[] args) {
        // Extract arguments safely to avoid local IndexOutOfBoundsException.
        // If missing, we pass empty/dummy values to let the SERVER reject them!
        String arg1 = args.length > 1 ? args[1] : "";
        String arg2 = args.length > 2 ? args[2] : "";

        switch (cmd) {
            case "nick" -> proxy.requestSetUsername(arg1);
            case "create" -> {
                int size = arg1.matches("\\d+") ? Integer.parseInt(arg1) : 0; // Server will reject 0
                proxy.requestCreateLobby(size);
            }
            case "join" -> {
                try {
                    int idx = Integer.parseInt(arg1);
                    String lobbyId = lobbyModel.getAvailableLobbies().get(idx).lobbyId();
                    proxy.requestJoinLobby(lobbyId);
                } catch (Exception e) {
                    view.onError("Client syntax error. Use: join <index number>");
                }
            }
            case "reconnect" -> proxy.requestReconnect(arg1, arg2);
            case "totem" -> {
                try {
                    proxy.requestSelectTotem(Totem.valueOf(arg1.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    view.onError("Client syntax error. Valid totems: PURPLE, WHITE, etc.");
                }
            }
            case "leave" -> proxy.requestLeaveLobby();
            case "move" -> {
                char tile = arg1.isEmpty() ? ' ' : arg1.charAt(0);
                proxy.moveTotem(tile); // Send to server, let it evaluate!
            }
            case "resolve" -> {
                List<String> ids = args.length > 1 ?
                        new java.util.ArrayList<>(java.util.Arrays.asList(args).subList(1, args.length)) :
                        new java.util.ArrayList<>();
                proxy.resolveActions(ids); // Send empty list to server, let it evaluate!
            }
            case "quit" -> {
                proxy.disconnect();
                System.exit(0);
            }
            default -> proxy.resolveActions(List.of(cmd)); // Throw unrecognized commands to the server!
        }
    }
}