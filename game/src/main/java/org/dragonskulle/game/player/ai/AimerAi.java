/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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
 * An AI player which will aim for a {@code Building} of a player when viewed. This is done using
 * the A* Algorithm
 *
 * @author Dragonskulle
 */
@Log
public class AimerAi extends ProbabilisticAiPlayer {

    /**
     * A Class which holds the Goal Node and Start Node
     *
     * @author DragonSkulle
     */
    private class ImportantNodes {

        private Node goalNode;
        private Node startNode;
    }

    /** This will hold the path to go */
    private Deque<Integer> mPath = new ArrayDeque<Integer>();

    /** This will hold where we have gone */
    private Deque<Integer> mGone;

    /** This is the graph to traverse */
    private Graph mGraph;

    /** Random Creator */
    private Random mRandom = new Random();

    /** Whether to use the A* route */
    private final float PLAY_A_STAR = 0.9f;

    /** Whether to attack or to upgrade a building */
    private final float GO_DOWN_PATH = .6f;

    /** The chance to aim at a capital */
    private final float AIM_AT_CAPITAL = 0.01f;

    /** Basic Constructor */
    public AimerAi() {}

    @Override
    protected void simulateInput() {

        if (getPlayer() == null) {
            return;
        }

        // Checks if we have reached the capital
        if (mPath.size() == 0) {

            // Will perform all necessary checks for A*
            aStar();
            log.info("A* Ran");

            // Whilst it cannot find a path play probabilistically
            if (mPath.size() == 0) {

                super.simulateInput();
                return;
            }
            return;
        }

        // Checks if we are at our capital.  If so will build one on
        if (mGone.size() == 0) {
            atCapital();
            return;
        }

        // This will choose whether to play as an A* player or as a Probablistic Player
        if (mRandom.nextFloat() < PLAY_A_STAR) {

            // This will choose whether to go attacking and get closer to the building/capital or to
            // upgrade
            if (mRandom.nextFloat() < GO_DOWN_PATH) {

                log.info("Using A*");

                moveBackwards();

                int nextNode = moveForwards();

                if (nextNode == Integer.MAX_VALUE) {
                    return;
                }

                if (Reference.isInvalid(mGraph.getNode(nextNode).getHexTile())) {
                    return;
                }
                HexagonTile nextTile = mGraph.getNode(nextNode).getHexTile().get();

                // Checks whether to build or to attack
                if (nextTile.isClaimed()) {
                    Player nextTilePlayer = nextTile.getClaimant();
                    attack(nextTile, nextTilePlayer, nextNode);
                    return;
                } else {
                    building(nextTile, nextNode);
                    return;
                }

            } else {
                super.upgradeBuilding();
            }
        } else {
            super.simulateInput();
        }
    }

