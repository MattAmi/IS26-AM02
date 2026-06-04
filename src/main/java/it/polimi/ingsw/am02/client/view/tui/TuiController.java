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
 * TUI-specific input handler that reads commands from standard input and
 * dispatches them to the appropriate {@link ClientController} actions.
 *
 * <p>Owns the main read-dispatch loop ({@link #run()}) and translates raw
 * text tokens into typed method calls on the parent controller. The class also
 * stores the connection parameters ({@code networkType}, {@code host},
 * {@code port}) needed to rebuild the server proxy after a disconnection.</p>
 *
 * <p><b>MVC layer:</b> View — pure input handling; no rendering logic.</p>
 */
public class TuiController extends ClientController {

    /** Direct reference to the TUI view, avoiding repeated casting. */
    private final TuiView tuiView;

    /** Transport technology chosen by the user at startup (RMI or Socket). */
    private final NetworkType networkType;

    /** Server hostname or IP address. */
    private final String host;

    /** Server port number. */
    private final int port;

    /**
     * Creates a {@code TuiController} bound to the given proxy, models, and view.
     *
     * @param proxy       the server-communication proxy
     * @param lobbyModel  shared client-side lobby state
     * @param view        the TUI view that will render output; must be a {@link TuiView}
     * @param networkType the transport technology chosen at startup
     * @param host        server hostname or IP address
     * @param port        server port number
     */
    public TuiController(ServerProxy proxy, LobbyModel lobbyModel, ClientView view,
                         NetworkType networkType, String host, int port) {
        super(proxy, lobbyModel, view);
        this.tuiView = (TuiView) view;
        this.networkType = networkType;
        this.host = host;
        this.port = port;
    }

    /**
     * Starts the blocking input loop, reading one line at a time from standard
     * input and forwarding each non-empty line to {@link #dispatch(String)}.
     *
     * <p>Runs on the calling thread; designed to be invoked from the application
     * main thread after all components have been wired up.</p>
     */
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

    /**
     * Parses a single trimmed input line and routes it to the correct handler.
     *
     * <p>Recognised top-level commands: {@code nick}, {@code create}, {@code join},
     * {@code reconnect}, {@code totem}, {@code totems}, {@code leave}, {@code move},
     * {@code resolve}, {@code summary}, {@code info}, {@code help}, {@code lobby},
     * {@code quit}. Any unrecognized token is treated as a bare card ID and
     * forwarded to {@link #handleResolveActions} when it is the local player's turn.</p>
     *
     * @param input the raw, trimmed input line
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Rebuilds the server proxy using the connection parameters supplied at
     * construction time, enabling transparent reconnection after a network fault.</p>
     */
    @Override
    protected ServerProxy createNewProxy() throws Exception {
        return ServerProxyFactory.create(networkType, host, port, lobbyModel, view);
    }

    /**
     * Attempts to parse {@code input} as an integer.
     * Displays {@code usage} via the view and returns {@code -1} if the string
     * is empty or not a valid integer.
     *
     * @param input the string token to parse
     * @param usage human-readable usage hint shown on parse failure
     * @return the parsed integer, or {@code -1} on failure
     */
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