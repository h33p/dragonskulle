/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.game.player.ai.AimerAi;

/**
 * Will implement a directed Graph data structure. This has been adapted from Nathaniel Lowis's (one
 * of our group members) repository: https://github.com/low101043/aiProjectComputer
 *
 * @author Dragonskulle
 */
@Log
public class Graph {

    /** The hash map which will hold the node id to the Node */
    protected HashMap<Integer, Node> mGraph;

    /** The next node number to be used */
    private int mCurrentNodeId;

    /** The {@code HexagonMap} which is used */
    private Reference<HexagonMap> mMap;

    /** The {@code HexgaonTile} to aim at */
    private Reference<HexagonTile> mTarget;

    /**
     * Constructor to create the whole map
     *
     * @param map The {@code HexagonMap} to convert to a {@code Graph}
     * @param target The {@code HexagonTile} to aim for
     */
    public Graph(HexagonMap map, HexagonTile target) {

        initialise(map, target);

        mCurrentNodeId = 0;
        mGraph = new HashMap<Integer, Node>();
        map.getAllTiles().forEach(this::convertToNode);
        mCurrentNodeId = 0;
        map.getAllTiles().forEach(this::addConnections);
    }

    /**
     * Constructor to create only part of the map
     *
     * @param map The {@code HexagonMap} to convert to a {@code Graph}
     * @param target The {@code HexagonTile} to aim for
     * @param aimer The {@code CapitalAimer} to use to create the map
     */
    public Graph(HexagonMap map, HexagonTile target, AimerAi aimer) {

        initialise(map, target);

        mCurrentNodeId = 0;
        aimer.getViewableTiles().forEach(this::convertToNode);
        mCurrentNodeId = 0;
        aimer.getViewableTiles().forEach(this::addConnections);
    }

    /**
     * This is the variables set up in both constructors
     *
     * @param map The {@code HexagonMap} to be used
     * @param tileAiming The {@code HexagonTile} being aimed at
     */
    private void initialise(HexagonMap map, HexagonTile tileAiming) {
        mTarget = new Reference<HexagonTile>(tileAiming);
        mMap = map.getReference(HexagonMap.class);
        mGraph = new HashMap<Integer, Node>();
    }

    /**
     * This will convert a {@code HexagonTile} to a {@code Node} which can be used by a {@code
     * Graph}
     *
     * @param tile The {@code HexagonTile} to be made a node
     */
    private void convertToNode(HexagonTile tile) {

        if (tile.getTileType() != TileType.LAND) {
            return;
        }
        log.info("Node num: " + mCurrentNodeId);

        addNode(mCurrentNodeId, tile);
        int heuristic = tile.distTo(mTarget.get().getQ(), mTarget.get().getR());
        setNodeHeuristic(mCurrentNodeId, heuristic);
        mCurrentNodeId++;
    }

    /**
     * This will add all the connections for a node
     *
     * @param tile The {@code HexagonTile} to add connections for in the graph
     */
    private void addConnections(HexagonTile tile) {
        if (tile.getTileType() != TileType.LAND) {
            return;
        }
        log.info("Node num: " + mCurrentNodeId);
        ArrayList<HexagonTile> neighbourTilesList = new ArrayList<HexagonTile>();
        List<HexagonTile> neighbourTiles =
                mMap.get().getTilesInRadius(tile, 1, false, neighbourTilesList);

        for (HexagonTile tileNeighbour : neighbourTiles) {
            for (Node node : mGraph.values()) {
                if (tileNeighbour.getQ() == node.getHexTile().get().getQ()
                        && tileNeighbour.getR() == node.getHexTile().get().getR()) {

                    addConnection(mCurrentNodeId, node.getNodeId(), 1); // Weight set to 1
                }
            }
        }
        mCurrentNodeId++;
    }

    /**
     * This will add a node to a mGraph with no connections
     *
     * @param nodeId The node number
     * @param tile The {@code HexagonTile} it corresponds to
     */
    public void addNode(int nodeId, HexagonTile tile) {

        Node newNode = new Node(nodeId, tile); // Makes a new node
        mGraph.put(nodeId, newNode); // Adds to mGraph
    }

    /**
     * Adds a connection between two nodes
     *
     * @param originNode The origin node
     * @param destinationNode the end node
     * @param weight The weight between the nodes
     */
    public void addConnection(int originNode, int destinationNode, int weight) {

        // Gets the connection if in the graph -- assumption is that all are added by now.
        Node node = mGraph.get(originNode);

        node.addConnection(destinationNode, weight); // Adds the connection to the node
    }

    /**
     * Returns the connections of that node
     *
     * @param nodeNum The node to find
     * @return The connections
     */
    public ArrayList<Connection> getConnection(int nodeNum) {

        Node node = mGraph.get(nodeNum);
        if (node == null) {
            return new ArrayList<Connection>();
        } else {
            return mGraph.get(nodeNum).getConnections();
        }
    }

    /**
     * Returns the heuristic for that node
     *
     * @param nodeToGet The node which has the special info to get
     * @return the heuristic for that node
     */
    public int getNodeHeuristic(int nodeToGet) {

        return mGraph.get(nodeToGet).getHeuristic();
    }

    /**
     * A setter which sets the extra info
     *
     * @param nodeToChange The node to change
     * @param newInfo The extra info to change
     */
    public void setNodeHeuristic(int nodeToChange, int newInfo) {

        Node node = mGraph.get(nodeToChange);

        node.setHeuristic(newInfo);
        mGraph.replace(nodeToChange, node);
    }

    /**
     * Will return all the node numbers in the mGraph
     *
     * @return An integer array which has all the Nodes used
     */
    public Integer[] getNodes() {

        ArrayList<Integer> nodes = new ArrayList<Integer>();

        for (Map.Entry<Integer, Node> entry : mGraph.entrySet()) {
            Integer node = entry.getKey();
            nodes.add(node);
        }

        return nodes.toArray(new Integer[0]);
    }

    /**
     * Returns a node
     *
     * @param node The node to find
     * @return The actual {@code Node}
     */
    public Node getNode(Integer node) {
        return mGraph.get(node);
    }
}
