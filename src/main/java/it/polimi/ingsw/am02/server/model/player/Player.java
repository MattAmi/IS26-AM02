package it.polimi.ingsw.am02.server.model.player;

import it.polimi.ingsw.am02.common.enumerations.Totem;

/**
 * Represents a player in the game, identified by a unique nickname and a totem color.
 * Each player owns a {@link Tribu} that accumulates resources and characters during the game.
 */
public class Player {

    private final String nickname;
    private final Totem totem;
    private final Tribu tribu;

    /**
     * Creates a new player with the given nickname and totem.
     * A fresh, empty {@link Tribu} is automatically created.
     *
     * @param nickname the player's unique display name
     * @param totem    the totem color assigned to this player
     */
    public Player(String nickname, Totem totem) {
        this.nickname = nickname;
        this.totem = totem;
        this.tribu = new Tribu();
    }

    /** @return the player's unique display name */
    public String getNickname() {
        return nickname;
    }

    /** @return the totem color assigned to this player */
    public Totem getTotem() {
        return totem;
    }

    /** @return the {@link Tribu} belonging to this player */
    public Tribu getTribu() { return tribu; }
}
