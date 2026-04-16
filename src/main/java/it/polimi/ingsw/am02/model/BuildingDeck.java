package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.common.enumerations.Era;
import java.util.*;
import java.util.stream.Collectors;

public class BuildingDeck {

    private final Map<Era, List<String>> eraDecks;

    public BuildingDeck(int numPlayers) {
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

            Collections.shuffle(eraCards);

            int cardsToKeep = getCardCountForEra(era, numPlayers);

            List<String> selectedCards = new ArrayList<>(
                    eraCards.subList(0, Math.min(cardsToKeep, eraCards.size()))
            );

            this.eraDecks.put(era, selectedCards);
        }
    }

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