package it.polimi.ingsw.am02.client.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side singleton that loads all card definitions from the JSON resource files
 * and provides formatted string representations for use in both the TUI and the GUI.
 *
 * <p>Card definitions are loaded once at class-initialization time from three JSON files:
 * {@code Characters.JSON}, {@code Events.JSON}, and {@code Buildings.JSON}.
 * All cards are indexed by their {@code cardID} field for O(1) lookup.
 *
 * <p>This class is the client-side equivalent of
 * {@link it.polimi.ingsw.am02.server.model.GameRegistry} for display purposes only;
 * it performs no game-rule logic.
 *
 * <p>Obtain the singleton via {@link #getInstance()}.
 */
public class CardCatalog {


    private static final String CHARACTERS_PATH = "/it/polimi/ingsw/am02/JSON/Characters.JSON";
    private static final String EVENTS_PATH = "/it/polimi/ingsw/am02/JSON/Events.JSON";
    private static final String BUILDINGS_PATH = "/it/polimi/ingsw/am02/JSON/Buildings.JSON";
    private static final CardCatalog INSTANCE = new CardCatalog();
    private final Map<String, JsonNode> cards = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    private CardCatalog() {
        loadJson(CHARACTERS_PATH);
        loadJson(BUILDINGS_PATH);
        loadJson(EVENTS_PATH);
    }

    /** @return the singleton instance */
    public static CardCatalog getInstance() { return INSTANCE; }

    private void loadJson(String resourcePath) {
        try (InputStream is = CardCatalog.class.getResourceAsStream(resourcePath)) {
            JsonNode root = mapper.readTree(is);
            root.forEach(card -> cards.put(card.get("cardID").asText(), card));
        } catch (Exception e) {
            System.err.println("[CardCatalog] Failed to load " + resourcePath);
        }
    }



    /**
     * Returns a compact single-line description of the card for board/hand display.
     * Format varies by card type:
     * <ul>
     *   <li>Characters — ID, era, type, and the most relevant stat</li>
     *   <li>Buildings  — ID, era, cost, base PP, and a short effect summary</li>
     *   <li>Events     — ID, era, type, final flag, and key numeric parameters</li>
     * </ul>
     *
     * @param cardId the card identifier (e.g. {@code "C_001"}, {@code "B_020"}, {@code "E_005"})
     * @return a formatted single-line string, or {@code "[cardId]"} if the ID is unknown
     */
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

        String effectType = c.path("effectType").asText();
        JsonNode params = c.path("effectParams");

        String shortEffect;
        switch (effectType) {
            case "EVENT_CHARACTER_BONUS" ->
                    shortEffect = params.path("eventType").asText() + " bonus for " + params.path("characterType").asText();

            case "ENDGAME_CHARACTER_PP" ->
                    shortEffect = "+" + params.path("bonusPP").asInt() + " PP per " + params.path("characterType").asText();

            case "PP_MULTIPLIER_PER_TYPE" ->
                    shortEffect = "x" + params.path("multiplier").asInt() + " PP for " + params.path("characterType").asText();

            case "EXTRA_SHAMAN_STARS" ->
                    shortEffect = "+" + params.path("bonusStars").asInt() + " ★";

            case "INVENTOR_PAIR_FOOD_REWARD" ->
                    shortEffect = "+" + params.path("foodReward").asInt() + " Food per INVENTOR pair";

            case "FULL_SET_FOOD_REWARD" ->
                    shortEffect = "+" + params.path("foodReward").asInt() + " Food per Set(6)";

            case "ENDGAME_FULL_SET_PP" ->
                    shortEffect = "+" + params.path("bonusPP").asInt() + " PP per Set(6)";

            case "SHAMANIC_WIN_MULTIPLIER" ->
                    shortEffect = "x" + params.path("multiplier").asInt() + " Shamanic Win PP";

            case "SHAMANIC_IMMUNITY" ->
                    shortEffect = "Shamanic Minority Immunity";

            case "TURN_ORDER_FOOD_BONUS" ->
                    shortEffect = "+1 Food from Turn Order slot";

            case "EXTRA_TURN" ->
                    shortEffect = "+1 Pick at End of Round";

            case "ENDGAME_FLAT_PP" ->
                    shortEffect = "+" + params.path("bonusPP").asInt() + " Flat PP";

            default ->
                    shortEffect = effectType; // Fallback di sicurezza
        }

        return String.format("%-5s [Era %s] BUILDING  cost:%-2d  PP:%-2d  (%s)",
                id, era, cost, pp, shortEffect);
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

    // -----------------------------------------------------------------------
// Summary Card (TUI 'summary' command)
// -----------------------------------------------------------------------

    /**
     * Returns the text content of the Summary Card — a fixed reference panel
     * summarizing round events and end-game scoring rules.
     * Used by the TUI {@code summary} command and mirrored by the GUI's
     * {@code SummaryCardOverlay}.
     *
     * @return a multi-line ASCII-box string
     */
    public String getSummaryCardText() {
        return """
                ╔══════════════════════════════════════════════════════════════╗
                ║                    MESOS — SUMMARY CARD                      ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  ROUND EVENTS (resolved at end of each round)                ║
                ║                                                              ║
                ║  [SHAMANIC RITUAL] ★ above threshold → +? PP                 ║
                ║                    ★ below threshold → -? PP                 ║
                ║                                                              ║
                ║  [HUNT]             gain 1 Food + ? PP × Hunters             ║
                ║                                                              ║
                ║  [CAVE PAINTINGS]   0–? Shamans → -? PP                      ║
                ║                     ?+ Shamans → ? PP × Shamans              ║
                ║                                                              ║
                ║  [SUSTENANCE]       pay 1 Food per tribe member              ║
                ║                     OR lose ? PP × tribe members             ║
                ║                     (resolved last)                          ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  FINAL SCORING  (end of game, after round 10)                ║
                ║                                                              ║
                ║  Builders   → PP as printed on each Builder card             ║
                ║  Inventors  → number of Inventors                            ║
                ║               × number of different invention icons          ║
                ║  Artists    → 10 PP per every 2 Artists in your tribe        ║
                ║  Buildings  → PP printed on card                             ║
                ║               + any end-game effect bonuses                  ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  TIEBREAKER: most Food wins. Still tied → shared victory.    ║
                ╚══════════════════════════════════════════════════════════════╝
            """;
    }

    // Full cards description (TUI 'info' command / GUI tooltip)

    /**
     * Returns the full multi-line rule description of the card, suitable for a
     * detail tooltip in the GUI or the TUI {@code info} command.
     *
     * @param cardId the card identifier
     * @return a multi-line description string, or an error message if the ID is unknown
     */
    public String getFullDescription(String cardId) {
        JsonNode c = cards.get(cardId);
        if (c == null) return "Unknown Card ID: [" + cardId + "]";

        String era = c.path("era").asText();

        if (cardId.startsWith("C_")) return getFullCharacterDesc(c, era);
        if (cardId.startsWith("B_")) return getFullBuildingDesc(c, era);
        if (cardId.startsWith("E_")) return getFullEventDesc(c, era);

        return "No description available for: " + cardId;
    }

    private String getFullCharacterDesc(JsonNode c, String era) {
        String id = c.get("cardID").asText();
        String type = c.get("type").asText();
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("=== [%s] %s (Era %s) ===\n", id, type, era));

        switch (type) {
            case "INVENTOR" -> {
                sb.append("Invention: ").append(c.path("invention").asText()).append("\n");
                sb.append("Effect: At the end of the game, Inventors provide Prestige Points equal to the number of Inventors in your tribe multiplied by the number of different Invention icons you have.");
            }
            case "BUILDER" -> {
                sb.append("Building Discount: -").append(c.path("buildingDiscount").asInt()).append(" Food\n");
                sb.append("Prestige Points: ").append(c.path("prestigePoints").asInt()).append("\n");
                sb.append("Effect: Reduces the Food cost of every Building card you take. Provides the indicated PP at the end of the game.");
            }
            case "GATHERER" -> {
                sb.append("Food Discount: -").append(c.path("foodDiscount").asInt()).append(" Food\n");
                sb.append("Effect: During the Sustenance Event, they provide a discount of 3 Food tokens on the total you would have to pay.");
            }
            case "ARTIST" -> {
                sb.append("Effect: Used to gain or avoid losing Prestige Points during the Cave Paintings Event. At the end of the game, you gain 10 PP for every 2 Artists in your tribe.");
            }
            case "SHAMAN" -> {
                sb.append("Shaman Stars: ").append(c.path("shamanStars").asInt()).append("\n");
                sb.append("Effect: During the Shamanic Ritual Event, having the majority of these icons provides PP; having the minority results in losing PP.");
            }
            case "HUNTER" -> {
                sb.append("Has Gigot Icon: ").append(c.path("hasGigot").asBoolean() ? "Yes (+1 Food when played)" : "No").append("\n");
                sb.append("Effect: During the Hunt Event, you take Food and gain PP based on the number of Hunters in your tribe.");
            }
        }
        return sb.toString();
    }

    private String getFullEventDesc(JsonNode c, String era) {
        String id = c.get("cardID").asText();
        String type = c.get("type").asText();
        boolean isFinal = c.path("isFinal").asInt() == 1;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== [%s] EVENT: %s (Era %s)%s ===\n",
                id, type, era, isFinal ? " [FINAL]" : ""));

        switch (type) {
            case "HUNT" -> {
                sb.append("Effect: Take ").append(c.path("foodPerHunter").asInt())
                        .append(" Food token and gain ").append(c.path("ppPerHunter").asInt())
                        .append(" Prestige Points for each Hunter in your tribe.");
            }
            case "SUSTENANCE" -> {
                sb.append("Effect: Pay 1 Food token for each Character card in your tribe. You lose ")
                        .append(c.path("penaltyPerUnfed").asInt())
                        .append(" Prestige Points for each Character card you couldn't feed.");
            }
            case "SHAMANIC_RITUAL" -> {
                sb.append("Effect: The player with the most Shaman stars gains ")
                        .append(c.path("majorityBonus").asInt()).append(" PP. The player with the fewest loses ")
                        .append(c.path("minorityMalus").asInt()).append(" PP.");
            }
            case "CAVE_PAINTINGS" -> {
                sb.append("Requirement: At least ").append(c.path("minArtists").asInt()).append(" Artist(s).\n");
                sb.append("Effect: If you meet the requirement, gain ").append(c.path("bonusPerArtist").asInt())
                        .append(" PP for each Artist. If you fail, you lose ").append(c.path("ppMalusIfFailed").asInt()).append(" PP.");
            }
        }
        return sb.toString();
    }

    private String getFullBuildingDesc(JsonNode c, String era) {
        String id = c.get("cardID").asText();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== [%s] BUILDING (Era %s) ===\n", id, era));
        sb.append("Cost: ").append(c.path("buildingCost").asInt()).append(" Food\n");
        sb.append("Base PP: ").append(c.path("buildingPp").asInt()).append("\n");

        String effectType = c.path("effectType").asText();
        JsonNode params = c.path("effectParams"); // .path() avoids NullPointerException if field is missing

        sb.append("Effect: ");
        switch (effectType) {
            case "TURN_ORDER_FOOD_BONUS" ->
                    sb.append("When you move your Totem back to the Turn Order tile on a space with a Food bonus, immediately take 1 additional Food token.");
            case "INVENTOR_PAIR_FOOD_REWARD" ->
                    sb.append("Every time you obtain a pair of identical Inventors, take ")
                            .append(params.path("foodReward").asInt()).append(" Food tokens.");
            case "FULL_SET_FOOD_REWARD" ->
                    sb.append("Every time you complete a set of 6 different Character cards, take ")
                            .append(params.path("foodReward").asInt()).append(" Food tokens.");
            case "EVENT_CHARACTER_BONUS" -> {
                String eType = params.path("eventType").asText();
                String cType = params.path("characterType").asText();
                if ("SUSTENANCE".equals(eType)) {
                    sb.append("During the Sustenance Event, you have a discount of ")
                            .append(params.path("foodDiscount").asInt())
                            .append(" Food token on the total for each ").append(cType).append(" in your tribe.");
                } else if ("CAVE_PAINTINGS".equals(eType)) {
                    sb.append("During the Cave Paintings Event, take ")
                            .append(params.path("foodReward").asInt())
                            .append(" Food token for each ").append(cType).append(" in your tribe.");
                } else if ("HUNT".equals(eType)) {
                    sb.append("During the Hunt Event, take ")
                            .append(params.path("foodReward").asInt()).append(" Food token and gain ")
                            .append(params.path("prestigeReward").asInt()).append(" additional PP for each ").append(cType).append(".");
                }
            }
            case "ENDGAME_CHARACTER_PP" ->
                    sb.append("At the end of the game, gain ")
                            .append(params.path("bonusPP").asInt()).append(" PP for each ")
                            .append(params.path("characterType").asText()).append(" in your tribe.");
            case "ENDGAME_FULL_SET_PP" ->
                    sb.append("At the end of the game, gain ")
                            .append(params.path("bonusPP").asInt())
                            .append(" PP for each set of 6 different Character cards in your tribe.");
            case "PP_MULTIPLIER_PER_TYPE" ->
                    sb.append("At the end of the game, gain double (x")
                            .append(params.path("multiplier").asInt())
                            .append(") the Prestige Points indicated on the ")
                            .append(params.path("characterType").asText()).append(" cards in your tribe.");
            case "EXTRA_SHAMAN_STARS" ->
                    sb.append("During the Shamanic Ritual Event, your tribe has ")
                            .append(params.path("bonusStars").asInt()).append(" additional star icons.");
            case "SHAMANIC_WIN_MULTIPLIER" ->
                    sb.append("During the Shamanic Ritual Event, if you have more star icons than any other player, gain double (x")
                            .append(params.path("multiplier").asInt()).append(") the indicated PP.");
            case "SHAMANIC_IMMUNITY" ->
                    sb.append("During the Shamanic Ritual Event, you do not lose Prestige Points if you have fewer star icons than all other players.");
            case "EXTRA_TURN" ->
                    sb.append("After resolving all actions, you can take ")
                            .append(params.path("extraUpperPicks").asInt())
                            .append(" Character or Building card from the top row (paying its cost).");
            case "ENDGAME_FLAT_PP" ->
                    sb.append("At the end of the game, gain ")
                            .append(params.path("bonusPP").asInt()).append(" Prestige Points.");
            default -> sb.append("Unknown effect.");
        }

        return sb.toString();
    }

}