package it.polimi.ingsw.am02.common.messages.commands;

import java.util.List;

public record ResolveActionsCommand(String nickname, List<String> selectedIDs) implements GameCommand {}