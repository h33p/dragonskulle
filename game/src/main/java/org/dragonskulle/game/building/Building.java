/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.ArrayList;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.stat.SyncAttackDistanceStat;
import org.dragonskulle.game.building.stat.SyncAttackStat;
import org.dragonskulle.game.building.stat.SyncDefenceStat;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.building.stat.SyncTokenGenerationStat;
import org.dragonskulle.game.building.stat.SyncViewDistanceStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.sync.SyncBool;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * A Building component.
 *
 * <p>Once created, the GameObject needs a {@link TransformHex} component to place it in the game
 * and to allow the logic to access its position.
 *
 * <p>The owner of the Building also needs to be set via {@link NetworkObject#setOwnerId}.
 *
 * <p>The building needs to be added to the relevant {@link HexagonTile} (which can be done via
 * {@link HexagonMap#storeBuilding(Building, int, int)}).
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
@Log
public class Building extends NetworkableComponent implements IOnAwake, IOnStart {

    /** Stores the attack strength of the building. */
    @Getter public final SyncAttackStat mAttack = new SyncAttackStat(this);
    /** Stores the defence strength of the building. */
    @Getter public final SyncDefenceStat mDefence = new SyncDefenceStat(this);
    /** Stores how many tokens the building can generate in one go. */
    @Getter
    public final SyncTokenGenerationStat mTokenGeneration = new SyncTokenGenerationStat(this);
    /** Stores the view range of the building. */
    @Getter public final SyncViewDistanceStat mViewDistance = new SyncViewDistanceStat(this);
    /** Stores the attack range of the building. */
    @Getter public final SyncAttackDistanceStat mAttackDistance = new SyncAttackDistanceStat(this);

    /** Whether the building is a capital. */
    public final SyncBool mIsCapital = new SyncBool(false);
    
    /** The tiles the building claims, including the tile the building is currently on. */
    @Getter private ArrayList<HexagonTile> mClaimedTiles = new ArrayList<HexagonTile>();

    /** The tiles the building can currently view (within the current {@link #mViewDistance}). */
    @Getter private ArrayList<HexagonTile> mViewableTiles = new ArrayList<HexagonTile>();

    /**
     * The tiles the building can currently attack (within the current {@link #mAttackDistance}).
     */
    @Getter private ArrayList<HexagonTile> mAttackableTiles = new ArrayList<HexagonTile>();

    /** The cost to buy a {@link Building}. */
    public static final int BUY_PRICE = 10;
    /** The reimbursement from selling a {@link Building}. */
    public static final int SELL_PRICE = 2;

    /** Store the {@link HexagonMap} that the {@link Building} is on. */
    private Reference<HexagonMap> mMap = new Reference<HexagonMap>(null);
    
    /**
     * Create a new {@link Building}. This should be added to a {@link HexagonTile}. {@link
     * HexagonTile}.
     */
    public Building() {}

    @Override
    public void onAwake() {
        // For debugging, set all stat levels to 5.
        // TODO: Remove.
        mAttack.setLevel(5);
        mDefence.setLevel(5);
        mTokenGeneration.setLevel(5);
        mViewDistance.setLevel(5);
        mAttackDistance.setLevel(5);
    }

    @Override
    public void onStart() {
    	// Store the map.
    	mMap = Scene.getActiveScene().getSingleton(HexagonMap.class).getReference(HexagonMap.class);
    	if(mMap == null || mMap.isValid() == false) {
    		log.severe("Map is null.");
    	}
    	
    	// Add the Building to the owner's mOwnedBuildings.
        Player owningPlayer = getOwner();
        if (owningPlayer != null) owningPlayer.addOwnership(this);

        // Add the building to the relevant HexagonTile.
        getTile().setBuilding(this);

        // Generate the list of tiles that have been claimed by the Building. This is always a set
        // radius so only needs to be generated once.
        generateClaimTiles();
        // Generate the lists of tiles that are influenced by the Stats of the Building.
        generateTileLists();
    }

    /** Generate the stored lists of {@link HexagonTile}s. */
    private void generateTileLists() {
        generateViewTiles();
        generateAttackableTiles();
    }

    /**
     * Ensures that changes to stats are reflected in the building.
     *
     * <ul>
     *   <li><b>Needs to manually be called on the server</b> after stats have been changed.
     *   <li>Automatically called on the client via {@link SyncStat}s.
     * </ul>
     */
    public void afterStatChange() {
        log.info("After stats change.");
        generateTileLists();
    }

    /** Claim the tiles around the building and the tile the building is on. */
    private void generateClaimTiles() {
        // Get the map.
        HexagonMap map = getMap();
        if (map == null) return;

        // Claim the tiles around the building.
        mClaimedTiles = map.getTilesInRadius(getTile(), 1);
        // Claim the tile the building is on.
        mClaimedTiles.add(getTile());

        // Allow each hexagon tile to store the claim.
        for (HexagonTile hexagonTile : mClaimedTiles) {
            hexagonTile.setClaimedBy(this);
        }
    }

    /**
     * Store the tiles that can be viewed around the building, including the tile the building is
     * on.
     */
    private void generateViewTiles() {
        // Get the map.
        HexagonMap map = getMap();
        if (map == null) return;

        // Get the current view distance.
        int distance = mViewDistance.getValue();

        // Get the tiles within the view distance.
        mViewableTiles = map.getTilesInRadius(getTile(), distance);
        // View the tile the building is on.
        mViewableTiles.add(getTile());
    }

    /**
     * Store the tiles that can be attacked around the building, excluding the tile the building is
     * on.
     */
    private void generateAttackableTiles() {
        // Get the map.
        HexagonMap map = getMap();
        if (map == null) return;

        // Get the current attack distance.
        int distance = mAttackDistance.getValue();
        // Get the tiles within the attack distance.
        mAttackableTiles = map.getTilesInRadius(getTile(), distance);
    }

    /**
     * Attack an opponent building.
     *
     * <p><b> Currently has no effect other than the basic calculations (i.e. it does not transfer
     * ownership of Buildings). </b>
     *
     * <p>There is a chance this will either fail or succeed, influenced by the attack stat of the
     * attacking building and the defence stats of the opponent building.
     *
     * @param opponent The building to attack.
     * @return Whether the attack was successful or not.
     */
    public boolean attack(Building opponent) {
        /** The number of sides on the dice */
        final int maxValue = 1000;

        // Get the attacker and defender's stats.
        int attack = mAttack.getValue();
        int defence = opponent.mDefence.getValue();

        // Stores the highest result of rolling a dice a set number of times, defined by the attack
        // stat.
        int highestAttack = 0;
        // Stores the highest result of rolling a dice a set number of times, defined by the defence
        // stat.
        int highestDefence = 0;

        // Roll a die a number of times defined by the attack stat.
        for (int i = 1; i < attack; i++) {
            int value = (int) (Math.random() * (maxValue) + 1);
            // Store the highest value achieved.
            if (value > highestAttack) {
                highestAttack = value;
            }
        }

        // Roll a die a number of times defined by the defence stat.
        for (int i = 1; i < defence; i++) {
            int value = (int) (Math.random() * (maxValue) + 1);
            // Store the highest value achieved.
            if (value > defence) {
                highestDefence = value;
            }
        }

        // Check to see who has the highest value, and won.
        if (highestAttack > highestDefence) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get an ArrayList of opponent {@link Building}s within the range defined by {@link
     * #mAttackDistance}.
     *
     * @return An ArrayList of opponent Buildings that can be attacked.
     */
    public ArrayList<Building> getAttackableBuildings() {
        ArrayList<Building> buildings = new ArrayList<Building>();

        // Get the map.
        HexagonMap map = getMap();
        if (map == null) return buildings;

        // Get all the tiles in attackable distance.
        ArrayList<HexagonTile> attackTiles = getAttackableTiles();
        for (HexagonTile tile : attackTiles) {
            // Get the building on an attackable tile, if it exists.
            Building building = tile.getBuilding();
            if (building == null) continue;

            // Ensure the building is not owned by the owner of this building.
            if (getOwnerID() == building.getOwnerID()) {
                log.info("Building owned by same player.");
                continue;
            }

            // Add the opponent building to the list of attackable buildings.
            buildings.add(building);
        }

        return buildings;
    }

    /**
     * Get whether the target {@link Building} is within attackable range from the Building.
     *
     * @param target The Building to attack.
     * @return {@code true} if the target is within attackable distance, otherwise {@code false}.
     */
    public boolean isBuildingAttackable(Building target) {
        ArrayList<HexagonTile> attackTiles = getAttackableTiles();
        HexagonTile targetTile = target.getTile();
        for (HexagonTile tile : attackTiles) {
            if (tile.equals(targetTile)) return true;
        }
        return false;
    }

    /**
     * Get the {@link HexagonTile} the {@link Building} is on.
     *
     * @return The tile the building is on, or {@code null}.
     */
    public HexagonTile getTile() {
        HexagonMap map = getMap();
        if (map == null) return null;

        Vector3i position = getPosition();
        if (position == null) return null;

        return map.getTile(position.x(), position.y());
    }

    /**
     * Get the current axial coordinates of the building using the {@link GameObject}'s {@link
     * TransformHex}.
     *
     * @return A 3d-vector of integers containing the x, y and z position of the building, or {@code
     *     null}.
     */
    private Vector3i getPosition() {
        TransformHex tranform = getGameObject().getTransform(TransformHex.class);

        if (tranform == null) {
            return null;
        }

        Vector3f floatPosition = new Vector3f();
        tranform.getLocalPosition(floatPosition);

        Vector3i integerPosition = new Vector3i();
        integerPosition.set(
                (int) floatPosition.x(), (int) floatPosition.y(), (int) floatPosition.z());

        return integerPosition;
    }

    /**
     * Get the {@link HexagonMap} being used.
     *
     * @return The map.
     */
    private HexagonMap getMap() {
        return mMap.get();
    }

    /**
     * Get an ArrayList of Stats that the Building has.
     *
     * @return An ArrayList of Stats.
     */
    public ArrayList<SyncStat<?>> getStats() {
        ArrayList<SyncStat<?>> stats = new ArrayList<SyncStat<?>>();
        stats.add(mAttack);
        stats.add(mDefence);
        stats.add(mTokenGeneration);
        stats.add(mViewDistance);
        stats.add(mAttackDistance);

        return stats;
    }

    /**
     * Get the ID of the owner of the building.
     *
     * @return The ID of the owner.
     */
    public int getOwnerID() {
        return getNetworkObject().getOwnerId();
    }

    /**
     * Get the {@link Player} which owns the {@link Building}.
     *
     * @return The owning player, or {@code null}.
     */
    public Player getOwner() {
        return getNetworkObject()
                .getNetworkManager()
                .getObjectsOwnedBy(getNetworkObject().getOwnerId())
                .map(NetworkObject::getGameObject)
                .map(go -> go.getComponent(Player.class))
                .filter(ref -> ref != null)
                .filter(Reference::isValid)
                .map(Reference::get)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get whether the building is a capital.
     *
     * @return Whether the building is a capital.
     */
    public boolean isCapital() {
        return mIsCapital.get();
    }

    /**
     * Set the building to be a capital.
     *
     * <p>By default, buildings are not capitals.
     *
     * @param isCapital Whether the building should be capital.
     */
    public void setCapital(boolean isCapital) {
        mIsCapital.set(isCapital);
    }

    /**
     * // TODO: Ensure this is correct.
     *
     * <p>Remove this building from the {@link Player} who owns the building and references to it in
     * any {@link HexagonTile}s.
     */
    public void remove() {
        // Remove the building from the tile.
        getTile().setBuilding(null);

        // Remove any claims.
        for (HexagonTile hexagonTile : mClaimedTiles) {
            hexagonTile.setClaimedBy(null);
        }

        // Reset the list of claimed, viewable and attackable tiles.
        mClaimedTiles = new ArrayList<HexagonTile>();
        mViewableTiles = new ArrayList<HexagonTile>();
        mAttackableTiles = new ArrayList<HexagonTile>();

        // TODO: Request that the building should be destroyed.
        // destroy();
    }
    
    public int attackCost() {
    	
    	//Base cost
    	int cost = 20;
    	
    	cost += (mDefence.get() * 3);
    	cost += (mAttack.get() * 2);
    	cost += (mTokenGeneration.get());
    	cost += (mViewDistance.get());
    	cost += (mAttackDistance.get());
    	
    	if (isCapital()) {
    		cost += 10;
    	}
    	
    	return cost;
    }

    @Override
    protected void onDestroy() {}
}
