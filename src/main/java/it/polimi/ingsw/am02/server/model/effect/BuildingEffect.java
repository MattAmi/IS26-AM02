package it.polimi.ingsw.am02.server.model.effect;

/**
 * Marker interface for all active building effects.
 * Implementations register themselves with the correct game observer
 * via the Visitor pattern ({@link EffectVisitor}).
 */
public interface BuildingEffect {

    /**
     * Accepts an {@link EffectVisitor} so the correct observer registration
     * method is called based on the concrete effect type.
     *
     * @param v the visitor handling the registration
     */
    void accept(EffectVisitor v);

}
