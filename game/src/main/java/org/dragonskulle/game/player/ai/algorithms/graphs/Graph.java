/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.ai.algorithms.exceptions.GraphNodeException;

/**
 * Graph which implements {@code GraphInterface}. Will implement a directed Graph data structure.
 * This has been adapted from Nathaniel Lowis's (one of our group members) repository:
 * https://github.com/low101043/aiProjectComputer
 *
 * @author Dragonskulle
 */
@Log
public class Graph {

    protected Map<Integer, Node> mGraph; // The hash map which will have the integer to the Node
    private int mNodeNum;
    private Reference<HexagonMap> mMap;
    private final int mOwnerId;
    private Reference<HexagonTile> mTileAiming;

    public Graph(HexagonMap map, int ownerId, HexagonTile tileAiming) {
        mTileAiming = new Reference<HexagonTile>(tileAiming);
        mMap = map.getReference(HexagonMap.class);
        mOwnerId = ownerId;
        mNodeNum = 0;
        mGraph = new HashMap<Integer, Node>();
        map.getAllTiles().forEach(this::convertToNode);
        mNodeNum = 0;
        map.getAllTiles().forEach(this::addConnections);
    }

    /**
     * This will convert a {@code HexagonTile} to a {@code Node} which can be used by a {@code
     * Graph}
     *
     * @param tile The {@code HexagonTile} to be made a node
     */
    private void convertToNode(HexagonTile tile) {

        try {
            addNode(mNodeNum, tile);

            int heuristic = tile.distTo(mTileAiming.get().getQ(), mTileAiming.get().getR());

            setNodeSpecial(mNodeNum, heuristic);

        } catch (GraphNodeException e) { // TODO shouldn't need
            // TODO Already in mGraph
        }
        mNodeNum++;
    }

    /**
     * This will add all the connections for a node
     *
     * @param tile The {@code HexagonTile} to add connections for in the graph
     */
    private void addConnections(HexagonTile tile) {
        boolean found = false;
        ArrayList<HexagonTile> neighbourTiles = mMap.get().getTilesInRadius(tile, 1);

        for (HexagonTile tileNeighbour : neighbourTiles) {
            for (Map.Entry<Integer, Node> mapEntry : mGraph.entrySet()) {
                if (tileNeighbour.getQ() == mapEntry.getValue().getHexTile().get().getQ()
                        && tileNeighbour.getR() == mapEntry.getValue().getHexTile().get().getR()) {

                    addConnection(mNodeNum, mapEntry.getValue().getNode(), 1); // Weight set to 1
                }
            }
        }
        mNodeNum++;
    }

    /**
     * This will add a node to a mGraph with no connections
     *
     * @param nodeToAdd The node number
     * @param tile The {@code HexagonTile} it corresponds to
     * @throws GraphNodeException If the node already exists
     */
    public void addNode(int nodeToAdd, HexagonTile tile) throws GraphNodeException {

        Node newNode = new Node(nodeToAdd, tile); // Makes a new node
        mGraph.put(nodeToAdd, newNode); // Adds to mGraph
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
     * @throws GraphNodeException If the node does not exist
     */
    public ArrayList<Connection> getConnection(int nodeNum) throws GraphNodeException {

        Node node = mGraph.get(nodeNum);
        if (node == null) {
            throw new GraphNodeException();
        } else {
            return mGraph.get(nodeNum).getConnections();
        }
    }

    /**
     * Returns the special info for that node
     *
     * @param nodeToGet The node which has the special info to get
     * @return the extra info for that node
     * @throws GraphNodeException If the node does not exist -- shouldn't happen
     */
    public int getNodeSpecial(int nodeToGet) throws GraphNodeException {

        Node node = mGraph.get(nodeToGet);
        if (node == null) {
            throw new GraphNodeException();
        } else {

            return mGraph.get(nodeToGet).getExtraInfo();
        }
    }

    /**
     * A setter which sets the extra info
     *
     * @param nodeToChange The node to change
     * @param newInfo The extra info to change
     * @throws GraphNodeException If the node does not exist -- shouldn't happen 
     */
    public void setNodeSpecial(int nodeToChange, int newInfo) throws GraphNodeException {

        Node node = mGraph.get(nodeToChange);
        if (node == null) {
            throw new GraphNodeException();
        }
        node.setExtraInfo(newInfo);
        mGraph.replace(nodeToChange, node);
    }

    /**
     * Will return all the node numbers in the mGraph -- Used for testing
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
