package it.polimi.ingsw.am02.server.model;

import it.polimi.ingsw.am02.common.dto.EffectOutcome;
import it.polimi.ingsw.am02.common.dto.ResourceDelta;
import it.polimi.ingsw.am02.common.enumerations.ResourceType;
import it.polimi.ingsw.am02.server.model.exceptions.CardNotFoundException;
import it.polimi.ingsw.am02.server.model.enumerations.CharacterType;
import it.polimi.ingsw.am02.server.model.enumerations.InventionType;
import it.polimi.ingsw.am02.server.model.listeners.TribuObserver;

import java.util.*;

/**
 * Represents a player's tribe — the central aggregate that tracks all resources,
 * characters, buildings, inventions, and passive effects accumulated during the game.
 *
 * <p>Resource fields (food, prestige points, shaman stars, discounts) are accumulated
 * additively throughout the game. Building effects and character effects are registered
 * via the observer pattern and fire automatically when the relevant game events occur.
 */
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

    /**
     * Creates a new, empty Tribu with all resource counters initialised to zero.
     * All character slots, invention counts, and observer lists start empty.
     */
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

    /** @return the current food points owned by this tribe */
    public int getFoodPoints() { return foodPoints; }

    /** @return the current prestige points accumulated by this tribe */
    public int getPrestigePoints() {
        return prestigePoints;
    }

    /** @return the total number of shaman stars held by this tribe */
    public int getShamanStars() {
        return shamanStars;
    }

    /** @return the cumulative food discount applied when paying for tribe sustenance */
    public int getFoodDiscount() {
        return totalFoodDiscount;
    }

    /** @return the cumulative building cost discount applied when purchasing buildings */
    public int getBuildingDiscount() {
        return totalBuildingDiscount;
    }

    /** @return the prestige points contributed by Builder characters */
    public int getPPBuilders() {
        return totalPPBuilders;
    }

    /** @return the prestige points contributed by owned buildings */
    public int getTotalPPBuildings() { return totalPPBuildings; }

    /**
     * @param characterType the type of character to count
     * @return the number of characters of the given type currently in this tribe
     */
    public int getCharacterCount(CharacterType characterType) {
        return characters.get(characterType).size();
    }

    /** @return the total number of characters of all types in this tribe */
    public int getNumCharacters() {
        int count = 0;
        for (CharacterType characterType : characters.keySet()) {
            count += characters.get(characterType).size();
        }
        return count;
    }

    /**
     * @param inventionType the invention type to query
     * @return the number of inventions of the given type held by this tribe
     */
    public int getInventionTypeCount(InventionType inventionType) {
        return inventionCounts.get(inventionType);
    }

    /**
     * Records that this tribe has acquired one more invention of the given type.
     *
     * @param inventionType the type of invention to add
     */
    public void addInventionType(InventionType inventionType){
        inventionCounts.put(inventionType, getInventionTypeCount(inventionType) + 1);
    }

    /**
     * @return the number of distinct invention types (with at least one copy) held by this tribe.
     *         Used in final scoring to compute Inventor prestige points.
     */
    public int getNumOfDifferentInventionTypes(){
        int count = 0;
        for (int val : inventionCounts.values()) {
            if (val > 0) count++;
        }
        return count;
    }

    /**
     * Directly sets the food points to an absolute value.
     * Prefer {@link #addFoodPoints(int)} for incremental changes.
     *
     * @param foodPoints the new absolute food value
     */
    public void setFoodPoints(int foodPoints) {
        this.foodPoints = foodPoints;
    }

    /**
     * Adds (or subtracts, if negative) food points to this tribe.
     *
     * @param foodPoints the amount to add; use a negative value to deduct food
     */
    public void addFoodPoints(int foodPoints){
        this.foodPoints += foodPoints;
    }

    /**
     * Adds (or subtracts, if negative) prestige points to this tribe.
     *
     * @param prestigePoints the amount to add; use a negative value to deduct prestige
     */
    public void addPrestigePoints(int prestigePoints) { this.prestigePoints += prestigePoints; }

    /**
     * Adds shaman stars to this tribe.
     *
     * @param shamanStars the number of shaman stars to add (must be non-negative)
     */
    public void addShamanStars(int shamanStars){
        this.shamanStars += shamanStars;
    }

    /**
     * Increases the cumulative food discount for this tribe.
     *
     * @param foodDiscount the discount amount to add
     */
    public void addFoodDiscount(int foodDiscount){
        this.totalFoodDiscount += foodDiscount;
    }

    /**
     * Increases the cumulative building cost discount for this tribe.
     *
     * @param buildingDiscount the discount amount to add
     */
    public void addBuildingDiscount(int buildingDiscount){
        this.totalBuildingDiscount += buildingDiscount;
    }

    /**
     * Increases the prestige points credited to Builder characters.
     *
     * @param ppBuilders the amount to add
     */
    public void addPPBuilders(int ppBuilders){
        this.totalPPBuilders += ppBuilders;
    }

    /**
     * Adds a character card to this tribe and applies its immediate effect.
     * Also notifies all registered {@link TribuObserver}s of the insertion.
     *
     * @param characterID the ID of the character card to insert (must exist in {@link GameRegistry})
     * @param owner       the player who owns this tribe
     * @return an {@link EffectOutcome} aggregating all resource deltas produced by the insertion
     */
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

    /**
     * Adds a building card to this tribe, credits its base prestige points, and registers
     * its active {@link BuildingEffect} with the appropriate game-level observers.
     *
     * @param buildingID the ID of the building card to insert (must exist in {@link GameRegistry})
     * @param owner      the player who owns this tribe
     * @param game       the current {@link Game} instance, used to attach phase/event observers
     * @return an {@link EffectOutcome} containing the prestige-point delta for the building's base PP
     * @throws CardNotFoundException if the building ID does not exist in the registry
     */
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

    /**
     * Registers a {@link TribuObserver} that will be notified on future character insertions.
     *
     * @param effect the observer to attach
     */
    public void attachTribuObserver(TribuObserver effect) {
        tribuObservers.add(effect);
    }

    /**
     * Computes the net food cost this tribe must pay during a Sustenance event,
     * after applying the tribe's cumulative food discount.
     *
     * @return the number of food points that must be paid (always {@code >= 0})
     */
    public int calculateSustenanceCost(){

        int costPreDiscount;
        costPreDiscount = characters.values()
                .stream()
                .mapToInt(List::size).sum();

        return Math.max(0, costPreDiscount - totalFoodDiscount);
    }

    /**
     * Sets or clears immunity to the Shamanic Ritual penalty.
     * When immune, this tribe does not lose prestige points for having the fewest shaman stars.
     *
     * @param status {@code true} to grant immunity, {@code false} to remove it
     */
    public void setImmuneToShamanicPenalty(boolean status) {
        immuneToShamanicPenalty = status;
    }

    /**
     * @return {@code true} if this tribe is currently immune to the Shamanic Ritual penalty
     */
    public boolean isImmune() {
        return immuneToShamanicPenalty;
    }

    /**
     * Records the prestige-point bonus this tribe received in the most recent event resolution.
     * Used by building effects that react to event outcomes.
     *
     * @param bonus the bonus amount received (0 if none)
     */
    public void setLastEventBonusReceived(int bonus) { this.lastEventBonusReceived = bonus; }

    /** @return the prestige-point bonus received in the most recent event resolution */
    public int getLastEventBonusReceived() { return lastEventBonusReceived; }



    List<BuildingEffect> getActiveBuildingEffects() {return activeBuildingEffects; } // For testing
}