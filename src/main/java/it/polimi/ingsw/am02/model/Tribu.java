package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.*;
import java.util.*;

public class Tribu {

    private int foodPoints;
    private int prestigePoints;
    private int shamanStars;
    private int totalFoodDiscount;
    private int totalBuildingDiscount;

    private Map<InventionType, Integer> inventionCounts;
    private Map<CharacterType, List<String>> characters;
    private List<String> buildings;
    private List<BuildingEffect> activeBuildingEffects;
    private List<TribuObserver> tribuObservers;


    public Tribu(int foodPoints, int prestigePoints, int shamanStars) {

        this.foodPoints = foodPoints;
        this.prestigePoints = prestigePoints;
        this.shamanStars = shamanStars;
        this.totalFoodDiscount = 0;
        this.totalBuildingDiscount = 0;
        this.inventionCounts = new EnumMap<>(InventionType.class);
        for (InventionType type : InventionType.values()) {
            this.inventionCounts.put(type, 0);
        }
        this.characters = new EnumMap<>(CharacterType.class);
        for (CharacterType type : CharacterType.values()) {
            this.characters.put(type, new ArrayList<>());
        }
        this.buildings = new ArrayList<>();
        this.activeBuildingEffects = new ArrayList<>();
        this.tribuObservers = new ArrayList<>();
    }





    public static void attachTribuObserver(TribuObserver effect) {
        // TODO: Husnain
    }

    public int addPrestigePoints(int pp) {
        return 0;// TODO: Raed
    }

    public List<String> getCharactersOfType(CharacterType type) {
        return characters.getOrDefault(type, new ArrayList<>());
    }
}