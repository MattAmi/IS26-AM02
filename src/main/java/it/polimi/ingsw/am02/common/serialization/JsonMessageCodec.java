package it.polimi.ingsw.am02.common.serialization;

/**
 * JSON-specific specialization of {@link MessageCodec}.
 *
 * <p>Serves as a typed marker so that components depending on JSON
 * serialization can declare that dependency explicitly rather than
 * relying on the more general {@link MessageCodec}.
 */
public interface JsonMessageCodec extends MessageCodec {
}
