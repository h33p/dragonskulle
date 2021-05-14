/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameConfig.AiAimerConfig;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.PredefinedBuildings;
import org.dragonskulle.game.player.ai.algorithms.AStar;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;
import org.dragonskulle.game.player.ai.algorithms.graphs.Node;

/**
 * An AI player which will aim for a {@link Building} of a player when viewed. This is done using
 * the A* Algorithm.
 *
 * @author Dragonskulle
 */
@Log
public class AimerAi extends ProbabilisticAiPlayer {

    /**
     * A Class which holds the Goal Node and Start Node.
     *
     * @author DragonSkulle
     */
    private class ImportantNodes {

        /** This will be the goal node. */
        private Node mGoalNode;

        /** This will be the start node. */
        private Node mStartNode;
    }

    /** This will hold the path to go. */
    private Deque<Integer> mPath = new ArrayDeque<Integer>();

    /** This will hold where we have gone. */
    private Deque<Integer> mGone;

    /** This is the graph to traverse. */
    private Graph mGraph;

    /** Random Creator. */
    private Random mRandom = new Random();

    /** This states whether we are aiming at the capital. */
    private boolean mCapitalAimer;

    /** This states how many times we have tried to move from the same place. */
    private int mAttempts = 0;

    /** The node we previously were on. */
    private Node mNodePreviouslyOn = null;

    /** The chance to aim at a capital. */
    private float mAimAtCapital = 0.0f;

    /** The number of times we've attempted A*. */
    private int mAStarAttempts = 0;

    /** Basic Constructor. */
    public AimerAi() {}

    @Override
    protected void simulateInput() {

        Player player = getPlayer();
        if (player == null) return;

        // Checks if we have reached the capital
        if (mPath.size() == 0) {

            // Will perform all necessary checks for A*
            aStar();

            // Increase the chance that next time we aim for the capital.
            mAStarAttempts += 1;
            mAimAtCapital =
                    (float)
                            (Math.pow(
                                            2,
                                            ((float) mAStarAttempts
                                                    / getConfig().getAiAimer().getMaxAttempts()))
                                    - 1);

            // Whilst it cannot find a path play probabilistically
            if (mPath.size() == 0) {
                super.simulateInput();
            }

            return;
        }

        // Checks if we are at our capital.  If so will build one on
        if (mGone.size() == 0) {
            atCapital();
            return;
        }

        AiAimerConfig cfg = getConfig().getAiAimer();

        // This will choose whether to play as an A* player or as a Probablistic Player
        if (mRandom.nextFloat() < cfg.getPlayAStar()) {

            moveBackwards();

            int nextNode = moveForwards();

            if (nextNode == Integer.MAX_VALUE) {
                return;
            }

            if (!Reference.isValid(mGraph.getNode(nextNode).getHexTile())) {
                mPath = new ArrayDeque<Integer>();
                return;
            }

            HexagonTile nextTile = mGraph.getNode(nextNode).getHexTile().get();
            // If the AI wants to attack, then try to ensure its not in cooldown.
            if (nextTile.isClaimed() && player.inCooldown()) {
                return;
            }

            if (mGraph.getNode(nextNode) == mNodePreviouslyOn) {
                mAttempts++;
            } else {
                mAttempts = 0;
            }

            // Checks if we have been on this tile for ages
            if (mAttempts > cfg.getTries()) {
                log.fine("All tried depleted.");
                mPath = new ArrayDeque<Integer>();
                return;
            }
            mNodePreviouslyOn = mGraph.getNode(nextNode);
            // Checks whether to build or to attack
            if (nextTile.isClaimed()) {
                attack(nextTile, nextNode);
                return;
            } else {
                build(nextTile, nextNode);
                return;
            }

        } else {
            super.simulateInput();
        }
    }