    /**
     * This will attack the next building
     *
     * @param nextTile The next {@code HexagonTile} to aim for
     * @param nextTilePlayer The {@code Player} which owns the next tile
     * @param nextNode The {@code Node} number for the next {@code HexagonTile}
     */
    private void attack(HexagonTile nextTile, Player nextTilePlayer, int nextNode) {
        log.info("Attacking");
        // ATTACK
        if (!nextTile.hasBuilding()) {
            // Assuming that the building is on the next node

            mGone.push(nextNode);
            nextNode = mPath.pop();
            if (Reference.isInvalid(mGraph.getNode(nextNode).getHexTile())) {
                mPath.push(nextNode);
                return;
            }
            nextTile = mGraph.getNode(nextNode).getHexTile().get();
            nextTilePlayer = nextTile.getClaimant();
        }

        // Checks if building exists && Checks if we have to build
        Building building = nextTile.getBuilding();
        if (!nextTile.hasBuilding() && nextTilePlayer == null) {

            building(nextTile, nextNode);
            return;
        }
        if (!nextTile.hasBuilding()) {

            // Will attack instead and remove
            while (!nextTile.hasBuilding()) {
                mPath.push(nextNode);
                if (mGone.size() == 0) {
                    mPath = new ArrayDeque<Integer>();
                    return;
                }
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

        // Checks for attackable buildings
        for (Building attacker : building.getAttackableBuildings()) {

            if (getPlayer().isBuildingOwner(attacker)) {

                // Used so lambdas work
                Building defender = nextTile.getBuilding();
                getPlayer().getClientAttackRequest().invoke(d -> d.setData(attacker, defender));

                // Checks if the attack was successful
                if (nextTilePlayer != null
                        && nextTilePlayer.getNetworkObject().getOwnerId()
                                == getPlayer().getNetworkObject().getOwnerId()) {
                    mGone.push(nextNode);
                }
                return;
            }
        }
    }

    /**
     * This will build the next building
     *
     * @param nextTile The next {@code HexagonTile} to aim for
     * @param nextNode The {@code Node} number for the next {@code HexagonTile}
     */
    private void building(HexagonTile tileToBuildOn, int nextNode) {
        log.info("Building");
        // BUILD
        getPlayer().getClientBuildRequest().invoke((d) -> d.setTile(tileToBuildOn));
        mGone.push(nextNode);
    }

    /** Move the Player forward to the first node not owned by them */
    private int moveForwards() {
        // This will point us to the next tile to use
        int nextNode = mPath.pop();
        if (Reference.isInvalid(mGraph.getNode(nextNode).getHexTile())) {
            mPath.push(nextNode);
            return Integer.MAX_VALUE;
        }
        HexagonTile nextTile = mGraph.getNode(nextNode).getHexTile().get();
        Player nextTilePlayer = nextTile.getClaimant();
        while (nextTile != null
                && nextTilePlayer != null
                && nextTilePlayer.getNetworkObject().getOwnerId()
                        == getPlayer().getNetworkObject().getOwnerId()) {
            mGone.push(nextNode);
            if (mPath.size() == 0) {
                return Integer.MAX_VALUE;
            }
            nextNode = mPath.pop();
            if (Reference.isInvalid(mGraph.getNode(nextNode).getHexTile())) {
                nextTile = null;
                return Integer.MAX_VALUE;
            } else {
                nextTile = mGraph.getNode(nextNode).getHexTile().get();
            }
            nextTilePlayer = nextTile.getClaimant();
        }

        return nextNode;
    }

    /** Move the player to a node owned by them */
    private void moveBackwards() {

        int previousNode = mGone.pop();

        // This will get you to the first node which you claimed
        boolean onYourNode = false;
        while (!onYourNode) {

            Reference<HexagonTile> hexagonTile = mGraph.getNode(previousNode).getHexTile();

            if (Reference.isInvalid(hexagonTile)) {
                mPath.push(previousNode);
                return;
            }

            if (Reference.isValid(hexagonTile) && !hexagonTile.get().isClaimed()) {
                mPath.push(previousNode);
                previousNode = mGone.pop();
            } else if (!getPlayer().isClaimingTile(hexagonTile.get())) {
                mPath.push(previousNode);
                previousNode = mGone.pop();
            } else {
                onYourNode = true;
            }
        }

        mGone.push(previousNode);
    }

    /** This is what happens when we start the search and moves from the capital */
    private void atCapital() {
        log.info("mGone == 0");
        int nextDoor = mPath.pop();
        mGone.push(nextDoor);
        int firstElement = mPath.pop();
        if (Reference.isInvalid(mGraph.getNode(firstElement).getHexTile())) {
            mPath.push(firstElement);
            return;
        }
        if (getPlayer().isClaimingTile(mGraph.getNode(firstElement).getHexTile().get())) {

            mGone.push(firstElement);
        } else {
            log.warning("You might be dead");
        }
        return;
    }

    /**
     * This will find the capital node for both you and the opponent
     *
     * @param graph The {@code Graph} to check through
     * @param opponentPlayer The opponent {@code Player} which you are attacking
     * @return An Array of {@code Node}'s which you start from and end at
     */
    private ImportantNodes findCapital(Graph graph, Player opponentPlayer) {
        Integer[] nodesInGraph = graph.getNodes();

        ImportantNodes capitals = new ImportantNodes();
        // Go through all nodes to find the capital
        for (int nodeNumber : nodesInGraph) {
            Node node = graph.getNode(nodeNumber);
            HexagonTile nodesHexagonTile = node.getHexTile().get();

            // Checks if hexagontile has a capital
            if (nodesHexagonTile.isClaimed()
                    && node.getHexTile().get().getBuilding() != null
                    && node.getHexTile().get().getBuilding().isCapital()) {

                // Checks if the hexagonTiles capital is the opponent players
                if (opponentPlayer.isClaimingTile(node.getHexTile().get())) {
                    capitals.goalNode = node;

                    // Checks if hexagonTiles capital is ours
                } else if (getPlayer().isClaimingTile(node.getHexTile().get())) {
                    capitals.startNode = node;
                }
            }
        }
        return capitals;
    }

    /**
     * This will get the tile which needs to be aimed for
     *
     * @param opponentPlayer The Opponent {@code Player} to aim for
     * @return A {@code Reference} to a {@code HexagonTile}
     */
    private Reference<HexagonTile> getTileCapital(Player opponentPlayer) {

        Building capitalBuilding = opponentPlayer.getCapital();

        if (capitalBuilding == null) {
            log.warning("No Capital on this player");
            return null;
        }

        return new Reference<HexagonTile>(capitalBuilding.getTile());
    }

    /**
     * This will set the opponent to aim for
     *
     * @return The {@code Player} to attack
     */
    private Player findOpponent() {

        List<Reference<Building>> ownedBuildings = getPlayer().getOwnedBuildings();
        if (ownedBuildings.size() == 0) {
            // This has already been checked but hey ho
            return null;
        }
        int index = mRandom.nextInt(ownedBuildings.size());
        final int end = index;

        // Goes through the ownedBuildings
        while (true) {
            Reference<Building> building = ownedBuildings.get(index);

            // Checks the building is valid
            if (Reference.isValid(building)
                    && building.get().getAttackableBuildings().size() != 0) {
                // Check

                ArrayList<Building> attackableBuildings =
                        new ArrayList<Building>(building.get().getAttackableBuildings());
                Building buildingToAim =
                        attackableBuildings.get(mRandom.nextInt(attackableBuildings.size()));
                return buildingToAim.getOwner();
            }
            index++;

            // If gone over start at 0
            if (index >= ownedBuildings.size()) {
                index = 0;
            }

            // Checks if we've gone through the whole list
            if (index == end) {
                return null;
            }
        }
    }

    /** This will perform the A* Search and all related operations to it */
    private void aStar() {

        // Will find the opponent to attack
        log.info("Changing opponent");
        Player opponentPlayer = findOpponent();

        if (opponentPlayer != null) {

            if (mRandom.nextFloat() < AIM_AT_CAPITAL) {
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
            mGraph = new Graph(getPlayer().getMap(), tileToAim.get());

            // Finds the capitals
            ImportantNodes capitals = findCapital(mGraph, opponentPlayer);

            Node capNode = capitals.startNode;
            Node oppCapNode = capitals.goalNode;

            if (capNode == null || oppCapNode == null) {

                // Null pointer check to try and not destroy the server
                mPath = new ArrayDeque<Integer>();
                return;
            }
            // Performs A* Search
            AStar aStar = new AStar(mGraph, capNode.getNodeId(), oppCapNode.getNodeId());

            mPath = aStar.getPath();

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
        if (opponentPlayer == null) {
            mPath = new ArrayDeque<Integer>();
            return;
        }
        // Will find the tile to attack
        Reference<HexagonTile> tileToAim = getTileBuilding(opponentPlayer);
        log.info("Found Tile");

        if (Reference.isInvalid(tileToAim)) {
            mPath = new ArrayDeque<Integer>();
        }

        // Creates a graph
        mGraph =
                new Graph(
                        getPlayer().getMap(), // TODO Change to stream
                        tileToAim.get(),
                        this);
        // Finds the buildings
        ImportantNodes buildings = findBuilding(mGraph, opponentPlayer, tileToAim.get());

        Node capNode = buildings.startNode;
        Node oppNode = buildings.goalNode;

        if (capNode == null || oppNode == null) {

            // Null pointer check to try and not destroy the server
            mPath = new ArrayDeque<Integer>();
            return;
        }
        // Performs A* Search
        AStar aStar = new AStar(mGraph, capNode.getNodeId(), oppNode.getNodeId());
        log.severe("Completed");

        mPath = aStar.getPath();

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
    }

    /**
     * Get a tile to aim for
     *
     * @param opponentPlayer The {@code Player} to aim for
     * @return The {@code HexagonTile} to aim for
     */
    private Reference<HexagonTile> getTileBuilding(Player opponentPlayer) {

        HexagonTile[] tileToAim = new HexagonTile[1];
        // This checks if any of the buildings are the capital
        boolean foundTile =
                getViewableTiles()
                        .anyMatch(
                                tile -> {
                                    if (tile.hasBuilding()
                                            && tile.getBuilding().isCapital()
                                            && opponentPlayer.isClaimingTile(tile)) {
                                        tileToAim[0] = tile;
                                        return true;
                                    } else {
                                        return false;
                                    }
                                });

        // If no capitals are found just select a random building to attack
        if (!foundTile) {
            foundTile =
                    getViewableTiles()
                            .anyMatch(
                                    tile -> {
                                        if (tile.getBuilding() != null
                                                && opponentPlayer.isClaimingTile(tile)) {
                                            tileToAim[0] = tile;
                                            return true;
                                        } else {
                                            return false;
                                        }
                                    });
        }
        if (!foundTile) {
            log.severe("Cannot find a tile to aim for");
            return null;
        } else {
            return new Reference<HexagonTile>(tileToAim[0]);
        }
    }

    /**
     * This will convert the visible tiles into a stream
     *
     * @return The visible {@code HexagonTile}'s as a {@code Stream}
     */
    public Stream<HexagonTile> getViewableTiles() {

        return getPlayer().getViewableTiles();
    }

    /**
     * This will find the Building node for both your opponent and the capital for you
     *
     * @param graph The {@code Graph} to use
     * @param opponentPlayer The {@code Player} to aim for
     * @return An array of {@code Node}s which have the {@code Node} to start from and go to
     */
    private ImportantNodes findBuilding(Graph graph, Player opponentPlayer, HexagonTile target) {
        Integer[] nodesInGraph = graph.getNodes();

        ImportantNodes buildings = new ImportantNodes();
        // Go through all nodes to find the capital
        for (int nodeNumber : nodesInGraph) {
            Node node = graph.getNode(nodeNumber);

            HexagonTile tile = node.getHexTile().get();

            if (tile.hasBuilding()) {
                if (tile.getQ() == target.getQ()
                        && tile.getQ() == target.getQ()
                        && opponentPlayer.isClaimingTile(tile)) {
                    buildings.goalNode = node;
                } else if (tile.getBuilding().isCapital() && getPlayer().isClaimingTile(tile)) {
                    buildings.startNode = node;
                }
            }
        }
        return buildings;
    }

    /**
     * Returns the AI {@code Player}
     *
     * @return The {@code Player} in the {@code Reference}
     */
    private Player getPlayer() {
        if (Reference.isInvalid(mPlayer)) {
            return null;
        }
        return mPlayer.get();
    }
}
