/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.ai.algorithms.AStar;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;
import org.dragonskulle.game.player.ai.algorithms.graphs.Node;

/**
 * An AI player which will aim for a capital of a player. This is done using the A* Algorithm
 *
 * @author Dragonskulle
 */
@Log
public class CapitalAimer extends AiPlayer {

    /** This will hold the path to go */
    private Deque<Integer> mPath = new ArrayDeque<Integer>();

    /** This will hold where we have gone */
    private Deque<Integer> mGone;

    /** This is the graph to traverse */
    private Graph mGraph;

    /** The opponent to attack */
    private Player mOpponent;

    /** The tile which contains the capital */
    private Reference<HexagonTile> mTileToAim;

    public CapitalAimer() {}

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
                buildRandomly();
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

        // This will move us onto our own claimed tiles
        int previousNode = mGone.pop();

        boolean onYourNode = false;
        while (!onYourNode) {
            if (mGraph.getNode(previousNode).getHexTile().get().getClaimant()
                    == null) { // TODO null checks please
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
        while (mGraph.getNode(nextNode).getHexTile().get().getClaimant() != null
                && mGraph.getNode(nextNode)
                                .getHexTile()
                                .get()
                                .getClaimant()
                                .getNetworkObject()
                                .getOwnerId()
                        == mPlayer.get().getNetworkObject().getOwnerId()) {
            mGone.push(nextNode);
            nextNode = mPath.pop();
        }

        // Checks whether to build or to attack
        if (mGraph.getNode(nextNode).getHexTile().get().getClaimant() == null) {
            log.info("Building");
            // BUILD
            HexagonTile tileToBuildOn = mGraph.getNode(nextNode).getHexTile().get();
            mPlayer.get()
                    .getClientBuildRequest()
                    .invoke((d) -> d.setTile(tileToBuildOn)); // TODO Make as close as final as
            // possible
            mGone.push(nextNode);
            return;
        } else if (mGraph.getNode(nextNode).getHexTile().get().getClaimant() != null) {

            log.info("Attacking");
            // ATTACK
            if (!mGraph.getNode(nextNode).getHexTile().get().hasBuilding()) {
                // Assuming that the building is on the next node

                mGone.push(nextNode);
                nextNode = mPath.pop();
            }
            Building toAttackCheck = mGraph.getNode(nextNode).getHexTile().get().getBuilding();
            if (toAttackCheck == null) {
            	if (mGraph.getNode(nextNode).getHexTile().get().getClaimant() == null) {
            		HexagonTile tileToBuildOn = mGraph.getNode(nextNode).getHexTile().get();
            		mPlayer.get().getClientBuildRequest().invoke((d) -> d.setTile(tileToBuildOn));
            	}
            	else {
            		nextNode = mGone.pop();
            		toAttackCheck = mGraph.getNode(nextNode).getHexTile().get().getBuilding();
            	}
            }

            for (Building attacker : toAttackCheck.getAttackableBuildings()) {

                if (attacker.getOwnerId() == mPlayer.get().getNetworkObject().getOwnerId()) {

                	Building toAttack = mGraph.getNode(nextNode).getHexTile().get().getBuilding();
                    mPlayer.get()
                            .getClientAttackRequest()
                            .invoke(d -> d.setData(attacker, toAttack));
                    if (mGraph.getNode(nextNode).getHexTile().get().getClaimant() != null
                            && mGraph.getNode(nextNode)
                                            .getHexTile()
                                            .get()
                                            .getClaimant()
                                            .getNetworkObject()
                                            .getOwnerId()
                                    == mPlayer.get().getNetworkObject().getOwnerId()) {
                        mGone.push(nextNode);
                    }
                    return;
                }
            }
        }
    }

    private void buildRandomly() {

        Random random = new Random();
        int index = random.nextInt(mPlayer.get().getNumberOfOwnedBuildings());
        final int END = index;

        // Goes through the ownedBuildings
        while (true) {
            ArrayList<Reference<Building>> buildings = mPlayer.get().getOwnedBuildings();

            // Checks the list is valid
            if (Reference.isValid(buildings.get(index))
                    && buildings.get(index).get().getViewableTiles().size() != 0) {

                Building building = buildings.get(index).get();
                // Check
                if (building.getViewableTiles().size() != 0) {

                    // Get the visible tiles
                    List<HexagonTile> visibleTiles =
                            new ArrayList<HexagonTile>(building.getViewableTiles());
                    int j = random.nextInt(visibleTiles.size());
                    final int END2 = j;

                    // Checks if we can use one of the tiles to build from
                    while (true) {
                        HexagonTile tile = visibleTiles.get(j);
                        if (tile.isClaimed() == false && tile.hasBuilding() == false) {
                            mPlayer.get().getClientBuildRequest().invoke((d) -> d.setTile(tile));
                            return;
                        }
                        j++;
                        if (j >= visibleTiles.size()) {
                            j = 0;
                        }
                        if (j == END2) {
                            return;
                        }
                    }
                }
                index++;

                // If gone over start at 0
                if (index >= mPlayer.get().getNumberOfOwnedBuildings()) {
                    index = 0;
                }

                // Checks if we've gone through the whole list
                if (index == END) {
                    return;
                }
            }
        }
    }

    /** This will find the capital node for both you and the opponent */
    private Node[] findCapital(Graph graph, Player opponentPlayer) {
        Integer[] nodesInGraph = graph.getNodes();

        Node[] capitals = new Node[2];
        // Go through all nodes to find the capital
        for (int nodeNumber : nodesInGraph) {
            Node node = graph.getNode(nodeNumber);

            if (node.getHexTile().get().getClaimant() != null
                    && node.getHexTile().get().getBuilding() != null
                    && node.getHexTile().get().getBuilding().isCapital()) {
                if (node.getHexTile().get().getClaimantId()
                        == opponentPlayer.getNetworkObject().getOwnerId()) {
                    capitals[0] = node;
                } else if (node.getHexTile().get().getClaimantId()
                        == mPlayer.get().getNetworkObject().getOwnerId()) {
                    capitals[1] = node;
                }
            }
        }
        return capitals;
    }

    /** This will get the tile which needs to be aimed for */
    private Reference<HexagonTile> getTile(Player opponentPlayer) {

        boolean foundTile =
                mPlayer.get()
                        .getMap()
                        .getAllTiles()
                        .anyMatch(
                                tile -> {
                                    if (tile.getBuilding() != null
                                            && tile.getBuilding().isCapital()
                                            && tile.getClaimantId()
                                                    == opponentPlayer
                                                            .getNetworkObject()
                                                            .getOwnerId()) {
                                        mTileToAim = new Reference<HexagonTile>(tile);
                                        return true;
                                    } else {
                                        return false;
                                    }
                                });

        if (!foundTile) {
            log.severe("We have a serious problem");
            return null;
        } else {
            return mTileToAim;
        }
    }

    /** This will set the opponent to aim for */
    private Player findOpponent() {

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

    /** This will perform the A* Search and all related operations to it */
    private void aStar() {

        // Will find the opponent to attack
        log.info("Changing opponent");
        Player opponentPlayer = findOpponent();

        if (opponentPlayer != null) {
            // Will find the tile to attack
            Reference<HexagonTile> tileToAim = getTile(opponentPlayer);
            log.info("Found Tile");
            // Creates a graph
            Graph graph =
                    new Graph(
                            mPlayer.get().getMap(),
                            mPlayer.get().getNetworkObject().getOwnerId(),
                            tileToAim.get());

            mGraph = graph;
            // Finds the capitals
            Node[] capitals = findCapital(graph, opponentPlayer);

            Node capNode = capitals[1];
            Node oppCapNode = capitals[0];

            if (capNode == null || oppCapNode == null) {

                // Null pointer check to try and not destroy the server
                mPath = new ArrayDeque<Integer>();
                return;
            }
            // Performs A* Search
            AStar aStar = new AStar(graph);
            aStar.aStarAlgorithm(capNode.getNode(), oppCapNode.getNode());
            log.severe("Completed");

            mPath = aStar.getAnswerOfNodes();

            // TODO Testing - remove before PR
            String answer = "";
            if (mPath.size() == 0) {
                log.severe("HOWWWWW");
            }
            for (int node : mPath) {
                answer = answer + node + " ->";
            }
            log.info(answer);

            mGone = new ArrayDeque<Integer>();
        } else {
            mPath = new ArrayDeque<Integer>();
        }
    }
}
