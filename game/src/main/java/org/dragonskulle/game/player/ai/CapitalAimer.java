/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Time;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.ai.algorithms.AStar;
import org.dragonskulle.game.player.ai.algorithms.exceptions.GraphNodeException;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;
import org.dragonskulle.game.player.ai.algorithms.graphs.Node;

/**
 * An AI player which will aim for a capital of a player.  This is done using the A* Algorithm
 *
 * @author Dragonskulle
 */
@Log
public class CapitalAimer extends AiPlayer {

    private Player mOpponentPlayer = mPlayer.get();
    private final float UPDATE_PATH_TIME = 60;
    private float mTimeSinceLastCheck = Time.getTimeInSeconds() - UPDATE_PATH_TIME;
    private Deque<Integer> mPath;
    private Deque<Integer> mGone;
    private Graph mGraph = null;
    private Node mCapNode = null;
    private Node mOppCapNode = null;
    private Reference<HexagonTile> mTileToAim = new Reference<HexagonTile>(null);

    public CapitalAimer() {};

    @Override
    protected void simulateInput() {
        if (mOpponentPlayer.getNetworkObject().getOwnerId()
                == mPlayer.get().getNetworkObject().getOwnerId()) {
            findOpponent();
            mTileToAim = new Reference<HexagonTile>(null);
            getTile();
        }

        if (mTimeSinceLastCheck + UPDATE_PATH_TIME > Time.getTimeInSeconds()) {
            aStar();
        }

        if (mPath.size() == 0) {
            mOpponentPlayer = mPlayer.get();
            return;
        }

        if (mGone.size() == 0) {
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
        }

        int previousNode = mGone.pop();

        boolean onYourNode = false;
        while (!onYourNode) {
            if (mGraph.getNode(previousNode).getHexTile().get().getClaimant() == null) {
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
        int nextNode = mPath.pop();
        while (mGraph.getNode(nextNode)
                        .getHexTile()
                        .get()
                        .getClaimant()
                        .getNetworkObject()
                        .getOwnerId()
                == mPlayer.get().getNetworkObject().getOwnerId()) {
            mGone.push(nextNode);
            nextNode = mPath.pop();
        }

        if (mGraph.getNode(nextNode).getHexTile().get().getClaimant() == null) {
            // BUILD
            HexagonTile tileToBuildOn = mGraph.getNode(nextNode).getHexTile().get();
            mPlayer.get()
                    .getClientBuildRequest()
                    .invoke((d) -> d.setTile(tileToBuildOn)); // TODO Make as close as final as
            // possible
            mGone.push(nextNode);
        } else if (mGraph.getNode(nextNode).getHexTile().get().getClaimant() != null) {
            // ATTACK
            if (!mGraph.getNode(nextNode).getHexTile().get().hasBuilding()) {
                // Assuming that the building is on the next node
                mGone.push(nextNode);
                nextNode = mPath.pop();
            }
            Building toAttack = mGraph.getNode(nextNode).getHexTile().get().getBuilding();
            for (Building attacker : toAttack.getAttackableBuildings()) {
                if (attacker.getOwnerID() == mPlayer.get().getNetworkObject().getOwnerId()) {
                    mPlayer.get()
                            .getClientAttackRequest()
                            .invoke(
                                    d ->
                                            d.setData(
                                                    attacker,
                                                    toAttack)); // TODO Make as close as final as
                    // possible
                    mGone.push(nextNode);
                }
            }
        }
        /*TODO
         * To do this I need to have 2 Deques - one with the tiles which I have done (Must be a stack)
         * The second one is the next ones to use
         * Check if the next tile is claimed in the deque -- if it isn't claim (either attack/build)
         * If it is put it in the claimed deque.
         * Everytime you redo A* it needs to be reset
         * The start node is always OUR capital.
         *
         */

    }

    /** 
     * This will find the capital node for both you and the opponent 
    */
    private void findCapital() {
        Integer[] nodesInGraph = mGraph.getNodes();

        for (int nodeNumber : nodesInGraph) {
            Node node = mGraph.getNode(nodeNumber);
            if (node.getHexTile().get().getClaimant() == mOpponentPlayer
                    && node.getHexTile().get().getBuilding().isCapital()) {
                mOppCapNode = node;
            } else if (node.getHexTile().get().getClaimant() == mPlayer.get()
                    && node.getHexTile().get().getBuilding().isCapital()) {
                mCapNode = node;
            }
        }
    }

    /**
     * This will get the tile which needs to be aimed for
     */
    private void getTile() {
        Stream<HexagonTile> tiles = mPlayer.get().getMap().getAllTiles();
        while (mTileToAim.get() == null) {
            Optional<HexagonTile> tile = tiles.findAny();
            HexagonTile tileFound = tile.get();
            if (tileFound.getBuilding() != null && tileFound.getBuilding().isCapital()) {
                mTileToAim = new Reference<HexagonTile>(tileFound);
            }
        }
    }

    /** 
     * This will set the opponent to aim for 
    */
    private void findOpponent() {
        Stream<HexagonTile> tiles = mPlayer.get().getMap().getAllTiles();

        while (mOpponentPlayer.getNetworkObject().getOwnerId()
                == mPlayer.get().getNetworkObject().getOwnerId()) {
            Optional<HexagonTile> tile = tiles.findAny();
            HexagonTile tileFound = tile.get();
            Player playerAiming = tileFound.getClaimant();
            if (playerAiming != null) {
                mOpponentPlayer = playerAiming;
            }
        }
    }

    /** 
     * This will perform the A* Search and all related operations to it
    */
    private void aStar() {
        Graph tempGraph =
                new Graph(
                        mPlayer.get().getMap(),
                        mPlayer.get().getNetworkObject().getOwnerId(),
                        mTileToAim
                                .get()); 
        mGraph = tempGraph;
        AStar aStar = new AStar(mGraph);
        findCapital();
        try {
            aStar.aStarAlgorithm(mCapNode.getNode(), mOppCapNode.getNode());
        } catch (GraphNodeException e) {
            // TODO Shouldn't get here.
            log.severe("EXCEPTION");
        }
        mPath = aStar.getAnswerOfNodes();
        mGone = new ArrayDeque<Integer>();
    }
}
