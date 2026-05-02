package it.polimi.ingsw.am02.client.controller;

import it.polimi.ingsw.am02.client.model.GameModel;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.client.view.tui.TuiView;
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

    /**
     * Called by the network proxy when the server confirms the game has started.
     * Creates the GameModel, wires it to the view, and stores it locally.
     *
     * @param nickname the local player's confirmed nickname
     */
    public void onGameModelRequired(String nickname) {
        if (this.gameModel != null) return; // già inizializzato, ignora
        resetGameModel(nickname);
    }

    public void resetGameModel(String nickname) {
        GameModel model = new GameModel(nickname);
        this.gameModel = model;
        this.view.setGameModel(model);
        model.addObserver(this.view);
    }

    /**
     * Returns the current GameModel, or null if the game has not started yet.
     */
    public GameModel getGameModel() {
        return gameModel;
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
        String arg1 = args.length > 1 ? args[1] : "";
        String arg2 = args.length > 2 ? args[2] : "";

        // Determiniamo in quale "schermata" si trova l'utente
        boolean inGame = (gameModel != null);
        boolean inLobby = (!inGame && lobbyModel.getCurrentLobby() != null);
        boolean inPreLobby = (!inGame && lobbyModel.getCurrentLobby() == null);

        switch (cmd) {
            case "nick" -> {
                if (inGame) view.onError("Command 'nick' is not allowed during a game.");
                else proxy.requestSetUsername(arg1);
            }

            case "create" -> {
                if (!inPreLobby) view.onError("You can only create a lobby from the main menu.");
                else {
                    int size = arg1.matches("\\d+") ? Integer.parseInt(arg1) : 0;
                    proxy.requestCreateLobby(size);
                }
            }

            case "join" -> {
                if (!inPreLobby) view.onError("You can only join a lobby from the main menu.");
                else {
                    try {
                        int idx = Integer.parseInt(arg1);
                        String lobbyId = lobbyModel.getAvailableLobbies().get(idx).lobbyId();
                        proxy.requestJoinLobby(lobbyId);
                    } catch (Exception e) {
                        view.onError("Client syntax error. Use: join <index number>");
                    }
                }
            }

            case "reconnect" -> {
                if (inGame) view.onError("You are already in a game.");
                else proxy.requestReconnect(arg1, arg2);
            }

            // ... dentro ClientController.java -> dispatch() ...

            case "totem" -> {
                if (lobbyModel.getCurrentLobby() == null || inGame) {
                    view.onError("You can only select a totem while waiting in a lobby.");
                } else if (arg1.isEmpty()) {
                    view.onError("Client syntax error. Use: totem <color> (e.g., totem WHITE)");
                } else {
                    try {
                        Totem selected = Totem.valueOf(arg1.toUpperCase());
                        proxy.requestSelectTotem(selected);
                    } catch (IllegalArgumentException e) {
                        view.onError("Invalid totem color: '" + arg1 + "'. Type 'totems' to see available colors.");
                    }
                }
            }

            case "totems" -> {
                if (lobbyModel.getCurrentLobby() == null) {
                    view.onError("You can only check available totems while in a lobby.");
                } else if (this.view instanceof TuiView tuiView) {
                    tuiView.onShowAvailableTotems();
                }
            }

            case "leave" -> {
                if (!inLobby) view.onError("You are not currently in a lobby.");
                else proxy.requestLeaveLobby();
            }

            case "move" -> {
                if (!inGame) view.onError("Command 'move' is only available during a game.");
                else {
                    char tile = arg1.isEmpty() ? ' ' : arg1.charAt(0);
                    proxy.moveTotem(tile);
                }
            }

            case "resolve" -> {
                if (!inGame) view.onError("Command 'resolve' is only available during a game.");
                else {
                    List<String> ids = args.length > 1 ?
                            new java.util.ArrayList<>(java.util.Arrays.asList(args).subList(1, args.length)) :
                            new java.util.ArrayList<>();
                    proxy.resolveActions(ids);
                }
            }

            case "info" -> {
                if (!inGame) {
                    view.onError("Command 'info' is only available during a game.");
                } else if (arg1.isEmpty()) {
                    view.onError("Client syntax error. Use: info <cardID> (e.g., info C_008)");
                } else {
                    // Call the specific TUI view method if the view supports it.
                    // We check if the view is a TuiView to avoid breaking other UI implementations.
                    if (this.view instanceof TuiView tuiView) {
                        tuiView.onShowCardInfo(arg1);
                    } else {
                        // For GUI or other views where this command might not be supported via CLI
                        view.onError("The 'info' command is not supported in this view mode.");
                    }
                }
            }

            case "lobby" -> {
                // Questo mantiene tutta la logica avanzata che avevamo costruito!
                if (gameModel != null) {
                    if (!gameModel.isGameEnded()) {
                        view.onError("You cannot return to lobby while a game is in progress.");
                    } else {
                        try {
                            proxy.disconnect();
                            proxy.connect();
                            this.gameModel = null;
                            view.onReturnToLobby();
                        } catch (Exception e) {
                            view.onError("Return to lobby failed: Server is unreachable.");
                        }
                    }
                } else {
                    if (lobbyModel.getCurrentLobby() != null) {
                        proxy.requestLeaveLobby();
                    } else {
                        view.onReturnToLobby();
                    }
                }
            }

            case "help" -> {
                if (this.view instanceof TuiView tuiView) {
                    tuiView.onShowHelp(inPreLobby, inLobby, inGame);
                } else {
                    view.onError("Help command not supported in this view.");
                }
            }

            case "quit" -> {
                proxy.disconnect();
                System.exit(0);
            }

            default -> {
                // Se l'utente digita una stringa a caso (es. "CIAO"):
                // Se è in gioco, usiamo il tuo fallback originale (lo tratta come una carta da risolvere).
                // Se non è in gioco, blocchiamolo per non spammarlo al server.
                if (inGame) {
                    proxy.resolveActions(List.of(cmd));
                } else {
                    view.onError("Unrecognized command: '" + cmd + "'. Check the available commands.");
                }
            }
        }
    }
}