/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.ArrayList;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.stat.AttackDistanceStat;
import org.dragonskulle.game.building.stat.AttackStat;
import org.dragonskulle.game.building.stat.DefenceStat;
import org.dragonskulle.game.building.stat.Stat;
import org.dragonskulle.game.building.stat.TokenGenerationStat;
import org.dragonskulle.game.building.stat.ViewDistanceStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;

/**
 * A Building component.
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
@Log
public class Building extends Component {

    /** Stores the attack strength of the building. */
    @Getter private AttackStat mAttack;
    /** Stores the defence strength of the building. */
    @Getter private DefenceStat mDefence;
    /** Stores how many tokens the building can generate in one go. */
    @Getter private TokenGenerationStat mTokenGeneration;
    /** Stores the view range of the building. */
    @Getter private ViewDistanceStat mViewDistance;
    /** Stores the attack range of the building. */
    @Getter private AttackDistanceStat mAttackDistance;

    /** The owner of the building. */
    @Getter @Setter private Reference<Player> mOwner = new Reference<>(null);
    /** The HexagonTile the building is on. */
    @Getter private Reference<HexagonTile> mTile = new Reference<HexagonTile>(null);
    /** The HexagonMap being used. */
    private Reference<HexagonMap> mHexagonMap = new Reference<HexagonMap>(null);

    /**
     * Create a new {@link Building}. Adds the Building to the {@link HexagonMap} at the specified
     * {@link HexagonTile}.
     *
     * @param hexagonMap The HexagonMap being used.
     * @param hexagonTile The HexagonTile the building is on.
     */
    public Building(Reference<HexagonMap> hexagonMap, Reference<HexagonTile> hexagonTile) {

        // TODO Clean up.
        // Move contents out of constructor.

        mTile = hexagonTile;
        mHexagonMap = hexagonMap;

        mAttack = new AttackStat();
        mDefence = new DefenceStat();
        mTokenGeneration = new TokenGenerationStat();
        mViewDistance = new ViewDistanceStat();
        mAttackDistance = new AttackDistanceStat();

        mAttack.setLevel(5);
        mDefence.setLevel(5);
        mTokenGeneration.setLevel(5);
        mViewDistance.setLevel(5);
        mAttackDistance.setLevel(5);

        // Move out of constructor:
        HexagonMap map = hexagonMap.get();
        HexagonTile tile = mTile.get();
        if (map != null && tile != null) {
            map.storeBuilding(this, tile.getQ(), tile.getR());
        }
    }

    /**
     * Attack an opponent building.
     *
     * <p>There is a chance this will either fail or succeed, influenced by the attack stat of the
     * attacking building and the defence stats of the opponent building.
     *
     * @param opponent The building to attack.
     */
    public void attack(Building opponent) {
        // TODO: Make attack success dependent on building stats.

        Random random = new Random();
        double successChance = random.nextDouble();
        // Set a 50% chance of success.
        double target = 0.5;

        if (successChance >= target) {
            log.info(
                    String.format(
                            "Successful attack: random number %f was greater or equal to target %f.",
                            successChance, target));

            // Claim the opponent building.
            opponent.setOwner(mOwner);
            // TODO: Allow the Players to update their lists of buildings they own.
        } else {
            log.info(
                    String.format(
                            "Failed attack: random number %f was not greater or equal to target %f.",
                            successChance, target));
        }
    }

    /**
     * Get an ArrayList of {@link HexagonTile}s that are within the Building's view range, as
     * specified by {@link #mViewDistance}.
     *
     * @return All the HexagonTiles within the building's view range, excluding the Building's
     *     HexagonTile, otherwise an empty ArrayList.
     */
    public ArrayList<HexagonTile> getViewableTiles() {
        // Get the current view distance.
        int distance = mViewDistance.getValue();
        // Get the tiles within the view distance.
        return getTilesInRadius(distance);
    }

    /**
     * Get an ArrayList of {@link HexagonTile}s that are within the Building's attack range, as
     * specified by {@link #mAttackDistance}.
     *
     * @return All the HexagonTiles within the building's attack range, excluding the Building's
     *     HexagonTile, otherwise an empty ArrayList.
     */
    public ArrayList<HexagonTile> getAttackableTiles() {
        // Get the current view distance.
        int distance = mAttackDistance.getValue();
        // Get the tiles within the view distance.
        return getTilesInRadius(distance);
    }

    /**
     * Get an ArrayList of all {@link HexagonTile}s, excluding the building's HexagonTile, within a
     * set radius.
     *
     * @param radius The radius.
     * @return An ArrayList of HexgaonTiles, otherwise an empty ArrayList.
     */
    private ArrayList<HexagonTile> getTilesInRadius(int radius) {
        ArrayList<HexagonTile> tiles = new ArrayList<HexagonTile>();

        // Attempt to get the current HexagonTile and HexagonMap.
        HexagonTile tile = mTile.get();
        HexagonMap map = mHexagonMap.get();
        if (tile == null || map == null) return tiles;

        // Get the current q and r coordinates.
        int qCentre = tile.getQ();
        int rCentre = tile.getR();

        for (int rOffset = -radius; rOffset <= radius; rOffset++) {
            for (int qOffset = -radius; qOffset <= radius; qOffset++) {
                // Only get tiles whose s coordinates are within the desired range.
                int sOffset = -qOffset - rOffset;

                // Do not include tiles outside of the radius.
                if (sOffset > radius || sOffset < -radius) continue;
                // Do not include the building's HexagonTile.
                if (qOffset == 0 && rOffset == 0) continue;

                // log.info(String.format("qOffset = %d, rOffset = %d, s = %d ", qOffset, rOffset,
                // s));

                // Attempt to get the desired tile, and check if it exists.
                HexagonTile selectedTile = map.getTile(qCentre + qOffset, rCentre + rOffset);
                if (selectedTile == null) continue;

                // Add the tile to the list.
                tiles.add(selectedTile);
            }
        }

        log.info("Number of tiles in range: " + tiles.size());

        return tiles;
    }

    /**
     * Get an ArrayList of opponent {@link Building}s within the range defined by {@link
     * #mAttackDistance}.
     *
     * @return An ArrayList of opponent Buildings that can be attacked.
     */
    public ArrayList<Building> getAttackableBuildings() {
        ArrayList<Building> buildings = new ArrayList<Building>();

        // Ensure the map and owner exist.
        HexagonMap map = mHexagonMap.get();
        Player owner = mOwner.get();
        if (map == null || owner == null) return buildings;

        // Get all the tiles in attackable distance.
        ArrayList<HexagonTile> attackTiles = getAttackableTiles();
        for (HexagonTile tile : attackTiles) {
            // Get the building on an attackable tile, if it exists.
            Building building = map.getBuilding(tile.getQ(), tile.getR());
            if (building == null) continue;

            // Ensure the building is not owned by the owner of this building.
            Reference<Player> buildingOwner = building.getOwner();
            // TODO Replace references to the player with a player ID.
            if (owner.equals(buildingOwner.get())) {
                log.info("Building owned by same player.");
                continue;
            }

            // Add the opponent building to the list of attackable buildings.
            buildings.add(building);
        }

        return buildings;
    }

    /**
     * Get an ArrayList of Stats that the Building has.
     *
     * @return An ArrayList of Stats.
     */
    public ArrayList<Stat<?>> getStats() {
        ArrayList<Stat<?>> stats = new ArrayList<Stat<?>>();
        stats.add(mAttack);
        stats.add(mDefence);
        stats.add(mTokenGeneration);
        stats.add(mViewDistance);
        stats.add(mAttackDistance);

        return stats;
    }

    @Override
    protected void onDestroy() {}
}
