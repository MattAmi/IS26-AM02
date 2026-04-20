package it.polimi.ingsw.am02.client.view.tui;

import it.polimi.ingsw.am02.client.model.ClientModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.Scanner;

public class TuiController {
    private final ServerProxy proxy;
    private final ClientModel model;
    private final TuiView view;

    public TuiController(ServerProxy proxy, ClientModel model, TuiView view) {
        this.proxy = proxy;
        this.model = model;
        this.view = view;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] args = input.split("\\s+");
            String cmd = args[0].toLowerCase();

            try {
                switch (cmd) {
                    case "login" -> {
                        if (args.length < 2) view.displayError("Usage: login <nickname>");
                        else proxy.requestSetUsername(args[1]);
                    }
                    case "create" -> {
                        if (args.length < 2) view.displayError("Usage: create <expected_players>");
                        else proxy.requestCreateLobby(Integer.parseInt(args[1]));
                    }
                    case "join" -> {
                        if (args.length < 2) view.displayError("Usage: join <lobby_index>");
                        else {
                            int idx = Integer.parseInt(args[1]);
                            String lobbyId = model.getAvailableLobbies().get(idx).lobbyId();
                            proxy.requestJoinLobby(lobbyId);
                        }
                    }
                    case "totem" -> {
                        if (args.length < 2) view.displayError("Usage: totem <color>");
                        else proxy.requestSelectTotem(Totem.valueOf(args[1].toUpperCase()));
                    }
                    case "start" -> proxy.requestStartGame();
                    case "leave" -> proxy.requestLeaveLobby();
                    case "quit" -> {
                        proxy.disconnect();
                        System.exit(0);
                    }
                    default -> view.displayError("Unknown command.");
                }
            } catch (IndexOutOfBoundsException e) {
                view.displayError("Invalid index selected.");
            } catch (IllegalArgumentException e) {
                view.displayError("Invalid argument format.");
            } catch (Exception e) {
                view.displayError("System error: " + e.getMessage());
            }
        }
    }
}