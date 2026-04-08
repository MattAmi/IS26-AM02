package it.polimi.ingsw.am02.model;

import it.polimi.ingsw.am02.model.Enumerations.*;
import java.util.*;

public class Tribu {

    private int foodPoints;
    private int prestigePoints;
    private int shamanStars;
    private int totalFoodDiscount;
    private int totalBuildingDiscount;
    private int totalPPBuilders;

    private final Map<InventionType, Integer> inventionCounts;
    private final Map<CharacterType, List<String>> characters;
    private final List<String> buildings;
    private final List<BuildingEffect> activeBuildingEffects;
    private final List<TribuObserver> tribuObservers;
    private boolean immuneToShamanicPenality;

    private int lastEventBonusReceived;


    public Tribu(int foodPoints, int prestigePoints, int shamanStars) {

        this.foodPoints = foodPoints;
        this.prestigePoints = prestigePoints;
        this.shamanStars = shamanStars;
        this.totalFoodDiscount = 0;
        this.totalBuildingDiscount = 0;
        this.totalPPBuilders = 0;
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

        this.lastEventBonusReceived = 0;
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
        return inventionCounts.size();
    }

    public void setFoodPoints(int foodPoints) {
        this.foodPoints = foodPoints;
    }

    public void addFoodPoints(int foodPoints){
        this.foodPoints += foodPoints;
    }

    public void addPrestigePoints(int prestigePoints){
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

    public void addPPBuilders(int ppBuilders){
        this.totalPPBuilders += ppBuilders;
    }

    public void insertCharacter(String characterID){

        CharacterCard newInsertion = GameRegistry.getInstance().getCharacter(characterID);

        for(TribuObserver observer : tribuObservers)
            observer.onCharacterInsertion(newInsertion.getType());

        characters.get(GameRegistry.getInstance().getCharacter(characterID).getType()).add(characterID);
        newInsertion.applyCharacterEffect(this);
    }

    public void insertBuilding(String buildingID, Game game) {

        this.buildings.add(buildingID);

        BuildingCard cardTemplate = GameRegistry.getInstance().getBuilding(buildingID);

        // sarebbe Exception // TODO
        if (cardTemplate == null) {
            System.err.println("Errore: Edificio " + buildingID + " non trovato nel Registry!");
            return;
        }

        BuildingEffect myPersonalEffect = BuildingFactory.createActiveEffect(
                cardTemplate.getEffectType(),
                cardTemplate.getEffectParams(),
                this,
                game
        );

        if (myPersonalEffect != null) {
            this.activeBuildingEffects.add(myPersonalEffect);
            RegistrationVisitor visitor = new RegistrationVisitor(game, this);
            myPersonalEffect.accept(visitor);
        }
    }

    public void attachTribuObserver(TribuObserver effect) {
        tribuObservers.add(effect);
    }

    public void setImmuneToShamanicPenality(boolean newState) {
        immuneToShamanicPenality = newState;
    }

    public int getPPBuilders() {
        return totalPPBuilders;
    }

    public List<String> getCharactersOfType(CharacterType type) {
        return characters.getOrDefault(type, new ArrayList<>());
    }

    public int calculateSustenanceCost(){ //to calculate how much food should be paid, after applying all discounts

        //how much food to pay normally (before applying discount)
        int costPreDiscount;
        costPreDiscount = characters.values()
                .stream()
                .mapToInt(List::size).sum();

        int totalDiscount = 0;
        //add gatherers' discount
        int gatherersDiscount = characters.get(CharacterType.GATHERER).size() * 3;

        return Math.max(0, costPreDiscount - gatherersDiscount);
    }

    public void setImmuneToShamanicPenalty(boolean status) {
        immuneToShamanicPenality = status;
    }

    public boolean isImmune() {
        return immuneToShamanicPenality;
    }

    public void setLastEventBonusReceived(int bonus) { this.lastEventBonusReceived = bonus; }

    public int getLastEventBonusReceived() { return lastEventBonusReceived; }
}