package it.polimi.ingsw.am02.common.serialization;

import it.polimi.ingsw.am02.common.messages.Message;

public interface MessageCodec {
    String encode(Message message);
    Message decode(String string);
}
