package it.polimi.ingsw.am02.client.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CardCatalog {

    private static final CardCatalog INSTANCE = new CardCatalog();
    private final Map<String, JsonNode> cards = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    private CardCatalog() {
        loadJson("/it.polimi.ingsw.am02.JSON/Characters.JSON");
        loadJson("/it.polimi.ingsw.am02.JSON/Buildings.JSON");
        loadJson("/it.polimi.ingsw.am02.JSON/Events.JSON");
    }

    public static CardCatalog getInstance() { return INSTANCE; }

    private void loadJson(String resourcePath) {
        try (InputStream is = CardCatalog.class.getResourceAsStream(resourcePath)) {
            JsonNode root = mapper.readTree(is);
            root.forEach(card -> cards.put(card.get("cardID").asText(), card));
        } catch (Exception e) {
            System.err.println("[CardCatalog] Failed to load " + resourcePath);
        }
    }

    public String format(String cardId) {
        JsonNode c = cards.get(cardId);
        if (c == null) return "[" + cardId + "]";

        String era = c.get("era").asText();

        if (cardId.startsWith("C_")) return formatCharacter(c, era);
        if (cardId.startsWith("B_")) return formatBuilding(c, era);
        if (cardId.startsWith("E_")) return formatEvent(c, era);
        return "[" + cardId + "]";
    }

    private String formatCharacter(JsonNode c, String era) {
        String id = c.get("cardID").asText();
        String type = c.get("type").asText();
        StringBuilder sb = new StringBuilder();
        sb.append(id).append("  [Era ").append(era).append("] ").append(type);

        switch (type) {
            case "INVENTOR" -> sb.append(" (").append(c.get("invention").asText()).append(")");
            case "BUILDER"  -> sb.append("  -").append(c.get("buildingDiscount").asInt())
                    .append(" cost  PP:").append(c.get("prestigePoints").asInt());
            case "GATHERER" -> sb.append("  -").append(c.get("foodDiscount").asInt()).append(" food");
            case "SHAMAN"   -> sb.append("  ★").append(c.get("shamanStars").asInt());
            case "HUNTER"   -> { if (c.get("hasGigot").asBoolean()) sb.append("  +gigot"); }
            case "ARTIST"   -> {}
        }
        return sb.toString();
    }

    private String formatBuilding(JsonNode c, String era) {
        String id = c.get("cardID").asText();
        int cost = c.get("buildingCost").asInt();
        int pp   = c.get("buildingPp").asInt();
        String effect = c.get("effectType").asText();
        return id + "  [Era " + era + "] BUILDING  cost:" + cost + "  PP:" + pp + "  " + effect;
    }

    private String formatEvent(JsonNode c, String era) {
        String id = c.get("cardID").asText();
        String type = c.get("type").asText();
        boolean isFinal = c.get("isFinal").asInt() == 1;
        StringBuilder sb = new StringBuilder();
        sb.append(id).append("  [Era ").append(era).append("] ").append(type);
        if (isFinal) sb.append(" [FINAL]");

        switch (type) {
            case "HUNT"            -> sb.append("  food/hunter:").append(c.get("foodPerHunter").asInt())
                    .append("  PP/hunter:").append(c.get("ppPerHunter").asInt());
            case "SUSTENANCE"      -> sb.append("  penalty/unfed:").append(c.get("penaltyPerUnfed").asInt());
            case "SHAMANIC_RITUAL" -> sb.append("  maj:+").append(c.get("majorityBonus").asInt())
                    .append("  min:-").append(c.get("minorityMalus").asInt());
            case "CAVE_PAINTINGS"  -> sb.append("  min:").append(c.get("minArtists").asInt())
                    .append("  +").append(c.get("bonusPerArtist").asInt())
                    .append("/artist  malus:-").append(c.get("ppMalusIfFailed").asInt());
        }
        return sb.toString();
    }
}