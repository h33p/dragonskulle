/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
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
public class AiPlayer extends Component implements IFixedUpdate, IOnStart {

    /** The time since the last check if the AI player can play. (Start at 0) */
    protected float mTimeSinceStart;
    /** The lower bound for the random number to choose a time */
    protected int mLowerBoundTime = 1;
    /** The upper bound for the random number to choose a time */
    protected int mUpperBoundTime = 2;
    /** Will hold how long the AI player has to wait until playing */
    protected int mTimeToWait;

    protected Reference<Player> mPlayer;

    protected Random mRandom = new Random();

    /** Will choose whether to place a building or to use the building. */
    protected float mTileProbability = (float) 1.0;

    protected float mBuildingProbability = 1 - mTileProbability;

    /** Choose what to do with the building -- These 3 must sum to 1 */
    protected float mUpgradeBuilding = (float) 0.2;

    protected float mAttackBuilding = (float) 0.7;
    protected float mSellBuilding = (float) 0.1;

    /** A Constructor for an AI Player */
    public AiPlayer() {}

    @Override
    public void onStart() {

        // Sets up all unitialised variables
        mPlayer = getGameObject().getComponent(Player.class);
        mTimeSinceStart = 0;
        createNewRandomTime();
    }

    /**
     * This will check to see whether the AI Player can actually play or not
     *
     * @param deltaTime The time since the last fixed update
     * @return A boolean to say whether the AI player can play
     */
    protected boolean shouldPlayGame(float deltaTime) {
        mTimeSinceStart += deltaTime;

        // Checks to see how long since last time AI player played and if longer than how long they
        // have to wait
        if (mTimeSinceStart >= mTimeToWait) {
            mTimeSinceStart = 0;
            createNewRandomTime(); // Creates new Random Number until next move
            return true;
        }

        return false;
    }

    /** This will set how long the AI player has to wait until they can play */
    protected void createNewRandomTime() {
        mTimeToWait = mRandom.nextInt() % (mUpperBoundTime + 1 - mLowerBoundTime) + mLowerBoundTime;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {

        // If you can play simulate the input
        if (shouldPlayGame(deltaTime)) {
            log.info("Playing game");
            simulateInput();
        }
    }

    /**
     * This will simulate the action to be done by the AI player. For the base class this will be
     * done using probability
     */
    private void simulateInput() {

        if (mPlayer.get().numberOfBuildings() == 0) {
            log.info("AI: I have " + mPlayer.get().numberOfBuildings() + " buildings.");

            int min = -10;
            int max = 10;

            int posX = min + (int) (Math.random() * ((max - min) + 1));
            int posY = min + (int) (Math.random() * ((max - min) + 1));

            HexagonTile tile = mPlayer.get().getMapComponent().getTile(posX, posY);

            if (tile == null) {
                return;
            }

            // log.info("Selected tile: " + tile);
            // log.info("Building on tile: " + tile.getBuilding());

            if (tile.getBuilding() == null) {
                // Send to server
                mPlayer.get().handleEvent(new BuildData(tile));
            }

            return;
        }

        // If only one building assumed that its capital
        if (mPlayer.get().numberOfBuildings() == 1) { // TODO Refactor it so it's only done once

            log.info(
                    "AI: I have "
                            + mPlayer.get().numberOfBuildings()
                            + " buildings. Should be one");

            // Gets all the tiles it can expand to
            List<HexagonTile> tilesToUse = hexTilesToExpand();

            // log.info("tilesToUse: " + tilesToUse);

            // Checks if there are tiles
            if (tilesToUse.size() != 0) {

                log.info("AI: Placing building cos only have capital");
                // Picks a random number thus a random tile
                int randomIndex = mRandom.nextInt(tilesToUse.size());
                HexagonTile tileToExpandTo = tilesToUse.get(randomIndex);

                // Send to server
                mPlayer.get().handleEvent(new BuildData(tileToExpandTo));
                return;
            } else {
                return; // end
            }

        } else {
            log.info(
                    "AI: I have "
                            + mPlayer.get().numberOfBuildings()
                            + " buildings. Shoudl be  more than  one");
            // Pick a random number to choose whether to place a building or to use a building
            float randomNumber = mRandom.nextFloat();

            // Choose to place a building
            if (randomNumber <= mTileProbability) {

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

                // Choose to do something with a building
            } else {
                // Pick a random number to choose whether to place a building or to use a building
                randomNumber = mRandom.nextFloat();

                // Choose to upgrade a building
                if (randomNumber <= mUpgradeBuilding) {

                    int upgradeID = mRandom.nextInt(mPlayer.get().numberOfBuildings());

                    log.info("AI: Upgrading");

                    Building buildingToUpgrade =  	//TODO This won't work due to mOwnedBuildings not being updated
                            mPlayer.get()
                                    .getOwnedBuildings()
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

                    // Choose to attack
                } else if (randomNumber > mUpgradeBuilding
                        && randomNumber <= mAttackBuilding + mUpgradeBuilding) {

                    log.info("AI: Attacking");
                    ArrayList<Building[]> buildingsToAttack = new ArrayList<Building[]>();

                    // Will create a list of [attacker (your building), defender (building to
                    // attack)]
                    mPlayer.get()
                            .getOwnedBuildings()  			//TODO This won't work due to mOwnedBuildings not being updated
                            .filter(Reference::isValid)
                            .map(Reference::get)
                            .forEach(
                                    b -> {
                                        List<Building> attackableBuildings =
                                                b.getAttackableBuildings();
                                        for (Building buildingWhichCouldBeAttacked :
                                                attackableBuildings) {
                                            Building[] listToAdd = {
                                                b, buildingWhichCouldBeAttacked
                                            };

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
                        mPlayer.get()
                                .handleEvent(
                                        new AttackData(buildingToAttack[0], buildingToAttack[1]));

                        return;
                    } else {
                        return;
                    }

                    // Choose to sell a building
                } else {
                    log.info("AI: Selling");
                    if (mPlayer.get().numberOfBuildings() > 1) {

                        int sellID = mRandom.nextInt(mPlayer.get().numberOfBuildings());

                        Building buildingToSell =
                                mPlayer.get()
                                        .getOwnedBuildings()			//TODO This won't work due to mOwnedBuildings not being updated
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
                .getOwnedBuildings()				//TODO This won't work due to mOwnedBuildings not being updated
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(
                        building -> {
                            List<HexagonTile> hexTilesWhichCanBeSeen = building.getViewableTiles();
                            // log.info("hexTilesWhichCanBeSeen: " + hexTilesWhichCanBeSeen);

                            // Check each tile is valid
                            for (HexagonTile hexTile : hexTilesWhichCanBeSeen) {

                                if (hexTile.getBuilding() != null) {
                                    // log.info("Already building at location."); // Ignore cos
                                    // theres already a building there
                                } else if (!checkCloseBuildings(hexTile)) {
                                    // log.info("Building within 1 hex."); // Ignore cos theres
                                    // already a building there; // IGNORE TILE IT'S WITHIN 1 HEX
                                } // Can add extra checks here.
                                else {
                                    hexTilesToExpand.add(hexTile);
                                    // log.info("Good tile:" + hexTile);
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
}
