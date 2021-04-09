/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.ai.algorithms.exceptions.GraphException;
import org.dragonskulle.game.player.ai.algorithms.exceptions.GraphNodeException;
import org.dragonskulle.game.player.ai.algorithms.graphs.Node;
import org.dragonskulle.game.player.ai.algorithms.graphs.Connection;


/**
 * Graph which implements {@code GraphInterface}. Will implement a directed Graph data structure
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
     * This will convert a {@code HexagonTile} to a {@code Node} which can be used by a {@code Graph}
     * @param tile The {@code HexagonTile} to be made a node
     */
    private void convertToNode(HexagonTile tile) {

        try {
            addNode(mNodeNum, tile);
            
            int heuristic = tile.distTo(mTileAiming.get().getQ(), mTileAiming.get().getR());
            
            
            this.setNodeSpecial(mNodeNum, heuristic);

        } catch (GraphNodeException e) {
            // TODO Already in mGraph
        }
        mNodeNum++;
    }

    /**
     * This will add all the connections for a node
     * @param tile The {@code HexagonTile} to add connections for in the graph
     */
    private void addConnections(HexagonTile tile) {
        boolean found = false;
        ArrayList<HexagonTile> neighbourTiles = mMap.get().getTilesInRadius(tile, 1);

        for (HexagonTile tileNeighbour : neighbourTiles) {
            for (Map.Entry<Integer, Node> mapEntry : mGraph.entrySet()) {
                if (tileNeighbour.getQ() == mapEntry.getValue().getHexTile().get().getQ()
                        && tileNeighbour.getR() == mapEntry.getValue().getHexTile().get().getR()) {

                    //int distance = tile.distTo(mTileAiming.get().getQ(), mTileAiming.get().getR());
                    
                    try {
                        addConnection(
                                mNodeNum,
                                mapEntry.getValue().getNode(),
                                1); // Weight set to 1
                    } catch (Exception e) {
                        log.severe("Exception -- not sure how is here");
                    }
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

        Node node =
                mGraph.get(nodeToAdd); // Gets the connection if in the mGraph. If not there gets null

        if (node == null) { // If the node is not in the mGraph
            Node newNode = new Node(nodeToAdd, tile); // Makes a new node
            mGraph.put(nodeToAdd, newNode); // Adds to mGraph

        } else {
            throw new GraphNodeException();
        }
    }

    /**
     * Adds a connection between two nodes
     *
     * @param originNode The origin node
     * @param destinationNode the end node
     * @param weight The weight between the nodes
     * @throws GraphException If a connection already exists between them
     * @throws GraphNodeException If the node already exists -- shouldn't happen
     */
    public void addConnection(int originNode, int destinationNode, double weight)
            throws GraphException, GraphNodeException {

        Node node =
                mGraph.get(
                        originNode); // Gets the connection if in the mGraph. If not there gets null

        if (node == null) { // If the node is not in the mGraph
            addNode(originNode); // Adds the node to the mGraph
            node = mGraph.get(originNode); // Gets the actual edge
        }
        Connection foundConnection = findConnection(originNode, destinationNode);

        if (foundConnection == null) {
            node.addConnection(destinationNode, weight); // Adds the connection to the node

            Node nodeEnd = mGraph.get(destinationNode); // Gets the destination node

            if (nodeEnd == null) { // Adds the node to the mGraph if it does not exist

                addNode(destinationNode);
            }
        } else {
            throw new GraphException();
        }
    }

    private void addNode(int originNode) {
		// TODO Auto-generated method stub
		
	}

	/**
     * Returns the connections as originNode, Destination node and weight
     *
     * @return A 2D Object Array with the origin node, the destination node and the weight between
     *     them
     */
    public Object[][] getConnections() {

        ArrayList<Object[]> connections =
                new ArrayList<>(); // The object which will have all the connections

        for (Map.Entry<Integer, Node> entry :
                mGraph.entrySet()) { // For each pair of values in the hash map

            ArrayList<Connection> connection = entry.getValue().getConnections();

            for (Connection edge : connection) // Will add it to the connections
            connections.add(
                        new Object[] {
                            edge.getOriginNode(), edge.getDestinationNode(), edge.getWeight()
                        });
        }

        return connections.toArray(new Object[0][0]);
    }

    /**
     * The number of nodes in the mGraph
     *
     * @return the size of the mGraph
     */
    public int getNumberOfNodes() {

        return mGraph.size();
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
     * @throws GraphNodeException
     */
    public double getNodeSpecial(int nodeToGet) throws GraphNodeException {

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
     * @throws GraphNodeException
     */
    public void setNodeSpecial(int nodeToChange, double newInfo) throws GraphNodeException {

        Node node = mGraph.get(nodeToChange);
        if (node == null) {
            throw new GraphNodeException();
        }
        node.setExtraInfo(newInfo);
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
     * This will find a specific connection
     *
     * @param node The node to start with
     * @param destination the destination node
     * @return The Connection if found
     * @throws GraphNodeException
     */
    private Connection findConnection(int node, int destination) throws GraphNodeException {

        ArrayList<Connection> connections = getConnection(node);
        for (Connection connection : connections) {
            if (connection.getDestinationNode() == destination) {
                return connection;
            }
        }
        return null;
    }

    /**
     * If the mGraph contains that node
     *
     * @param node The node to look for
     * @return True if the node is in the mGraph false if not
     */
    public boolean inGraph(int node) {
        return mGraph.containsKey(node);
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
