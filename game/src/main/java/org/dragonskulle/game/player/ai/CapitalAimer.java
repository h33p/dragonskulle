/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.Optional;
import java.util.stream.Stream;

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
public class CapitalAimer extends AiPlayer {

    private Player opponentPlayer = mPlayer.get();
    private final float UPDATE_PATH_TIME = 60;
    private float timeSinceLastCheck = Time.getTimeInSeconds() - UPDATE_PATH_TIME;
    private Integer[] nodesToUse;
    private Graph graph = null;
    private Node capNode = null;
    private Node oppCapNode = null;
    private Reference<HexagonTile> tileToAim = new Reference<HexagonTile>(null);

    public CapitalAimer() {};

    @Override
    protected void simulateInput() {
        if (opponentPlayer.getNetworkObject().getOwnerId() == mPlayer.get().getNetworkObject().getOwnerId()) {
            findOpponent();
        }

        if (timeSinceLastCheck + UPDATE_PATH_TIME > Time.getTimeInSeconds()) {
            aStar();
        }

        if (nodesToUse.length == 0) {
            opponentPlayer = mPlayer.get();
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
    	while (tileToAim == null) {
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

        while (opponentPlayer.getNetworkObject().getOwnerId() == mPlayer.get().getNetworkObject().getOwnerId()) {
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
        Graph tempGraph = new Graph(mPlayer.get().getMap(), mPlayer.get().getNetworkObject().getOwnerId(), );		//TODO Currently just creates a dummy building so the code compiles
        graph = tempGraph;
        AStar aStar = new AStar(graph);
        findCapital();
        try {
            aStar.aStarAlgorithm(capNode.getNode(), oppCapNode.getNode());
        } catch (GraphNodeException e) {
            // TODO Shouldn't get here.
            log.severe("EXCEPTION");
        }
        nodesToUse = aStar.nodesToVisit();
    }
}
