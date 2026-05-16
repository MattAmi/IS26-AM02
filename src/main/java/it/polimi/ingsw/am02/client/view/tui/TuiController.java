package it.polimi.ingsw.am02.client.view.tui;

import it.polimi.ingsw.am02.client.controller.ClientController;
import it.polimi.ingsw.am02.client.model.LobbyModel;
import it.polimi.ingsw.am02.client.network.ServerProxy;
import it.polimi.ingsw.am02.client.network.ServerProxyFactory;
import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.enumerations.NetworkType;
import it.polimi.ingsw.am02.common.enumerations.Totem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * TUI-specific controller.
 * Since this controller is created specifically for the TUI, it holds a
 * direct reference to TuiView to avoid continuous casting.
 */
public class TuiController extends ClientController {

    private final TuiView tuiView; // Specific reference to the TUI implementation
    private final NetworkType networkType;  // aggiunto
    private final String host;              // aggiunto
    private final int port;                 // aggiunto


    public TuiController(ServerProxy proxy, LobbyModel lobbyModel, ClientView view,
                         NetworkType networkType, String host, int port) {
        super(proxy, lobbyModel, view);
        this.tuiView = (TuiView) view;
        this.networkType = networkType;
        this.host = host;
        this.port = port;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;
                dispatch(input);
            } catch (Exception e) {
                view.onError("Fatal system error: " + e.getMessage());
            }
        }
    }

    private void dispatch(String input) {
        String[] args = input.split("\\s+");
        String cmd = args[0].toLowerCase();
        String arg1 = args.length > 1 ? args[1] : "";

        switch (cmd) {
            case "nick" -> handleSetNickname(arg1);

            case "create" -> {
                int size = parseNumericArg(arg1, "Usage: create <size>");
                if (size != -1) handleCreateLobby(size);
            }

            case "join" -> {
                int index = parseNumericArg(arg1, "Usage: join <index>");
                if (index != -1) handleJoinLobby(index);
            }

            case "reconnect" -> {
                if (args.length < 3) {
                    view.onError("Usage: reconnect <nickname> <gameId>");
                } else {
                    handleReconnect(args[1], args[2]);
                }
            }

            case "totem" -> {
                try {
                    handleSelectTotem(Totem.valueOf(arg1.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    view.onError("Invalid totem color. Use: WHITE, PURPLE, BLUE, RED, YELLOW.");
                }
            }

            case "totems" -> tuiView.onShowAvailableTotems();

            case "leave" -> {
                if (lobbyModel.getCurrentLobby() != null) proxy.requestLeaveLobby();
                else view.onError("You are not currently in a lobby.");
            }

            case "move" -> {
                String target = arg1.toUpperCase();
                if (target.equals("T")) handleMoveTotem('T');
                else if (!target.isEmpty()) handleMoveTotem(target.charAt(0));
                else view.onError("Usage: move <tileID> or move T");
            }

            case "resolve" -> {
                List<String> ids = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
                handleResolveActions(ids);
            }

            case "summary" -> {
                boolean inGame = (gameModel != null && !gameModel.isGameEnded());
                if (!inGame) {
                    view.onError("The 'summary' command is only available during a game.");
                } else {
                    tuiView.onShowSummaryCard();
                }
            }

            // --- NO MORE INSTANCEOF ---
            case "info" -> {
                if (arg1.isEmpty()) view.onError("Usage: info <cardID>");
                else tuiView.onShowCardInfo(arg1.toUpperCase());
            }

            case "help" -> {
                boolean inGame = (gameModel != null && !gameModel.isGameEnded());
                boolean inLobby = (!inGame && lobbyModel.getCurrentLobby() != null);
                boolean inPreLobby = (!inGame && !inLobby);
                tuiView.onShowHelp(inPreLobby, inLobby, inGame);
            }

            case "lobby" -> {
                if (gameModel == null) {
                    if (lobbyModel.getCurrentLobby() != null) {
                        view.onError("Action blocked: You are already in the lobby. Use 'leave' to exit.");
                    } else {
                        view.onError("Action blocked: You are already in the pre-lobby screen.");
                    }
                    return;
                }
                performReturnToLobby();
            }

            case "quit" -> {
                proxy.disconnect();
                System.exit(0);
            }

            default -> {
                boolean inGame = gameModel != null && !gameModel.isGameEnded();
                boolean isMyTurn = inGame && myNickname().equals(gameModel.getCurrentPlayer());
                if (isMyTurn) handleResolveActions(List.of(cmd.toUpperCase()));
                else view.onError("Unknown command: '" + cmd + "'. Type 'help' for assistance.");
            }
        }
    }

    @Override
    protected ServerProxy createNewProxy() throws Exception {
        return ServerProxyFactory.create(networkType, host, port, lobbyModel, view);
    }

    private int parseNumericArg(String input, String usage) {
        try {
            if (input.isEmpty()) {
                view.onError(usage);
                return -1;
            }
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            view.onError("Error: Argument must be a number.");
            return -1;
        }
    }
}