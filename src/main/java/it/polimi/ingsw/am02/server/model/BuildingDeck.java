package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.enumerations.Era;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The building card deck, partitioned by era.
 * On construction, all building cards are shuffled within each era and
 * trimmed to the player-count-appropriate quantity.
 */
public class BuildingDeck {

    private final Map<Era, List<String>> eraDecks;

    /**
     * Builds the era-partitioned deck for the given player count.
     *
     * @param numPlayers the number of players (2–5); determines how many cards are kept per era
     * @param gameRandom the seeded random source used for shuffling
     */
    public BuildingDeck(int numPlayers, Random gameRandom) {
        this.eraDecks = new EnumMap<>(Era.class);

        GameRegistry registry = GameRegistry.getInstance();
        List<String> allBuildingIds = registry.getAllBuildingsIDs();

        List<Era> buildingEras = Arrays.asList(Era.I, Era.II, Era.III);

        for (Era era : buildingEras) {
            List<String> eraCards = allBuildingIds.stream()
                    .map(registry::getBuilding)
                    .filter(Objects::nonNull)
                    .filter(card -> card.getEra() == era)
                    .map(BuildingCard::getCardID)
                    .collect(Collectors.toList());

            Collections.shuffle(eraCards, gameRandom);

            int cardsToKeep = getCardCountForEra(era, numPlayers);

            List<String> selectedCards = new ArrayList<>(
                    eraCards.subList(0, Math.min(cardsToKeep, eraCards.size()))
            );

            this.eraDecks.put(era, selectedCards);
        }
    }

    /**
     * @param era the era whose building cards are requested
     * @return the list of building card IDs selected for the given era; empty if none
     */
    public List<String> getBuildingsForEra(Era era) {
        return this.eraDecks.getOrDefault(era, new ArrayList<>());
    }

    private int getCardCountForEra(Era era, int numPlayers) {
        switch (numPlayers) {
            case 2: return era == Era.I ? 1 : era == Era.II ? 2 : 3;
            case 3: return era == Era.I ? 2 : era == Era.II ? 2 : 4;
            case 4: return era == Era.I ? 2 : era == Era.II ? 3 : 4;
            case 5: return era == Era.I ? 2 : era == Era.II ? 3 : 5;
            default: throw new IllegalArgumentException("Invalid number of players: " + numPlayers);
        }
    }

}