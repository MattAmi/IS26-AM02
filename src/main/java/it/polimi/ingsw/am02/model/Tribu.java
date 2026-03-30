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
    private boolean immuneToShamanicPenality;


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
        this.immuneToShamanicPenality = false;
    }





    public static void attachTribuObserver(TribuObserver effect) {
        // TODO
    }

    public static void addShamanStars(int bonusStars) {
        // TODO
    }

    public int addPrestigePoints(int pp) {
        return 0;// TODO
    }

    public List<String> getCharactersOfType(CharacterType type) {
        return characters.getOrDefault(type, new ArrayList<>());
    }

    public int getPPBuilders() {
        return 0;// TODO
    }

    public void addFoodPoints(int foodBonus) {
        // TODO
    }

    public int getCharacterCount(CharacterType type) {
        // TODO
    }

    public void addFoodDiscount(int foodDiscount) {
        // TODO
    }

    public void setImmuneToShamanicPenality(boolean newState) {
        immuneToShamanicPenality = newState;
    }

    public int getInventionTypeCount(InventionType type) {
        return 0; // TODO
    }


    public void insertBuilding(String buildingID, Game game) {

        this.buildings.add(buildingID);

        BuildingCard cardTemplate = GameRegistry.getInstance().getBuilding(buildingID);

        // Sarebbe Exception
        if (cardTemplate == null) {
            System.err.println("Errore: Edificio " + buildingID + " non trovato nel Registry!");
            return;
        }

        BuildingEffect myPersonalEffect = BuildingFactory.createActiveEffect(
                cardTemplate.getEffectType(),
                cardTemplate.getEffectParams(),
                this
        );

        if (myPersonalEffect != null) {
            this.activeBuildingEffects.add(myPersonalEffect);
            RegistrationVisitor visitor = new RegistrationVisitor(game, this);
            myPersonalEffect.accept(visitor);
        }
    }

    public int getShamanStars() {
        return 0;
    }
}