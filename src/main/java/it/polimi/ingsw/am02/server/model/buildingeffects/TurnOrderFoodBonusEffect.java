
package it.polimi.ingsw.am02.server.model.buildingeffects;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.*;
import it.polimi.ingsw.am02.common.enumerations.PhaseType;
import it.polimi.ingsw.am02.server.model.BuildingEffect;
import it.polimi.ingsw.am02.server.model.EffectVisitor;
import it.polimi.ingsw.am02.server.model.Game;
import it.polimi.ingsw.am02.server.model.Player;
import it.polimi.ingsw.am02.server.model.listeners.PhaseObserver;

import java.util.List;

/**
 * Building effect that grants one extra food point at the end of each player turn,
 * provided the owner's current turn-order position carries a positive food reward.
 *
 * <p>Registered as a {@link PhaseObserver}. Fires on {@link PhaseType#END_PLAYER_TURN}
 * and delegates to {@link Game#triggerTurnOrderExtraFood(Tribu)} to check eligibility.
 *
 * <p>JSON key: {@code "TURN_ORDER_FOOD_BONUS"}
 */
public class TurnOrderFoodBonusEffect implements BuildingEffect, PhaseObserver {
    final Player owner;
    final Game game;

    /**
     * @param owner the player who owns this building
     * @param game  the current game instance, used to trigger the turn-order food check
     */
    public TurnOrderFoodBonusEffect(Player owner, Game game) {
        this.owner = owner;
        this.game = game;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(EffectVisitor v) {
        v.visitPhaseObserver(this);
    }

    /**
     * At the end of each player turn, checks whether the owner's turn-order position
     * carries a positive food bonus and, if so, grants one extra food point.
     *
     * @param newPhase the phase the game is transitioning into
     * @return an {@link EffectOutcome} with the food delta if food was gained, or empty otherwise
     */
    @Override
    public EffectOutcome onPhaseChange(PhaseType newPhase) {
        if (newPhase == PhaseType.END_PLAYER_TURN) {
            Tribu tribu = owner.getTribu();
            String nickname = owner.getNickname();

            int foodBefore = tribu.getFoodPoints();

            game.triggerTurnOrderExtraFood(tribu);

            int foodAfter = tribu.getFoodPoints();
            int foodGained = foodAfter - foodBefore;

            if (foodGained > 0) {
                return new EffectOutcome(List.of(
                        new ResourceDelta(nickname, ResourceType.FOOD, foodAfter, foodGained)
                ));
            }
        }
        return EffectOutcome.empty();
    }
}
