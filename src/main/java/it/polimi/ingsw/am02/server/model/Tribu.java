package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.exceptions.CardNotFoundException;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.enumerations.InventionType;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;

import java.util.*;

public class Tribu {

    private int foodPoints;
    private int prestigePoints;
    private int shamanStars;
    private int totalFoodDiscount;
    private int totalBuildingDiscount;
    private int totalPPBuilders;
    private int totalPPBuildings;

    private final Map<InventionType, Integer> inventionCounts;
    private final Map<CharacterType, List<String>> characters;
    private final List<String> buildings;
    private final List<BuildingEffect> activeBuildingEffects;
    private final List<TribuObserver> tribuObservers;
    private boolean immuneToShamanicPenalty;

    private int lastEventBonusReceived;

    public Tribu() {

        this.foodPoints = 0;
        this.prestigePoints = 0;
        this.shamanStars = 0;
        this.totalFoodDiscount = 0;
        this.totalBuildingDiscount = 0;
        this.totalPPBuilders = 0;
        this.totalPPBuildings = 0;

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
        this.immuneToShamanicPenalty = false;

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

    public int getPPBuilders() {
        return totalPPBuilders;
    }

    public int getTotalPPBuildings() { return totalPPBuildings; }

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
        int count = 0;
        for (int val : inventionCounts.values()) {
            if (val > 0) count++;
        }
        return count;
    }

    public void setFoodPoints(int foodPoints) {
        this.foodPoints = foodPoints;
    }

    public void addFoodPoints(int foodPoints){
        this.foodPoints += foodPoints;
    }

    public void addPrestigePoints(int prestigePoints) { this.prestigePoints += prestigePoints; }

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

    public EffectOutcome insertCharacter(String characterID, Player owner){

        CharacterCard newInsertion = GameRegistry.getInstance().getCharacter(characterID);
        CharacterType type = newInsertion.getType();

        characters.get(type).add(characterID);

        List<ResourceDelta> allDeltas = new ArrayList<>();

        EffectOutcome cardOutcome = newInsertion.applyCharacterEffect(owner);
        if (cardOutcome != null && !cardOutcome.isEmpty()) {
            allDeltas.addAll(cardOutcome.resourceDeltas());
        }

        for(TribuObserver observer : tribuObservers) {
            EffectOutcome obsOutcome = observer.onCharacterInsertion(type);
            if (obsOutcome != null && !obsOutcome.isEmpty()) {
                allDeltas.addAll(obsOutcome.resourceDeltas());
            }
        }

        return new EffectOutcome(allDeltas);
    }

    public EffectOutcome insertBuilding(String buildingID, Player owner, Game game) {

        BuildingCard cardTemplate = GameRegistry.getInstance().getBuilding(buildingID);
        if (cardTemplate == null) throw new CardNotFoundException(buildingID);

        this.buildings.add(buildingID);

        List<ResourceDelta> deltas = new ArrayList<>();
        int pp = cardTemplate.getBuildingPp();

        if (pp > 0) {
            this.totalPPBuildings += pp;

            deltas.add(new ResourceDelta(
                    owner.getNickname(),
                    ResourceType.PP_BUILDINGS,
                    this.totalPPBuildings,
                    pp
            ));
        }

        BuildingEffect myPersonalEffect = BuildingFactory.createActiveEffect(
                cardTemplate.getEffectType(),
                cardTemplate.getEffectParams(),
                owner,
                game);

        if (myPersonalEffect != null) {
            this.activeBuildingEffects.add(myPersonalEffect);
            RegistrationVisitor visitor = new RegistrationVisitor(game, this);
            myPersonalEffect.accept(visitor);
        }

        return new EffectOutcome(deltas);
    }

    public void attachTribuObserver(TribuObserver effect) {
        tribuObservers.add(effect);
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

        return Math.max(0, costPreDiscount - totalFoodDiscount);
    }

    public void setImmuneToShamanicPenalty(boolean status) {
        immuneToShamanicPenalty = status;
    }

    public boolean isImmune() {
        return immuneToShamanicPenalty;
    }

    public void setLastEventBonusReceived(int bonus) { this.lastEventBonusReceived = bonus; }

    public int getLastEventBonusReceived() { return lastEventBonusReceived; }



    List<BuildingEffect> getActiveBuildingEffects() {return activeBuildingEffects; } // For testing
}