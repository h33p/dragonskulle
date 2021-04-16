package org.dragonskulle.game.player.ai;

import java.util.ArrayDeque;

import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.ai.algorithms.AStar;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;
import org.dragonskulle.game.player.ai.algorithms.graphs.Node;

import lombok.extern.java.Log;

/**
 * This AI character will aim for a Building when it sees one in its visible tiles
 * @author DragonSkulle
 *
 */
@Log
public class BuildingAimer extends Aimer {

	private Node[] findBuilding(Graph graph, Player opponent) {
		return null;
	}
	
	private Reference<HexagonTile> getTile(Player opponent){
		return null;
	}
	
	@Override
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
                Node[] capitals = findBuilding(graph, opponentPlayer);

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
	

