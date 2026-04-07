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


    public int getFoodPoints() { return foodPoints; }

    public int getPrestigePoints() {
        return prestigePoints;
    }

    public int getShamanStars() {
        return shamanStars;
    }

    public int getFoodDiscount() {
        return totalFoodDiscount;
    }

    public int getBuildingDiscount() {
        return totalBuildingDiscount;
    }

    public int getCharacterCount(CharacterType characterType) {
        return characters.get(characterType).size();
    }

    public int getNumCharacters() {
        int count = 0;
        for (CharacterType characterType : characters.keySet()) {
            count += characters.get(characterType).size();
        }
        return count;
    }

    public int getInventionTypeCount(InventionType inventionType) {
        return inventionCounts.get(inventionType);
    }

    public void addInventionType(InventionType inventionType){
        inventionCounts.put(inventionType, getInventionTypeCount(inventionType) + 1);
    }

    public int getNumOfDifferentInventionTypes(){
        return inventionCounts.keySet().size();
    }

    public void setFoodPoints(int foodPoints) {
        this.foodPoints = foodPoints;
    }

    public void addFoodPoints(int foodPoints){
        this.foodPoints += foodPoints;
    }

    public void  addPrestigePoints(int prestigePoints){
        this.prestigePoints += prestigePoints;
    }

    public void addShamanStars(int shamanStars){
        this.shamanStars += shamanStars;
    }

    public void addFoodDiscount(int foodDiscount){
        this.totalFoodDiscount += foodDiscount;
    }

    public void addBuildingDiscount(int buildingDiscount){
        this.totalBuildingDiscount += buildingDiscount;
    }

    public void insertCharacter(String characterID){
        //insert of the new character in the characters map
        characters.get(GameRegistry.getInstance().getCharacter(characterID).getType()).add(characterID);
        //apply the effect of the card to this tribu
        GameRegistry.getInstance().getCharacter(characterID).applyCharacterEffect(this);
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

    public void attachTribuObserver(TribuObserver effect) {
        // TODO
    }

    public void notifyEventResolution(Game game, EventCard event) {
        //TODO
    }

    //for immunity
    public void setImmuneToShamanicPenality(boolean newState) {
        immuneToShamanicPenality = newState;
    }

    public int getPPBuilders() {
        // TODO
    }


    public List<String> getCharactersOfType(CharacterType type) {
        return characters.getOrDefault(type, new ArrayList<>());
    }

    public int calculateSustanceCost(){ //to calculate how much food should be paid, after applying all discounts

        //how much food to pay normally (before applying discount)
        int costPreDiscount = 0;
        costPreDiscount = characters.values()
                .stream()
                .mapToInt(x->x.size()).sum();

        int totalDiscount = 0;
        //add gatherers' discount
        int gatherersDiscount = characters.get(CharacterType.GATHERER).size() * 3;
        //int buildingsDiscount = ;//TODO aspetta husnain e l'implementazione dei building;

                //totalDiscount = gatherersDiscount + buildingsDiscount;

        return Math.max(0, costPreDiscount - totalDiscount);
    }

}