/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;

/**
 * This base class will allow AI players to be created and used throughout the game.
 *
 * @author DragonSkulle
 */
@Log
public class ProbabilisticAiPlayer extends AiPlayer {

    /** Will choose whether to place a building or to use the building. */
    protected float mTileProbability = 0.5f;

    protected float mBuildingProbability = 1 - mTileProbability;

    /** Choose what to do with the building -- These 3 must sum to 1 */
    protected float mUpgradeBuilding = 0.2f;

    protected float mAttackBuilding = 0.7f;
    protected float mSellBuilding = 0.1f;

    /** A Constructor for an AI Player */
    public ProbabilisticAiPlayer() {}

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void simulateInput() {

        // TODO: Remove/formalise this.
        if (getPlayer().getNumberOfOwnedBuildings() == 0) {
            log.info("AI player has no buildings... Ending turn.");
            return;
        }

        // If only one building assumed that its capital
        if (getPlayer().getNumberOfOwnedBuildings() == 1) {
            // TODO: What if there are no valid places to put a building? Should it attack/upgrade instead?
        	log.info("AI Player has 1 building. Placing another building.");

            addBuilding();
            return;

        } else {
            log.info(
                    "AI: I have "
                            + getPlayer().getNumberOfOwnedBuildings()
                            + " buildings. Should be  more than  one");

            // Pick a random number to choose whether to place a building or to use a building
            float randomNumber = mRandom.nextFloat();

            // Choose to place a building
            if (randomNumber <= mTileProbability) {

                addBuilding();
                return;

                // Choose to do something with a building
            } else {
                // Pick a random number to choose whether to place a building or to use a building
                randomNumber = mRandom.nextFloat();

                // Choose to upgrade a building
                if (randomNumber <= mUpgradeBuilding) {

                    upgradeBuilding();
                    return;

                    // Choose to attack
                } else if (randomNumber > mUpgradeBuilding
                        && randomNumber <= mAttackBuilding + mUpgradeBuilding) {

                    attack();
                    return;

                    // Choose to sell a building
                } else {
                    sell();
                    return;
                }
            }
        }
    }

    /**
     * Gets all {@link HexagonTile}s that can be used to place a {@link Building} on.
     *
     * @return A list of HexagonTiles, all of which can have a building placed on them.
     */
    private List<HexagonTile> getBuildableTiles() {

        List<HexagonTile> buildableTiles = new ArrayList<HexagonTile>();

        getPlayer()
                .getOwnedBuildingsAsStream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(
                        building -> {
                            List<HexagonTile> visibleTiles = building.getViewableTiles();
                            // Check each tile is valid.
                            for (HexagonTile tile : visibleTiles) {
                            	if(tile.isClaimed() == false && tile.hasBuilding() == false) {
                            		buildableTiles.add(tile);
                            	}
                            }
                        });

        return buildableTiles;
    }

    /**
     * Pick and attempt to place a {@link Building}.
     * 
     * @return Whether the attempt to pick and add a building was successful.
     */
    private boolean addBuilding() {
    	log.info("Placing Building");

    	// Gets all the tiles it can expand to
        List<HexagonTile> tilesToUse = getBuildableTiles();

        // Checks if there are tiles
        if (tilesToUse.size() > 0) {
            // Picks a random number and thus a random tile.
            int randomIndex = mRandom.nextInt(tilesToUse.size());
            HexagonTile tileToBuildOn = tilesToUse.get(randomIndex);
            
            return getPlayer().buildAttempt(tileToBuildOn);
        }
        
        return false;
    }

    /**
     * Pick a {@link Building} and attempt to upgrade one of its stats.
     * 
     * @return Whether the attempt to upgrade a building's stats was successful.
     */
    private boolean upgradeBuilding() {
    	log.info("AI: Upgrading");
    	
    	if(getPlayer().getNumberOfOwnedBuildings() == 0) {
    		return false;
    	}
    	
    	int buildingIndex = mRandom.nextInt(getPlayer().getNumberOfOwnedBuildings());
    	Reference<Building> buildingReference = getPlayer().getOwnedBuildings().get(buildingIndex);
        if(buildingReference == null || buildingReference.isValid() == false) {
        	log.info("AI: could not get building to upgrade.");
        	return false;
        }
        
        // Get the Building.
        Building building = buildingReference.get();
        
        // Get Stat to upgrade.
        ArrayList<SyncStat<?>> stats = building.getStats();
        SyncStat<?> stat = stats.get(mRandom.nextInt(stats.size()));

        return getPlayer().statAttempt(building, stat);
    }

    /** This will attack a building */
    private void attack() {
        log.info("AI: Attacking");
        ArrayList<Building[]> buildingsToAttack = new ArrayList<Building[]>();

        // Will create a list of [attacker (your building), defender (building to
        // attack)]
        mPlayer.get()
                .getOwnedBuildingsAsStream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(
                        b -> {
                            List<Building> attackableBuildings = b.getAttackableBuildings();
                            for (Building buildingWhichCouldBeAttacked : attackableBuildings) {
                                Building[] listToAdd = {b, buildingWhichCouldBeAttacked};

                                buildingsToAttack.add(listToAdd);
                            }
                        });

        // Checks if you can attack
        if (buildingsToAttack.size() != 0) {

            // getting a random building to
            // {attackFrom, and attackTo}
            // Chosen building to attack in form [buildingToAttackFrom,
            // buildingToAttack]
            Building[] buildingToAttack =
                    buildingsToAttack.get(mRandom.nextInt(buildingsToAttack.size()));
            // Send to server

            if (buildingToAttack == null
                    || buildingToAttack[0] == null
                    || buildingToAttack[1] == null) {
                // Check in case accidentally a null slipped in
                return;
            }
            mPlayer.get().attackAttempt(buildingToAttack[0], buildingToAttack[1]);

            return;
        } else {
            return;
        }
    }

    /** This will sell a building */
    private void sell() {
        log.info("AI: Selling");
        if (mPlayer.get().getNumberOfOwnedBuildings() > 1) {

            int sellID = mRandom.nextInt(mPlayer.get().getNumberOfOwnedBuildings());

            Building buildingToSell =
                    mPlayer.get()
                            .getOwnedBuildingsAsStream()
                            .filter(Reference::isValid)
                            .map(Reference::get)
                            .filter(b -> !b.isCapital())
                            .limit(sellID + 1) // limit to the random number
                            .reduce((__, second) -> second) // take the last
                            .orElse(null);

            // Now have building to sell
            if (buildingToSell != null) {
                mPlayer.get().sellAttempt(buildingToSell);
            }
            return;
        } else {
            return;
        }
    }
    
    private Player getPlayer() {
    	Player player = mPlayer.get();
    	if(player == null) {
    		log.severe("Reference to mPlayer is null!");
    	}
    	return player;
    }
}
