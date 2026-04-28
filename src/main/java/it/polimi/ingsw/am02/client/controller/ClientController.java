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
        switch (cmd) {
            case "login" -> {
                if (args.length < 2) view.onError("Usage: login <nickname>");
                else proxy.requestSetUsername(args[1]);
            }
            case "create" -> {
                if (args.length < 2) view.onError("Usage: create <expected_players>");
                else proxy.requestCreateLobby(Integer.parseInt(args[1]));
            }
            case "join" -> {
                if (args.length < 2) view.onError("Usage: join <lobby_index>");
                else {
                    int idx = Integer.parseInt(args[1]);
                    // reads from LobbyModel, not GameModel
                    String lobbyId = lobbyModel.getAvailableLobbies().get(idx).lobbyId();
                    proxy.requestJoinLobby(lobbyId);
                }
            }
            case "totem" -> {
                if (args.length < 2) view.onError("Usage: totem <color>");
                else proxy.requestSelectTotem(Totem.valueOf(args[1].toUpperCase()));
            }
            case "leave" -> proxy.requestLeaveLobby();
            case "move" -> {
                if (args.length < 2) view.onError("Usage: move <tileID> (e.g. move B)");
                else proxy.moveTotem(args[1].charAt(0));
            }
            case "resolve" -> {
                if (args.length < 2) view.onError("Usage: resolve <id1> [id2 ...]");
                else {
                    List<String> ids = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
                    proxy.resolveActions(ids);
                }
            }
            case "quit" -> {
                proxy.disconnect();
                System.exit(0);
            }
            default -> view.onError("Unknown command: " + cmd);
        }
    }
}