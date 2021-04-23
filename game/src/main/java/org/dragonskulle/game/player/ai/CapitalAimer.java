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
        
        float PLAY_A_STAR = 0.9f;

        // This will choose whether to play as an A* player or as a Probablistic Player
        if (mRandom.nextFloat() < PLAY_A_STAR) {

        	float GO_DOWN_PATH = 0.6f;
            // This will choose whether to go attacking and get closer to the building/capital or to
            // upgrade
            if (mRandom.nextFloat() < GO_DOWN_PATH) {

                
                moveBackwards();
                
                int nextNode = moveForwards();
                
                if (nextNode == -1) {
                	return;
                }
                
                HexagonTile nextTile = mGraph.getNode(nextNode).getHexTile().get();
                Player nextTilePlayer = nextTile.getClaimant();
                

                // Checks whether to build or to attack
                if (nextTilePlayer == null) {
                    building(nextTile, nextTilePlayer, nextNode);
                    return;
                } else if (nextTilePlayer != null) {

                    attack(nextTile, nextTilePlayer, nextNode);
                    return;
                }
            } else {
                super.upgradeBuilding();
            }
        } else {
            super.simulateInput();
        }
    }
    
    private void attack(HexagonTile nextTile, Player nextTilePlayer, int nextNode) {
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
                getPlayer()
                        .getClientBuildRequest()
                        .invoke((d) -> d.setTile(tileToBuildOn));
                mGone.push(nextNode);
                return;

            } else {
                Building building = nextTile.getBuilding();

                // Will attack instead and remove
                while (building == null) {
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
        }

        // Checks for attackable buildings
        for (Building attacker : toAttackCheck.getAttackableBuildings()) {

            if (getPlayer().isBuildingOwner(attacker)) {

                Building toAttack = nextTile.getBuilding();
                getPlayer()
                        .getClientAttackRequest()
                        .invoke(d -> d.setData(attacker, toAttack));
                if (nextTilePlayer != null
                        && nextTilePlayer.getNetworkObject().getOwnerId()
                                == getPlayer().getNetworkObject().getOwnerId()) {
                    mGone.push(nextNode);
                }
                return;
            }
        }
    }
    
    private void building(HexagonTile nextTile, Player nextTilePlayer, int nextNode) {
    	log.info("Building");
        HexagonTile tileToBuildOn = nextTile;
        // BUILD
        getPlayer().getClientBuildRequest().invoke((d) -> d.setTile(tileToBuildOn));
        mGone.push(nextNode);
        return;
		
	}

	private int moveForwards() {
        // This will point us to the next tile to use
        int nextNode = mPath.pop();
        HexagonTile nextTile = mGraph.getNode(nextNode).getHexTile().get();
        Player nextTilePlayer = nextTile.getClaimant();
        while (nextTile != null
                && nextTilePlayer != null
                && nextTilePlayer.getNetworkObject().getOwnerId()
                        == getPlayer().getNetworkObject().getOwnerId()) {
            mGone.push(nextNode);
            if (mPath.size() == 0) {
                return -1;
            }
            nextNode = mPath.pop();
            nextTile = mGraph.getNode(nextNode).getHexTile().get();
            nextTilePlayer = nextTile.getClaimant();
        }
        
        return nextNode;
    }
    
    /**
     * This will move the player back to the first node which exists
     */
    private void moveBackwards() {
		
        int previousNode = mGone.pop();

        // This will get you to the first node which you claimed
        boolean onYourNode = false;
        while (!onYourNode) {
            if (Reference.isValid(mGraph.getNode(previousNode).getHexTile())
                    && mGraph.getNode(previousNode).getHexTile().get().getClaimant()
                            == null) {
                mPath.push(previousNode);
                previousNode = mGone.pop();
            } else if (!getPlayer().isClaimingTile(mGraph.getNode(previousNode)
                    .getHexTile()
                    .get())
                            ) {
                mPath.push(previousNode);
                previousNode = mGone.pop();
            } else {
                onYourNode = true;
            }
        }

        mGone.push(previousNode);
		
	}

	/**
     * This is what happens when we start the search
     */
    private void atCapital() {
        log.info("mGone == 0");
        int nextDoor = mPath.pop();
        mGone.push(nextDoor);
        int firstElement = mPath.pop();
        if (getPlayer().isClaimingTile(mGraph.getNode(firstElement)
                        .getHexTile()
                        .get()
                        )
                        
                ) {
            mGone.push(firstElement);
        } else {
            log.severe("You might be dead");
        }
        return;
    }

    /**
     * This will find the capital node for both you and the opponent 
     * @param graph The {@code Graph} to check through
     * @param opponentPlayer The opponent {@code Player} which you are attacking 
     * @return An Array of {@code Node}'s which you start from and end at
     */
    private Node[] findCapital(Graph graph, Player opponentPlayer) {
        Integer[] nodesInGraph = graph.getNodes();
        
        Node[] capitals = new Node[2];
        // Go through all nodes to find the capital
        for (int nodeNumber : nodesInGraph) {
            Node node = graph.getNode(nodeNumber);
            HexagonTile nodesHexagonTile = node.getHexTile().get();
            
            // Checks if hexagontile has a capital
            if (!nodesHexagonTile.isClaimed()
                    && node.getHexTile().get().getBuilding() != null
                    && node.getHexTile().get().getBuilding().isCapital()) {
            	
            	// Checks if the hexagonTiles capital is the opponent players
                if (opponentPlayer.isClaimingTile(node.getHexTile().get())) {
                    capitals[0] = node;
                    
                // Checks if hexagonTiles capital is ours
                } else if (getPlayer().isClaimingTile(node.getHexTile().get())) {
                    capitals[1] = node;
                }
            }
        }
        return capitals;
    }

    /**
     * This will get the tile which needs to be aimed for
     * @param opponentPlayer The Opponent {@code Player} to aim for
     * @return A {@code Reference} to a {@code HexagonTile}
     */
    private Reference<HexagonTile> getTileCapital(Player opponentPlayer) {
    	
    	Building capitalBuilding = opponentPlayer.getCapital();
    	
    	if (capitalBuilding == null) {
    		log.severe("No Capital on this player");
    		return null;
    	}
    	
    	mTileToAim = new Reference<HexagonTile>(capitalBuilding.getTile());
    	
    	
        return mTileToAim;
        
    }

    /**
     * This will set the opponent to aim for
     * @return The {@code Player} to attack
     */
    private Player findOpponent() {

    	
    	ArrayList<Reference<Building>> ownedBuildings = getPlayer().getOwnedBuildings();
    	 int index = mRandom.nextInt(ownedBuildings.size());
         final int end = index;

         // Goes through the ownedBuildings
         while (true) {
             Reference<Building> building = ownedBuildings.get(index);

             // Checks the building is valid
             if (Reference.isValid(building) && building.get().getAttackableBuildings().size() != 0) {
                 // Check
            	 
            	 ArrayList<Building> attackableBuildings = building.get().getAttackableBuildings();
                 Building buildingToAim = attackableBuildings.get(mRandom.nextInt(attackableBuildings.size()));
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

            if (mRandom.nextFloat() < 4) {
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
            Node[] capitals = findCapital(mGraph, opponentPlayer);

            Node capNode = capitals[1];
            Node oppCapNode = capitals[0];

            if (capNode == null || oppCapNode == null) {

                // Null pointer check to try and not destroy the server
                mPath = new ArrayDeque<Integer>();
                return;
            }
            // Performs A* Search
            AStar aStar = new AStar(mGraph);
            aStar.aStarAlgorithm(capNode.getNode(), oppCapNode.getNode());
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
                                getPlayer().getMap(), // TODO Change to stream
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

        // This checks if any of the buildings are the capital
        boolean foundTile =
                getStream()
                        .anyMatch(
                                tile -> {
                                    if (tile.hasBuilding()
                                            && tile.getBuilding().isCapital()
                                            && opponentPlayer.isClaimingTile(tile
                                                    )) {
                                        mTileToAim = new Reference<HexagonTile>(tile);
                                        return true;
                                    } else {
                                        return false;
                                    }
                                });

        // If no capitals are found just select a random building to attack
        if (!foundTile) {
            foundTile =
                    getStream()
                            .anyMatch(
                                    tile -> {
                                        if (tile.getBuilding() != null
                                                && opponentPlayer.isClaimingTile(tile)
                                                       ) {
                                            mTileToAim = new Reference<HexagonTile>(tile);
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
            return mTileToAim;
        }
    }

    /**
     * This will convert the visible tiles into a stream
     *
     * @return The visible {@code HexagonTile}'s as a {@code Stream}
     */
    public Stream<HexagonTile> getStream() {
    	
    	return getPlayer().getViewableTiles();
        
    }

    /** This will find the Building node for both your opponent and the capital for you */
    private Node[] findBuilding(Graph graph, Player opponentPlayer) {
        Integer[] nodesInGraph = graph.getNodes();

        Node[] buildings = new Node[2];
        // Go through all nodes to find the capital
        for (int nodeNumber : nodesInGraph) {
            Node node = graph.getNode(nodeNumber);
            
            HexagonTile nodesHexagonTile = node.getHexTile().get();

            if (!nodesHexagonTile.isClaimed()
                    && nodesHexagonTile.getBuilding() != null) {
                if (nodesHexagonTile.getQ() == mTileToAim.get().getQ()
                        && nodesHexagonTile.getQ() == mTileToAim.get().getQ()
                        && opponentPlayer.isClaimingTile(nodesHexagonTile)) {
                    buildings[0] = node;
                } else if (nodesHexagonTile.getBuilding().isCapital()
                        && getPlayer().isClaimingTile(nodesHexagonTile)) {
                    buildings[1] = node;
                }
            }
        }
        return buildings;
    }
    
    private Player getPlayer() {
    	return mPlayer.get();
    }
}
