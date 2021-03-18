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
    protected int mLowerBoundTime = 5;
    /** The upper bound for the random number to choose a time */
    protected int mUpperBoundTime = 10;
    /** Will hold how long the AI player has to wait until playing */
    protected int mTimeToWait;

    protected Reference<Player> mPlayer;

    protected Random mRandom = new Random();

    /** Will choose whether to place a building or to use the building. */
    protected float mTileProbability = (float) 0.5;

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
    protected boolean playGame(float deltaTime) {
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
        do {

            // Creates a time up to the upper bound
            mTimeToWait = mRandom.nextInt(mUpperBoundTime + 1);
        } while (mTimeToWait < mLowerBoundTime); // If lower than lower bound redo.
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {

        // If you can play simulate the input
        if (playGame(deltaTime)) {
            log.info("Playing game");
            simulateInput();
        }
    }

    /**
     * This will simulate the action to be done by the AI player. For the base class this will be
     * done using probability
     */
    private void simulateInput() {

        // If only one building assumed that its capital
        if (mPlayer.get().numberOfBuildings() == 1) { // TODO Refactor it so it's only done once

            // Gets all the tiles it can expand to
            List<HexagonTile> tilesToUse = hexTilesToExpand();

            // Checks if there are tiles
            if (tilesToUse.size() != 0) {

                log.info("AI: Placing building cos only have capital");
                // Picks a random number thus a random tile
                int randomIndex = mRandom.nextInt(tilesToUse.size());
                HexagonTile tileToExpandTo = tilesToUse.get(randomIndex);

                // Send to server
                mPlayer.get().mClientBuildRequest.invoke(new BuildData(tileToExpandTo));
                return;
            } else {
                return; // end
            }

        } else {
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
                    mPlayer.get().mClientBuildRequest.invoke(new BuildData(tileToExpandTo));
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

                    log.info("AI: Upgrading");
                    
                    if(mPlayer.get().numberOfBuildings() <= 0) {
                    	log.info("AI: Has no buildings.");
                    	return;
                    }
                    
                    // Get the building to upgrade
                    Reference<Building> building =
                            mPlayer.get()
                                    .getBuilding(
                                            mRandom.nextInt(mPlayer.get().numberOfBuildings()));

                    // Get Stat to upgrade
                    ArrayList<SyncStat<?>> statsArray = building.get().getStats();
                    SyncStat<?> statToUpgrade = statsArray.get(mRandom.nextInt(statsArray.size()));

                    // Send to server
                    mPlayer.get()
                            .mClientStatRequest
                            .invoke(new StatData(building.get(), statToUpgrade));
                    return;

                    // Choose to attack
                } else if (randomNumber > mUpgradeBuilding
                        && randomNumber <= mAttackBuilding + mUpgradeBuilding) {

                    log.info("AI: Attacking");
                    ArrayList<Building[]> buildingsToAttack = new ArrayList<Building[]>();

                    // Will create a list of [attacker (your building), defender (building to
                    // attack)]
                    for (int i = 0; i < mPlayer.get().numberOfBuildings(); i++) {

                        Building building = mPlayer.get().getBuilding(i).get();

                        // Will go through all possible combinations
                        List<Building> attackableBuildings = building.getAttackableBuildings();
                        for (Building buildingWhichCouldBeAttacked : attackableBuildings) {
                            Building[] listToAdd = {building, buildingWhichCouldBeAttacked};

                            buildingsToAttack.add(listToAdd);
                        }
                    }

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
                                .mClientAttackRequest
                                .invoke(new AttackData(buildingToAttack[0], buildingToAttack[1]));

                        return;
                    } else {
                        return;
                    }

                    // Choose to sell a building
                } else {

                    log.info("AI: Selling");
                    if (mPlayer.get().numberOfBuildings() > 1) {

                        // Pick a building to sell
                        Building buildingToSell =
                                mPlayer.get()
                                        .getBuilding(
                                                mRandom.nextInt(mPlayer.get().numberOfBuildings()))
                                        .get();

                        // TODO - Make sure its not the capital

                        // Now have building to sell
                        mPlayer.get().mClientSellRequest.invoke(new SellData(buildingToSell));
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

        // Create a list of all hex tiles to expand to
        for (int i = 0; i < mPlayer.get().numberOfBuildings(); i++) {

            // Checks each building and checks tiles around
            Building building = mPlayer.get().getBuilding(i).get();
            List<HexagonTile> hexTilesWhichCanBeSeen = building.getViewableTiles();

            // Check each tile is valid
            for (HexagonTile hexTile : hexTilesWhichCanBeSeen) {

                if (mPlayer.get().getMapComponent().get().getTile(hexTile.getR(), hexTile.getQ())
                        != null) {; // Ignore cos theres already a building there
                } else if (!checkCloseBuildings(hexTile)) {; // IGNORE TILE IT'S WITHIN 1 HEX
                }

                // Can add extra checks here.
                else {
                    hexTilesToExpand.add(hexTile);
                }
            }
        }
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
            if (mPlayer.get().getMapComponent().get().getBuilding(tile.getQ(), tile.getR())
                    != null) {
                return false;
            }
        }

        return true;
    }
}
