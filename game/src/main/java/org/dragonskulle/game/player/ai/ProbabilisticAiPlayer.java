/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.networkData.AttackData;
import org.dragonskulle.game.player.networkData.BuildData;
import org.dragonskulle.game.player.networkData.SellData;
import org.dragonskulle.game.player.networkData.StatData;

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

        // If only one building assumed that its capital
        if (mPlayer.get().numberOfBuildings() == 1) {
            log.warning("Only have 1");

            addBuilding();
            return;

        } else {
            log.info(
                    "AI: I have "
                            + mPlayer.get().numberOfBuildings()
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
     * A private method which will return all the hex tiles which can be used to place a building in
     *
     * @return A list of hexagon tiles which can be expanded into.
     */
    private List<HexagonTile> hexTilesToExpand() {

        List<HexagonTile> hexTilesToExpand = new ArrayList<HexagonTile>();

        mPlayer.get()
                .getOwnedBuildingsAsStream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(
                        building -> {
                            List<HexagonTile> hexTilesWhichCanBeSeen = building.getViewableTiles();
                            // log.info("hexTilesWhichCanBeSeen: " + hexTilesWhichCanBeSeen);

                            // Check each tile is valid
                            for (HexagonTile hexTile : hexTilesWhichCanBeSeen) {

                                if (hexTile.getBuilding() != null) {
                                    // Ignore cos there's already a building there
                                } else if (!checkCloseBuildings(hexTile)) {
                                    // IGNORE TILE IT'S WITHIN 1 HEX
                                } // Can add extra checks here.
                                else {
                                    hexTilesToExpand.add(hexTile);
                                }
                            }
                        });

        return hexTilesToExpand;
    }

    /**
     * This will check if the hex tile chosen to build in is within 1 place of any other building.
     *
     * @param hexTile The hex tile to build in
     * @return {@code true} if that hextile is valid to build in or {@code false} if it's not valid
     */
    private boolean checkCloseBuildings(HexagonTile hexTile) {

        // Get a radius of tiles
        ArrayList<HexagonTile> hexTiles = mPlayer.get().getTilesInRadius(1, hexTile);

        // Check if building there
        for (HexagonTile tile : hexTiles) {
            if (mPlayer.get().getMapComponent().getBuilding(tile.getQ(), tile.getR()) != null) {
                return false;
            }
        }

        return true;
    }

    /** This will add a building for the AiPlayer */
    private void addBuilding() {

        // Gets all the tiles it can expand to
        log.info("Placing Building");
        List<HexagonTile> tilesToUse = hexTilesToExpand();

        // Checks if there are tiles
        if (tilesToUse.size() != 0) {
            // Picks a random number thus a random tile
            int randomIndex = mRandom.nextInt(tilesToUse.size());
            HexagonTile tileToExpandTo = tilesToUse.get(randomIndex);

            // Send to server
            mPlayer.get().handleEvent(new BuildData(tileToExpandTo));

            return;
        } else {
            return; // end
        }
    }

    /** This will upgrade a building for the AiPlayer */
    private void upgradeBuilding() {
        log.warning("Number " + mPlayer.get().numberOfBuildings());
        int upgradeID = mRandom.nextInt(mPlayer.get().numberOfBuildings());

        log.info("AI: Upgrading");

        Building buildingToUpgrade =
                mPlayer.get()
                        .getOwnedBuildingsAsStream()
                        .filter(Reference::isValid)
                        .map(Reference::get)
                        .limit(upgradeID + 1) // limit to the random number
                        .reduce((__, second) -> second) // take the last
                        .orElse(null);

        if (buildingToUpgrade == null) {
            log.info("AI: could not get building to upgrade.");
            return;
        }

        // Get Stat to upgrade
        ArrayList<SyncStat<?>> statsArray = buildingToUpgrade.getStats();
        SyncStat<?> statToUpgrade = statsArray.get(mRandom.nextInt(statsArray.size()));

        // Send to server
        mPlayer.get().handleEvent(new StatData(buildingToUpgrade, statToUpgrade));
        return;
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
            mPlayer.get().handleEvent(new AttackData(buildingToAttack[0], buildingToAttack[1]));

            return;
        } else {
            return;
        }
    }

    /** This will sell a building */
    private void sell() {
        log.info("AI: Selling");
        if (mPlayer.get().numberOfBuildings() > 1) {

            int sellID = mRandom.nextInt(mPlayer.get().numberOfBuildings());

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
                mPlayer.get().handleEvent(new SellData(buildingToSell));
            }
            return;
        } else {
            return;
        }
    }
}
