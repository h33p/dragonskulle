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
import org.dragonskulle.game.player.algorithms.graphs.Graph;
import org.dragonskulle.game.player.algorithms.graphs.Node;

/**
 * An AI player which will aim for a capital of a player.
 *
 * @author Dragonskulle
 */
@Log
public class CapitalAimer extends AiPlayer {

    private Player opponentPlayer = mPlayer.get();
    private final float UPDATE_PATH_TIME = 60;
    private float timeSinceLastCheck = Time.getTimeInSeconds() - UPDATE_PATH_TIME;
    private Deque<Integer> path;
    private Deque<Integer> gone;
    private Graph graph = null;
    private Node capNode = null;
    private Node oppCapNode = null;
    private Reference<HexagonTile> tileToAim = new Reference<HexagonTile>(null);

    public CapitalAimer() {};

    @Override
    protected void simulateInput() {
        if (opponentPlayer.getNetworkObject().getOwnerId()
                == mPlayer.get().getNetworkObject().getOwnerId()) {
            findOpponent();
            tileToAim = new Reference<HexagonTile>(null);
            getTile();
        }

        if (timeSinceLastCheck + UPDATE_PATH_TIME > Time.getTimeInSeconds()) {
            aStar();
        }

        if (path.size() == 0) {
            opponentPlayer = mPlayer.get();
            return;
        }

        if (gone.size() == 0) {
            int firstElement = path.pop();
            if (graph.getNode(firstElement)
                            .getHexTile()
                            .get()
                            .getClaimant()
                            .getNetworkObject()
                            .getOwnerId()
                    == mPlayer.get().getNetworkObject().getOwnerId()) {
                gone.push(firstElement);
            } else {
                log.severe("You might be dead");
            }
        }

        int previousNode = gone.pop();

        boolean onYourNode = false;
        while (!onYourNode) {
            if (graph.getNode(previousNode).getHexTile().get().getClaimant() == null) {
                path.push(previousNode);
                previousNode = gone.pop();
            } else if (graph.getNode(previousNode)
                            .getHexTile()
                            .get()
                            .getClaimant()
                            .getNetworkObject()
                            .getOwnerId()
                    != mPlayer.get().getNetworkObject().getOwnerId()) {
                path.push(previousNode);
                previousNode = gone.pop();
            } else {
                onYourNode = true;
            }
        }

        gone.push(previousNode);
        int nextNode = path.pop();
        while (graph.getNode(nextNode)
                        .getHexTile()
                        .get()
                        .getClaimant()
                        .getNetworkObject()
                        .getOwnerId()
                == mPlayer.get().getNetworkObject().getOwnerId()) {
            gone.push(nextNode);
            nextNode = path.pop();
        }

        if (graph.getNode(nextNode).getHexTile().get().getClaimant() == null) {
            // BUILD
            mPlayer.get()
                    .getClientBuildRequest()
                    .invoke(
                            (d) ->
                                    d.setTile(
                                            graph.getNode(nextNode)
                                                    .getHexTile()
                                                    .get())); // TODO Make as close as final as
            // possible
            gone.push(nextNode);
        } else if (graph.getNode(nextNode).getHexTile().get().getClaimant() != null) {
            // ATTACK
            if (!graph.getNode(nextNode).getHexTile().get().hasBuilding()) {
                // Assuming that the building is on the next node
                gone.push(nextNode);
                nextNode = path.pop();
            }
            Building toAttack = graph.getNode(nextNode).getHexTile().get().getBuilding();
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
                    gone.push(nextNode);
                }
            }
        }
        // TODO Perform actions here
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

    /** This will find the capital node for both you and the opponent */
    private void findCapital() {
        Integer[] nodesInGraph = graph.getNodes();

        for (int nodeNumber : nodesInGraph) {
            Node node = graph.getNode(nodeNumber);
            if (node.getHexTile().get().getClaimant() == opponentPlayer
                    && node.getHexTile().get().getBuilding().isCapital()) {
                oppCapNode = node;
            } else if (node.getHexTile().get().getClaimant() == mPlayer.get()
                    && node.getHexTile().get().getBuilding().isCapital()) {
                capNode = node;
            }
        }
    }

    private void getTile() {
        Stream<HexagonTile> tiles = mPlayer.get().getMap().getAllTiles();
        while (tileToAim.get() == null) {
            Optional<HexagonTile> tile = tiles.findAny();
            HexagonTile tileFound = tile.get();
            if (tileFound.getBuilding() != null && tileFound.getBuilding().isCapital()) {
                tileToAim = new Reference<HexagonTile>(tileFound);
            }
        }
    }

    /** This will set the opponent to aim for */
    private void findOpponent() {
        Stream<HexagonTile> tiles = mPlayer.get().getMap().getAllTiles();

        while (opponentPlayer.getNetworkObject().getOwnerId()
                == mPlayer.get().getNetworkObject().getOwnerId()) {
            Optional<HexagonTile> tile = tiles.findAny();
            HexagonTile tileFound = tile.get();
            Player playerAiming = tileFound.getClaimant();
            if (playerAiming != null) {
                opponentPlayer = playerAiming;
            }
        }
    }

    /** This will perform the A* Search */
    private void aStar() {
        Graph tempGraph =
                new Graph(
                        mPlayer.get().getMap(),
                        mPlayer.get().getNetworkObject().getOwnerId(),
                        tileToAim
                                .get()); // TODO Currently just creates a dummy building so the code
        // compiles
        graph = tempGraph;
        AStar aStar = new AStar(graph);
        findCapital();
        try {
            aStar.aStarAlgorithm(capNode.getNode(), oppCapNode.getNode());
        } catch (GraphNodeException e) {
            // TODO Shouldn't get here.
            log.severe("EXCEPTION");
        }
        path = aStar.nodesToVisit();
        gone = new ArrayDeque<Integer>();
    }
}
