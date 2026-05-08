package it.polimi.ingsw.am02.common.messages.events.game;

import it.polimi.ingsw.am02.common.enumerations.CardType;
import it.polimi.ingsw.am02.common.enumerations.RowPosition;
import it.polimi.ingsw.am02.common.interfaces.VirtualView;

public record CardTakenEvent(String nickname, String cardID, CardType cardType, RowPosition sourceRow) implements GameEvent {
    @Override public void apply(VirtualView view) {
        view.notifyCardTaken(nickname, cardID, cardType, sourceRow);
    }
}