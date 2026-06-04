package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.effect.BuildingEffect;
import it.polimi.ingsw.am02.server.model.effect.EffectVisitor;
import it.polimi.ingsw.am02.server.model.player.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.server.model.player.Player;

import java.util.List;

/**
 * Building effect that multiplies the prestige points earned by Builder characters
 * at the end of the game.
 *
 * <p>At {@link PhaseType#END_GAME}, the extra bonus is computed as
 * {@code ppBuilders × (multiplier - 1)}, effectively scaling Builder PP by {@code multiplier}.
 *
 * <p>Registered as a {@link PhaseObserver}.
 *
 * <p>JSON key: {@code "PP_MULTIPLIER_PER_TYPE"}
 */
public class PrestigePointMultiplierPerTypeEffect implements BuildingEffect, PhaseObserver {
    final Player owner;
    final CharacterType type;
    final int multiplier;

    /**
     * @param owner      the player who owns this building
     * @param type       the character type whose PP are multiplied (currently always {@code BUILDER})
     * @param multiplier the factor by which Builder prestige points are scaled
     */
    public PrestigePointMultiplierPerTypeEffect(Player owner, CharacterType type, int multiplier) {
        this.owner = owner;
        this.type = type;
        this.multiplier = multiplier;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor visitor) {
        visitor.visitPhaseObserver(this);
    }

    /**
     * Applies the multiplier to the owner's Builder prestige points at game end.
     * Extra bonus = {@code ppBuilders × (multiplier - 1)}.
     *
     * @param newPhase the phase the game is transitioning into
     * @return an {@link EffectOutcome} with the prestige-point delta, or empty if not {@code END_GAME}
     */
    @Override
    public EffectOutcome onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int bonus = tribu.getPPBuilders() * (multiplier - 1);

            if (bonus == 0)
                return EffectOutcome.empty();

            tribu.addPrestigePoints(bonus);

            return new EffectOutcome(List.of(
                    new ResourceDelta(nickname, ResourceType.PRESTIGE_POINTS, tribu.getPrestigePoints(), bonus)
            ));
        }

        return EffectOutcome.empty();
    }

}
