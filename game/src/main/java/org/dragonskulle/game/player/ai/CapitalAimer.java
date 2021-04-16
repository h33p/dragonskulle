/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayDeque;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.ai.algorithms.AStar;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;
import org.dragonskulle.game.player.ai.algorithms.graphs.Node;

/**
 * An AI player which will aim for a capital of a player. This is done using the A* Algorithm. Only
 * one should ever be created
 *
 * @author Dragonskulle
 */
@Log
public class CapitalAimer extends Aimer {

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

    /** This will perform the A* Search and all related operations to it */
    protected void aStar() {

        // Will find the opponent to attack
        log.info("Changing opponent");
        Player opponentPlayer = findOpponent();

        if (opponentPlayer != null) {
            // Will find the tile to attack
            Reference<HexagonTile> tileToAim = getTile(opponentPlayer);
            log.info("Found Tile");

            if (Reference.isValid(tileToAim)) {
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
        } else {
            mPath = new ArrayDeque<Integer>();
        }
    }
}