    /**
     * This will attack the next building.
     *
     * @param nextTile The next {@link HexagonTile} to aim for.
     * @param nextNode The {@link Node} number for the next {@link HexagonTile}.
     */
    private void attack(HexagonTile nextTile, int nextNode) {
        log.fine("A* Attacking");
        Player nextTilePlayer = nextTile.getClaimant();

        // ATTACK
        if (!nextTile.hasBuilding()) {
            // If the building is not on the current node get the next tile
            mGone.push(nextNode);
            if (mPath.size() == 0) {
                return;
            }
            nextNode = mPath.pop();
            if (!Reference.isValid(mGraph.getNode(nextNode).getHexTile())) {
                mPath.push(nextNode);
                return;
            }
            nextTile = mGraph.getNode(nextNode).getHexTile().get();
            nextTilePlayer = nextTile.getClaimant();
        }

        // Get the building on the tile.
        Building building = nextTile.getBuilding();
        if (!nextTile.hasBuilding() && nextTilePlayer == null) {
            // If there is no building here and no one claims build.
            build(nextTile, nextNode);
            return;
        }

        if (!nextTile.hasBuilding()) {
            // The tile has not got a building but it is claimed so there is a building nearby

            // Go Back
            while (!nextTile.hasBuilding()) {
                mPath.push(nextNode);

                // Get the next node which is behind you.
                if (mGone.size() == 0) {

                    return;
                }
                if (mGone.size() == 0) {
                    mPath = new ArrayDeque<Integer>();
                    return;
                }
                nextNode = mGone.pop();

                if (!Reference.isValid(mGraph.getNode(nextNode).getHexTile())) {
                    // Something is wrong so reset everything
                    mPath = new ArrayDeque<Integer>();
                    return;
                }

                // Get the tile and building
                nextTile = mGraph.getNode(nextNode).getHexTile().get();
                nextTilePlayer = nextTile.getClaimant();
                building = nextTile.getBuilding();
            }

            if (building != null && getPlayer().isBuildingOwner(building)) {
                // Will attack
                super.tryToAttack(building);

                // Assumption is that the code at the start of the method will move back
                // to
                // correct position
                mPath.push(nextNode);
            }

            return;
        }

        // This will check for buildings to attack from
        for (Building attacker : building.getAttackableBuildings()) {

            // If the attacker is not ours, continue.
            if (!getPlayer().isBuildingOwner(attacker)) continue;

            // Ensure the player can afford to attack.
            if (attacker.getAttackCost() > getPlayer().getTokens().get()) continue;

            // Used so lambdas work
            final Building defender = nextTile.getBuilding();
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

    /**
     * This will build the next building.
     *
     * @param tileToBuildOn The next {@link HexagonTile} to aim for.
     * @param nextNode The {@link Node} number for the next {@link HexagonTile}.
     */
    private void build(HexagonTile tileToBuildOn, int nextNode) {
        log.fine("A* Building");

        // Get a building type that they can afford.
        BuildingDescriptor option = getRandomBuildingType();
        if (option == null) return;

        // BUILD
        getPlayer()
                .getClientBuildRequest()
                .invoke((d) -> d.setTile(tileToBuildOn, PredefinedBuildings.getIndex(option)));
        mGone.push(nextNode);
    }

    /**
     * Move the Player forward to the first node not owned by them.
     *
     * @return The nodeId which we end up at
     */
    private int moveForwards() {
        // This will point us to the next tile to use
        if (mPath.size() == 0) {
            return Integer.MAX_VALUE;
        }
        int nextNode = mPath.pop();
        if (!Reference.isValid(mGraph.getNode(nextNode).getHexTile())) {
            mPath = new ArrayDeque<Integer>();
            return Integer.MAX_VALUE;
        }
        HexagonTile nextTile = mGraph.getNode(nextNode).getHexTile().get();
        Player nextTilePlayer = nextTile.getClaimant();
        while (nextTilePlayer != null
                && nextTilePlayer.getNetworkObject().getOwnerId()
                        == getPlayer().getNetworkObject().getOwnerId()) {
            mGone.push(nextNode);
            if (mPath.size() == 0) {
                return Integer.MAX_VALUE;
            }
            nextNode = mPath.pop();
            if (!Reference.isValid(mGraph.getNode(nextNode).getHexTile())) {
                return Integer.MAX_VALUE;
            }

            nextTile = mGraph.getNode(nextNode).getHexTile().get();
            nextTilePlayer = nextTile.getClaimant();
        }

        return nextNode;
    }

    /** Move the player to a node owned by them. */
    private void moveBackwards() {

        if (mGone.size() == 0) {
            mPath = new ArrayDeque<Integer>();
            return;
        }
        int previousNode = mGone.pop();

        // This will get you to the first node which you claimed
        boolean onYourNode = false;
        while (!onYourNode) {

            Reference<HexagonTile> hexagonTile = mGraph.getNode(previousNode).getHexTile();

            if (!Reference.isValid(hexagonTile)) {
                mPath.push(previousNode);
                return;
            }

            if (!hexagonTile.get().isClaimed()) {
                mPath.push(previousNode);
                if (mGone.size() == 0) {
                    mPath = new ArrayDeque<Integer>();
                    return;
                }
                previousNode = mGone.pop();
            } else if (!getPlayer().hasClaimedTile(hexagonTile.get())) {
                mPath.push(previousNode);
                if (mGone.size() == 0) {
                    mPath = new ArrayDeque<Integer>();
                    return;
                }
                if (mGone.size() == 0) {
                    mPath = new ArrayDeque<Integer>();
                    return;
                }
                previousNode = mGone.pop();
            } else {
                onYourNode = true;
            }
        }

        mGone.push(previousNode);
    }

    /** This is what happens when we start the search and moves from the capital. */
    private void atCapital() {

        if (mPath.size() == 0) {
            return;
        }
        int nextDoor = mPath.pop();
        mGone.push(nextDoor);
        if (mPath.size() == 0) {
            return;
        }
        int firstElement = mPath.pop();
        if (!Reference.isValid(mGraph.getNode(firstElement).getHexTile())) {
            mPath.push(firstElement);
            return;
        }
        if (getPlayer().hasClaimedTile(mGraph.getNode(firstElement).getHexTile().get())) {

            mGone.push(firstElement);
            mNodePreviouslyOn = mGraph.getNode(firstElement);

        } else {
            log.fine("Cannot do the first move");
        }
        return;
    }

    /**
     * This will set the opponent to aim for.
     *
     * @return The {@link Player} to attack.
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
            Reference<Building> buildingReference = ownedBuildings.get(index);

            // Checks the building is valid
            if (Reference.isValid(buildingReference)) {
                Building buildingToAim = buildingReference.get().getRandomAttackableBuilding();

                if (buildingToAim != null && !buildingToAim.getOwner().hasLost()) {
                    return buildingToAim.getOwner();
                }
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

    /** This will perform the A* Search and all related operations to it. */
    private void aStar() {

        // Will find the opponent to attack
        Player opponentPlayer = findOpponent();

        if (opponentPlayer != null) {

            aimBuilding(opponentPlayer);
        } else {
            mPath = new ArrayDeque<Integer>();
        }
    }

    /**
     * This will aim for a building.
     *
     * @param opponentPlayer The {@link Player} to aim for.
     */
    private void aimBuilding(Player opponentPlayer) {
        if (opponentPlayer == null) {
            mPath = new ArrayDeque<Integer>();
            return;
        }

        mCapitalAimer = mRandom.nextFloat() <= mAimAtCapital;

        // Will find the tile to attack
        HexagonTile tileToAim = getTileBuilding(opponentPlayer);

        if (tileToAim == null) {
            mPath = new ArrayDeque<Integer>();
            return;
        }

        if (!mCapitalAimer) {

            // Creates a graph
            mGraph =
                    new Graph(
                            getPlayer().getMap(), // TODO Change to stream
                            tileToAim,
                            this);
        } else {
            mGraph = new Graph(getPlayer().getMap(), tileToAim);
        }
        // Finds the buildings
        ImportantNodes buildings = findBuilding(mGraph, opponentPlayer, tileToAim);

        Node startNode = buildings.mStartNode;
        Node endNode = buildings.mGoalNode;

        if (startNode == null || endNode == null) {

            // Null pointer check to try and not destroy the server
            mPath = new ArrayDeque<Integer>();
            return;
        }
        // Performs A* Search
        AStar aStar = new AStar(mGraph, startNode.getNodeId(), endNode.getNodeId());

        mPath = aStar.getPath();
        mGone = new ArrayDeque<Integer>();
        mNodePreviouslyOn = startNode;
        mAttempts = 0;
    }

    /**
     * Get a tile to aim for.
     *
     * @param opponentPlayer The {@link Player} to aim for.
     * @return The {@link HexagonTile} to aim for.
     */
    private HexagonTile getTileBuilding(Player opponentPlayer) {

        if (mCapitalAimer) {
            if (opponentPlayer.getCapital() == null) {
                return null;
            }
            return opponentPlayer.getCapital().getTile();
        }

        HexagonTile[] tileToAim = new HexagonTile[1];
        // This checks if any of the buildings are the capital
        boolean foundTile =
                getViewableTiles()
                        .anyMatch(
                                tile -> {
                                    if (tile.hasBuilding()
                                            && tile.getBuilding().isCapital()
                                            && opponentPlayer.hasClaimedTile(tile)) {
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
                                        if (tile.hasBuilding()
                                                && opponentPlayer.hasClaimedTile(tile)) {
                                            tileToAim[0] = tile;
                                            return true;
                                        } else {
                                            return false;
                                        }
                                    });
        }
        if (!foundTile) {
            log.fine("Cannot find a tile to aim for");
            return null;
        } else {
            return tileToAim[0];
        }
    }

    /**
     * This will convert the visible tiles into a stream.
     *
     * @return The visible {@link HexagonTile}'s as a {@link Stream}.
     */
    public Stream<HexagonTile> getViewableTiles() {

        return getPlayer().getViewableTiles();
    }

    /**
     * This will find the Building node for both your opponent and the capital for you.
     *
     * @param graph The {@link Graph} to use.
     * @param opponentPlayer The {@link Player} to aim for.
     * @param target The target {@link HexagonTile} to aim for.
     * @return An {@link ImportantNodes} which have the {@link Node} to start from and go to.
     */
    private ImportantNodes findBuilding(Graph graph, Player opponentPlayer, HexagonTile target) {

        ImportantNodes buildings = new ImportantNodes();
        // Go through all nodes to find the capital

        buildings.mGoalNode = mGraph.getNode(target);

        HexagonTile startHex = getStartHex();
        if (startHex == null) {
            buildings.mStartNode = null;

        } else {

            buildings.mStartNode = mGraph.getNode(startHex);
        }

        return buildings;
    }

    /**
     * This will choose randomly the HexagonTile to start from.
     *
     * @return The {@link HexagonTile} to start be the start node.
     */
    private HexagonTile getStartHex() {
        List<Reference<Building>> ownedBuildings = getPlayer().getOwnedBuildings();
        if (ownedBuildings.size() == 0) {
            // This has already been checked but hey ho
            return null;
        }
        int index = mRandom.nextInt(ownedBuildings.size());
        final int end = index;

        // Goes through the ownedBuildings
        while (true) {
            Reference<Building> buildingReference = ownedBuildings.get(index);

            // Checks the building is valid
            if (Reference.isValid(buildingReference)) {
                return buildingReference.get().getTile();
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
}
