package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.effect.BuildingEffect;
import it.polimi.ingsw.am02.server.model.effect.EffectVisitor;
import it.polimi.ingsw.am02.server.model.player.Player;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;
import it.polimi.ingsw.am02.server.model.player.Tribu;

import java.util.List;

/**
 * Building effect that awards prestige points at game end for each complete set
 * of all character types owned by the tribe.
 *
 * <p>A complete set is defined as having at least one character of every {@link CharacterType}.
 * The number of complete sets equals the minimum character count across all types.
 * The total bonus is {@code minCount × bonusPP}.
 *
 * <p>Registered as a {@link PhaseObserver}. Fires once on {@link PhaseType#END_GAME}.
 *
 * <p>JSON key: {@code "ENDGAME_FULL_SET_PP"}
 */
public class EndGameFullSetPPEffect implements BuildingEffect, PhaseObserver {
    final Player owner;
    final int bonusPP;

    /**
     * @param owner   the player who owns this building
     * @param bonusPP prestige points awarded per complete character set
     */
    public EndGameFullSetPPEffect(Player owner, int bonusPP) {
        this.owner = owner;
        this.bonusPP = bonusPP;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor v) {
        v.visitPhaseObserver(this);
    }

    /**
     * Computes and applies the full-set prestige bonus at game end.
     *
     * @param newPhase the phase the game is transitioning into
     * @return an {@link EffectOutcome} with the prestige-point delta, or empty if not {@code END_GAME}
     */
    @Override
    public EffectOutcome onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int nSet = Integer.MAX_VALUE;

            for (CharacterType type : CharacterType.values()) {
                int count = tribu.getCharacterCount(type);
                if (count < nSet) {
                    nSet = count;
                }
            }

            int bonus = nSet * bonusPP;

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
