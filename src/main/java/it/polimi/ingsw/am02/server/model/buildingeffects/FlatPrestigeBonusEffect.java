package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

import java.util.List;

/**
 * Building effect that awards a fixed number of prestige points at the end of the game.
 *
 * <p>Registered as a {@link PhaseObserver}. Fires once on {@link PhaseType#END_GAME}.
 *
 * <p>JSON key: {@code "ENDGAME_FLAT_PP"}
 */
public class FlatPrestigeBonusEffect implements BuildingEffect, PhaseObserver {
    final Player owner;
    final int bonusPP;

    /**
     * @param owner   the player who owns this building
     * @param bonusPP the flat number of prestige points awarded at game end
     */
    public FlatPrestigeBonusEffect(Player owner, int bonusPP) {
        this.owner = owner;
        this.bonusPP = bonusPP;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitPhaseObserver(this);
    }

    /**
     * Adds the flat prestige bonus to the owner's tribe when the game ends.
     * Has no effect during any other phase transition.
     *
     * @param newPhase the phase the game is transitioning into
     * @return an {@link EffectOutcome} with the prestige-point delta, or empty if not {@code END_GAME}
     */
    @Override
    public EffectOutcome onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            tribu.addPrestigePoints(bonusPP);

            return new EffectOutcome(List.of(
                    new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), bonusPP)
            ));
        }

        return EffectOutcome.empty();
    }
}
