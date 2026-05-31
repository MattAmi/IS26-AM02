package it.polimi.ingsw.am02.common.enumerations;

/**
 * Enumerates the five totem colors, one per player slot.
 * Each player selects a unique totem before the game starts;
 * the choice is recorded in {@link it.polimi.ingsw.am02.common.dto.LobbyInfo}
 * and persisted in the NDJSON log for game recovery.
 */
public enum Totem {
    WHITE,PURPLE,BLUE,RED,YELLOW
}
