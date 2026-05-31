package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.Tribu;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

import java.util.List;

/**
 * Building effect that awards prestige points at game end based on
 * the number of characters of a specific type in the owner's tribe.
 *
 * <p>Registered as a {@link PhaseObserver}. Fires once on {@link PhaseType#END_GAME}.
 * The bonus is {@code bonusPP × count(type)}.
 *
 * <p>JSON key: {@code "ENDGAME_CHARACTER_PP"}
 */
public class EndGameCharacterPrestigeEffect implements BuildingEffect, PhaseObserver {
    final Player owner;
    final CharacterType type;
    final int bonusPP;

    /**
     * @param owner   the player who owns this building
     * @param type    the character type whose count is multiplied by {@code bonusPP}
     * @param bonusPP prestige points awarded per character of the target type
     */
    public EndGameCharacterPrestigeEffect(Player owner, CharacterType type, int bonusPP) {
        this.owner = owner;
        this.type = type;
        this.bonusPP = bonusPP;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor v) {
        v.visitPhaseObserver(this);
    }

    /**
     * Computes and applies the end-game prestige bonus.
     * The total bonus is {@code bonusPP × characterCount(type)};
     * produces an empty outcome if the count is zero or the phase is not {@code END_GAME}.
     *
     * @param newPhase the phase the game is transitioning into
     * @return an {@link EffectOutcome} with the prestige-point delta, or empty
     */
    @Override
    public EffectOutcome onPhaseChange(PhaseType newPhase) {
        if(newPhase == PhaseType.END_GAME) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int bonus = bonusPP * tribu.getCharacterCount(type);

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
