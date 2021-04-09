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

    private Player mOpponentPlayer; 
    private final float UPDATE_PATH_TIME = 60;
    private float mTimeSinceLastCheck = Time.getTimeInSeconds()  - UPDATE_PATH_TIME;
    private Deque<Integer> mPath = new ArrayDeque<Integer>();
    private Deque<Integer> mGone;
    private Graph mGraph = null;
    private Node mCapNode = null;
    private Node mOppCapNode = null;
    private Reference<HexagonTile> mTileToAim = new Reference<HexagonTile>(null);

    public CapitalAimer() {}

    @Override
    protected void simulateInput() {
    	
    	// Checks if we have reached the capital
    	if (mPath.size() == 0) {
    		
    		// Will find the opponent to attack
    		log.info("Changing opponent");
            mOpponentPlayer = mPlayer.get();
            findOpponent();
           
            // Will find the tile to attack
            mTileToAim = new Reference<HexagonTile>(null);
            getTile();
            log.info("Found Tile");
        
            // Will perform all necessary checks for A*
            aStar();
            log.severe("A* Ran");
            return;
    	}
    	
    	// Checks if path size is 0 
        if (mPath.size() == 0) {
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
            if (mGraph.getNode(previousNode).getHexTile().get().getClaimant() == null) {		//TODO null checks please
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
        while (mGraph.getNode(nextNode).getHexTile().get().getClaimant() != null &&
        		mGraph.getNode(nextNode)					//TODO null checks please
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
        } else if (mGraph.getNode(nextNode).getHexTile().get().getClaimant() != null) {
        	log.info("Attacking");
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
    }

    /** 
     * This will find the capital node for both you and the opponent 
    */
    private void findCapital() {
        Integer[] nodesInGraph = mGraph.getNodes();

        // Go through all nodes to find the capital
        for (int nodeNumber : nodesInGraph) {
            Node node = mGraph.getNode(nodeNumber);
            
            if (node.getHexTile().get().getClaimant() != null && node.getHexTile().get().getClaimant().getNetworkObject().getOwnerId() == mOpponentPlayer.getNetworkObject().getOwnerId()
                   && node.getHexTile().get().getBuilding() != null && node.getHexTile().get().getBuilding().isCapital()) {
                mOppCapNode = node;
            } else if (node.getHexTile().get().getClaimant() != null && node.getHexTile().get().getClaimant().getNetworkObject().getOwnerId() == mPlayer.get().getNetworkObject().getOwnerId()
                    && node.getHexTile().get().getBuilding() != null && node.getHexTile().get().getBuilding().isCapital()) {
                mCapNode = node;
            }
        }
    }

    /**
     * This will get the tile which needs to be aimed for
     */
    private void getTile() {
    	boolean foundTile = mPlayer.get().getMap().getAllTiles().anyMatch(tile -> {
    		if (tile.getBuilding() != null && tile.getBuilding().isCapital() && tile.getClaimant().getNetworkObject().getOwnerId() ==
    				mOpponentPlayer.getNetworkObject().getOwnerId()) {
    			mTileToAim = new Reference<HexagonTile>(tile);
    			return true;
    		}
    		else {
    			return false;
    		}
    	});
        
    	if (!foundTile) {
    		log.severe("We have a serious problem");
    	}
    }

    /** 
     * This will set the opponent to aim for 
    */
    private void findOpponent() {

       boolean found = mPlayer.get().getMap().getAllTiles().anyMatch(tile -> {
            	if (tile.getClaimant() != null && tile.getClaimant().getNetworkObject().getOwnerId() !=  mPlayer.get().getNetworkObject().getOwnerId()) {
            		mOpponentPlayer = tile.getClaimant();
            		return true;
            	}
            	else {
            		
            		return false;
            	}
            });
       
       if (found == false) {
    	   log.severe("Houston we have a problem");
       }
           
        
    }

    /** 
     * This will perform the A* Search and all related operations to it
    */
    private void aStar() {
    	// Creates a graph
        Graph tempGraph =
                new Graph(
                        mPlayer.get().getMap(),
                        mPlayer.get().getNetworkObject().getOwnerId(),
                        mTileToAim
                                .get()); 
        mGraph = tempGraph;
 
        
        // Finds the capitals
        findCapital();
        
        // Performs A* Search
        AStar aStar = new AStar(mGraph);
        try {
            aStar.aStarAlgorithm(mCapNode.getNode(), mOppCapNode.getNode());
            log.severe("Completed");
        } catch (GraphNodeException e) {
            // TODO Shouldn't get here.
            log.severe("EXCEPTION");
        }
        
        mPath = aStar.getAnswerOfNodes();
        
        //TODO Testing - remove before PR
        String answer = "";
        if (mPath.size() == 0) {
        	log.severe("HOWWWWW");
        }
        for (int node: mPath) {
        	answer = answer + node + " ->";
        }
        log.info(answer);
        
        mGone = new ArrayDeque<Integer>();
    }
}
