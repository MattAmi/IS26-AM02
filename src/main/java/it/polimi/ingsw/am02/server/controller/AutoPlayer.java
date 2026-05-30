package it.polimi.ingsw.am02.server.controller;

import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.common.messages.commands.GameCommand;
import it.polimi.ingsw.am02.common.messages.commands.MoveTotemCommand;
import it.polimi.ingsw.am02.common.messages.commands.ResolveActionsCommand;
import it.polimi.ingsw.am02.server.model.GameRegistry;

import java.util.List;

/**
 * Stateless utility that computes the best available command for a
 * disconnected player based on the current {@link ServerGameSnapshot}.
 *
 * <p>The strategy is deliberately minimal:
 * <ul>
 *   <li>During {@link it.polimi.ingsw.am02.common.enumerations.PhaseType#TOTEM_PLACEMENT}:
 *       place the totem on the first free offer tile.</li>
 *   <li>During {@link it.polimi.ingsw.am02.common.enumerations.PhaseType#ACTION_RESOLUTION}:
 *       pick one character from the upper row if possible, then the lower row;
 *       otherwise return the totem.</li>
 *   <li>During any other phase: return {@code null} (no player action expected).</li>
 * </ul>
 *
 * <p>Package-private: only {@link GameController} may call this class.
 */
final class AutoPlayer {

    private AutoPlayer() {}

    /**
     * Returns the command appropriate for the current phase, or {@code null}
     * if no player action is expected in this phase (e.g. event resolution,
     * scoring).
     *
     * @param nickname the disconnected player for whom the command is produced
     * @param snapshot the current server-side game state
     * @return a ready-to-dispatch {@link GameCommand}, or {@code null}
     */
    static GameCommand computeMove(String nickname, ServerGameSnapshot snapshot) {
        PhaseType phase = snapshot.currentPhase();
        if (phase == null) return null;

        return switch (phase) {
            case TOTEM_PLACEMENT -> computeTotemPlacement(nickname, snapshot);
            case ACTION_RESOLUTION -> computeActionResolution(nickname, snapshot);
            default -> null;
        };
    }

    // ── Totem placement

    private static GameCommand computeTotemPlacement(String nickname, ServerGameSnapshot snapshot) {
        List<Character> free = snapshot.freeOfferTiles();
        if (free.isEmpty())
            return null;

        return new MoveTotemCommand(nickname, free.getFirst());
    }

    // Action resolution

    private static GameCommand computeActionResolution(String nickname, ServerGameSnapshot snapshot) {
        if (snapshot.isExtraTurnMode()) {
            return new MoveTotemCommand(nickname, 'T');   // extra turn: always skip
        }

        if (canPlayerFinish(nickname, snapshot)) {
            return new MoveTotemCommand(nickname,'T');
        }

        // Try to pick one Character, upper row first.
        int upperLeft = snapshot.remainingUpperOf(nickname);
        int lowerLeft = snapshot.remainingLowerOf(nickname);

        if (upperLeft > 0) {
            String card = firstCharacterIn(snapshot.upperRow());
            if (card != null) return new ResolveActionsCommand(nickname, List.of(card));
        }

        if (lowerLeft > 0) {
            String card = firstCharacterIn(snapshot.lowerRow());
            if (card != null) return new ResolveActionsCommand(nickname, List.of(card));
        }

        // No Character is reachable (only Events or nothing left).
        // Return the totem: GameBoard.canPlayerFinish will accept this because
        // no Character cards are available in the rows the player can draw from.
        return new MoveTotemCommand(nickname,'T');
    }

    private static boolean canPlayerFinish(String nickname, ServerGameSnapshot snapshot) {
        int upperLeft = snapshot.remainingUpperOf(nickname);
        int lowerLeft = snapshot.remainingLowerOf(nickname);

        if (upperLeft == 0 && lowerLeft == 0)
            return true;

        boolean upperDone = upperLeft == 0 || firstCharacterIn(snapshot.upperRow()) == null;
        boolean lowerDone = lowerLeft == 0 || firstCharacterIn(snapshot.lowerRow()) == null;

        return upperDone && lowerDone;
    }

    private static String firstCharacterIn(List<String> row) {
        GameRegistry registry = GameRegistry.getInstance();
        for (String id : row) {
            if (registry.isCharacter(id)) return id;
        }
        return null;
    }
}
