/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;
import java.util.stream.Stream;
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
public class CapitalAimer extends ProbabilisticAiPlayer {

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

    /** Random Creator */
    private Random mRandom = new Random();

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

        if (mRandom.nextFloat() < 0.9) {
            // This will move us onto our own claimed tiles
            if (mRandom.nextFloat() < 0.6) {
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
                super.upgradeBuilding();
            }
        } else {
            super.simulateInput();
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
    private Reference<HexagonTile> getTileCapital(Player opponentPlayer) {

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

            if (mRandom.nextFloat() < 0.01) {
                aimCapital(opponentPlayer);
            } else {
                aimBuilding(opponentPlayer);
            }
        } else {
            mPath = new ArrayDeque<Integer>();
        }
    }

    /**
     * This will aim for a capital
     *
     * @param opponentPlayer The {@code Player} to aim for
     */
    private void aimCapital(Player opponentPlayer) {
        // Will find the tile to attack
        Reference<HexagonTile> tileToAim = getTileCapital(opponentPlayer);
        log.info("Found Tile");

        if (Reference.isValid(tileToAim)) {
            // Creates a graph
            Graph graph = new Graph(mPlayer.get().getMap(), tileToAim.get());

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

    /**
     * This will aim for a building
     *
     * @param opponentPlayer The {@code Player} to aim for
     */
    private void aimBuilding(Player opponentPlayer) {
        if (opponentPlayer != null) {
            // Will find the tile to attack
            Reference<HexagonTile> tileToAim = getTileBuilding(opponentPlayer);
            log.info("Found Tile");

            if (Reference.isValid(tileToAim)) {
                // Creates a graph
                Graph graph =
                        new Graph(
                                mPlayer.get().getMap(), // TODO Change to stream
                                tileToAim.get(),
                                this);

                mGraph = graph;
                // Finds the buildings
                Node[] buildings = findBuilding(graph, opponentPlayer);

                Node capNode = buildings[1];
                Node oppNode = buildings[0];

                if (capNode == null || oppNode == null) {

                    // Null pointer check to try and not destroy the server
                    mPath = new ArrayDeque<Integer>();
                    return;
                }
                // Performs A* Search
                AStar aStar = new AStar(graph);
                aStar.aStarAlgorithm(capNode.getNode(), oppNode.getNode());
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
        } else {
            mPath = new ArrayDeque<Integer>();
        }
    }

    /**
     * Get a tile to aim for
     *
     * @param opponentPlayer The {@code Player} to aim for
     * @return The {@code HexagonTile} to aim for
     */
    private Reference<HexagonTile> getTileBuilding(Player opponentPlayer) {

        boolean foundTile =
                getStream()
                        .anyMatch(
                                tile -> {
                                    if (tile.getBuilding() != null
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

    /**
     * This will convert the visible tiles into a stream
     *
     * @return The visible {@code HexagonTile}'s as a {@code Stream}
     */
    public Stream<HexagonTile> getStream() {
        LinkedList<HexagonTile> allVisibleTiles = new LinkedList<HexagonTile>();

        for (int i = 0; i < mPlayer.get().getNumberOfOwnedBuildings(); i++) {
            Building building = mPlayer.get().getOwnedBuildings().get(i).get();

            for (HexagonTile tile : building.getViewableTiles()) {
                allVisibleTiles.add(tile);
            }
        }

        return Arrays.stream(allVisibleTiles.toArray(new HexagonTile[0]));
    }

    /** This will find the Building node for both your opponent and the capital for you */
    private Node[] findBuilding(Graph graph, Player opponentPlayer) {
        Integer[] nodesInGraph = graph.getNodes();

        Node[] buildings = new Node[2];
        // Go through all nodes to find the capital
        for (int nodeNumber : nodesInGraph) {
            Node node = graph.getNode(nodeNumber);

            if (node.getHexTile().get().getClaimant() != null
                    && node.getHexTile().get().getBuilding() != null) {
                if (node.getHexTile().get().getQ() == mTileToAim.get().getQ()
                        && node.getHexTile().get().getQ() == mTileToAim.get().getQ()
                        && node.getHexTile().get().getClaimantId()
                                == opponentPlayer.getNetworkObject().getOwnerId()) {
                    buildings[0] = node;
                } else if (node.getHexTile().get().getBuilding().isCapital()
                        && node.getHexTile().get().getClaimantId()
                                == mPlayer.get().getNetworkObject().getOwnerId()) {
                    buildings[1] = node;
                }
            }
        }
        return buildings;
    }
}
