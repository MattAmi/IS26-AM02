package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.enumerations.Totem;

public class Player {
    //Attributi
    private final String nickname;
    private final Totem totem;
    private final Tribu tribu;
    private Boolean isWinner;
    private Boolean isConnected;

    //Costruttore
    public Player(String nickname, Totem totem) {
        this.nickname = nickname;
        this.totem = totem;
        this.tribu = new Tribu();
    }

    //Metodi
    public String getNickname() {
        return nickname;
    }

    public Totem getTotem() {
        return totem;
    }

    public Tribu getTribu() { return tribu; }

    public Boolean isWinner() {
        return isWinner;
    }

    public Boolean isConnected() {
        return isConnected;
    }

    public void setAsWinner(Boolean winner) {
        isWinner = winner;
    }

    public void setConnected(Boolean connected) {
        isConnected = connected;
    }

}
