/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Random;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;

/**
 * This defines how some AI will play if they are aiming for either the Capital or a Building
 *
 * @author DragonSkulle
 */
@Log
public abstract class Aimer extends ProbabilisticAiPlayer {

    /** This will hold the path to go */
    protected Deque<Integer> mPath = new ArrayDeque<Integer>();

    /** This will hold where we have gone */
    protected Deque<Integer> mGone;

    /** This is the graph to traverse */
    protected Graph mGraph;

    /** The opponent to attack */
    protected Player mOpponent;

    /** The tile which contains the capital */
    protected Reference<HexagonTile> mTileToAim;

    public Aimer() {}

    @Override
    protected void simulateInput() {

        // Checks if we have reached the capital
        if (mPath.size() == 0) {

            // Will perform all necessary checks for A*
            aStar();
            log.severe("A* Ran");
            // Checks if path size is 0
            if (mPath.size() == 0) {
                // Will build randomly
                super.simulateInput();
                return;
            }
            return;
        }

        // Checks if we are at our capital.  If so will build one on
        if (mGone.size() == 0) {
            log.info("mGone == 0");
            int nextDoor = mPath.pop();
            mGone.push(nextDoor);
            int firstElement = mPath.pop();
            if (mGraph.getNode(firstElement)
                            .getHexTile()
                            .get()
                            .getClaimant()
                            .getNetworkObject()
                            .getOwnerId()
                    == mPlayer.get().getNetworkObject().getOwnerId()) {
                mGone.push(firstElement);
            } else {
                log.severe("You might be dead");
            }
            return;
        }

        Random random = new Random();

        if (random.nextFloat() < 0.9) {
            // This will move us onto our own claimed tiles

            if (random.nextFloat() < 0.5) {
                // ATTACK
                int previousNode = mGone.pop();

                boolean onYourNode = false;
                while (!onYourNode) {
                    if (Reference.isValid(mGraph.getNode(previousNode).getHexTile())
                            && mGraph.getNode(previousNode).getHexTile().get().getClaimant()
                                    == null) {
                        mPath.push(previousNode);
                        previousNode = mGone.pop();
                    } else if (mGraph.getNode(previousNode)
                                    .getHexTile()
                                    .get()
                                    .getClaimant()
                                    .getNetworkObject()
                                    .getOwnerId()
                            != mPlayer.get().getNetworkObject().getOwnerId()) {
                        mPath.push(previousNode);
                        previousNode = mGone.pop();
                    } else {
                        onYourNode = true;
                    }
                }

                mGone.push(previousNode);

                // This will point us to the next tile to use
                int nextNode = mPath.pop();
                HexagonTile nextTile = mGraph.getNode(nextNode).getHexTile().get();
                Player nextTilePlayer = nextTile.getClaimant();
                while (nextTile != null
                        && nextTilePlayer != null
                        && nextTilePlayer.getNetworkObject().getOwnerId()
                                == mPlayer.get().getNetworkObject().getOwnerId()) {
                    mGone.push(nextNode);
                    nextNode = mPath.pop();
                    nextTile = mGraph.getNode(nextNode).getHexTile().get();
                    nextTilePlayer = nextTile.getClaimant();
                }

                // Checks whether to build or to attack
                if (nextTilePlayer == null) {
                    log.info("Building");
                    HexagonTile tileToBuildOn = nextTile;
                    // BUILD
                    mPlayer.get().getClientBuildRequest().invoke((d) -> d.setTile(tileToBuildOn));
                    mGone.push(nextNode);
                    return;
                } else if (nextTilePlayer != null) {

                    log.info("Attacking");
                    // ATTACK
                    if (!nextTile.hasBuilding()) {
                        // Assuming that the building is on the next node

                        mGone.push(nextNode);
                        nextNode = mPath.pop();
                        nextTile = mGraph.getNode(nextNode).getHexTile().get();
                        nextTilePlayer = nextTile.getClaimant();
                    }

                    // Checks if building exists
                    Building toAttackCheck = nextTile.getBuilding();
                    if (toAttackCheck == null) {
                        // Checks if we have to build
                        if (nextTilePlayer == null) {

                            HexagonTile tileToBuildOn = nextTile;
                            mPlayer.get()
                                    .getClientBuildRequest()
                                    .invoke((d) -> d.setTile(tileToBuildOn));
                            mGone.push(nextNode);
                            return;

                        } else {
                            Building building = nextTile.getBuilding();
                            // Will attack instead and remove
                            while (building == null) {
                                mPath.push(nextNode);
                                nextNode = mGone.pop();
                                nextTile = mGraph.getNode(nextNode).getHexTile().get();
                                nextTilePlayer = nextTile.getClaimant();
                                building = nextTile.getBuilding();
                            }
                            super.tryToAttack(building);

                            // Assumption is that the code at the start of the method will move back
                            // to
                            // correct postion
                            mPath.push(nextNode);

                            return;
                        }
                    }

                    for (Building attacker : toAttackCheck.getAttackableBuildings()) {

                        if (attacker.getOwnerId()
                                == mPlayer.get().getNetworkObject().getOwnerId()) {

                            Building toAttack = nextTile.getBuilding();
                            mPlayer.get()
                                    .getClientAttackRequest()
                                    .invoke(d -> d.setData(attacker, toAttack));
                            if (nextTilePlayer != null
                                    && nextTilePlayer.getNetworkObject().getOwnerId()
                                            == mPlayer.get().getNetworkObject().getOwnerId()) {
                                mGone.push(nextNode);
                            }
                            return;
                        }
                    }
                }
            } else {
                // DEFEND by adding building
                super.addBuilding();
            }
        } else {
            super.simulateInput();
        }
    }

    /** This will set the opponent to aim for */
    protected Player findOpponent() {

        ArrayList<Reference<Building>> ownedBuildings = mPlayer.get().getOwnedBuildings();
        for (Reference<Building> building : ownedBuildings) {
            if (Reference.isValid(building)) {
                ArrayList<Building> visibleTiles = building.get().getAttackableBuildings();

                if (visibleTiles.size() != 0) {
                    Building buildingToAim = visibleTiles.get(0);
                    return buildingToAim.getOwner();
                }
            }
        }
        return null;
    }

    protected abstract void aStar();
}
