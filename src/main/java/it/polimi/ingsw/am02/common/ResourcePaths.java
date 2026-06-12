package it.polimi.ingsw.am02.common;

/** Classpath locations of bundled JSON data files. */
public final class ResourcePaths {

    private ResourcePaths() {}

    private static final String JSON_ROOT = "/it/polimi/ingsw/am02/json";

    public static final String CHARACTERS       = JSON_ROOT + "/Characters.json";
    public static final String EVENTS           = JSON_ROOT + "/Events.json";
    public static final String BUILDINGS        = JSON_ROOT + "/Buildings.json";
    public static final String OFFER_TILES      = JSON_ROOT + "/OfferTiles.json";
    public static final String TURN_ORDER_TILES = JSON_ROOT + "/TurnOrderTiles.json";
}
