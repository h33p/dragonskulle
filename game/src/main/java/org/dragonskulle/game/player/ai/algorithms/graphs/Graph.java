/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.ai.algorithms.exceptions.GraphException;
import org.dragonskulle.game.player.ai.algorithms.exceptions.GraphNodeException;

import lombok.extern.java.Log;

/**
 * Graph which implements {@code GraphInterface}. Will implement a directed Graph data structure
 *
 * @author Dragonskulle
 */
@Log
public class Graph {

    protected Map<Integer, Node> graph; // The hash map which will have the integer to the Node
    private int mNodeNum;
    private Reference<HexagonMap> mMap;
    private final int mOwnerId;
    private Reference<HexagonTile> mTileAiming;

    public Graph(HexagonMap map, int ownerId, HexagonTile tileAiming) {
        mTileAiming = new Reference<HexagonTile>(tileAiming);
        mMap = map.getReference(HexagonMap.class);
        mOwnerId = ownerId;
        mNodeNum = 0;
        graph = new HashMap<Integer, Node>();
        Stream<HexagonTile> tiles = map.getAllTiles();
        tiles.forEach(this::convertToNode);
        mNodeNum = 0;
        tiles.forEach(this::addConnections);
    }

    private void convertToNode(HexagonTile tile) {

        try {
            addNode(mNodeNum, tile);

            int heuristic = tile.distTo(mTileAiming.get().getQ(), mTileAiming.get().getR());

            this.setNodeSpecial(mNodeNum, heuristic);

        } catch (GraphNodeException e) {
            // TODO Already in graph
        }
        mNodeNum++;
    }

    private void addConnections(HexagonTile tile) {
        boolean found = false;
        ArrayList<HexagonTile> neighbourTiles = mMap.get().getTilesInRadius(tile, 1);

        for (HexagonTile tileNeighbour : neighbourTiles) {
            for (Map.Entry<Integer, Node> mapEntry : graph.entrySet()) {
                if (tileNeighbour.getQ() == mapEntry.getValue().getHexTile().get().getQ()
                        && tileNeighbour.getR() == mapEntry.getValue().getHexTile().get().getR()) {

                    int distance = 10; // A Chosen number so the hueristic will be smaller

                    if (tileNeighbour.getClaimant() == null) {
                        distance += 10; // TODO link this to building price
                    } else if (tileNeighbour.getClaimant().getNetworkObject().getOwnerId()
                            != mOwnerId) {
                        distance +=
                                tileNeighbour
                                        .getBuilding()
                                        .getAttackCost(); // This adds the cost of attack.
                    } else {
                        // Don't do anything as its claimed by you so you want to go over it
                    }
                    try {
                    addConnection(
                            mNodeNum, mapEntry.getValue().getNode(), distance); // Weight set to 10
                    }
                    catch (Exception e) {
                    	log.severe("Exception -- not sure how is here");
                    }
                    
                }
            }
        }
        mNodeNum++;
    }

    /**
     * This will add a node to a graph with no connections
     *
     * @param nodeToAdd The node number
     * @throws GraphNodeException If the node already exists
     */
    public void addNode(int nodeToAdd) throws GraphNodeException {

        Node node =
                graph.get(nodeToAdd); // Gets the connection if in the graph. If not there gets null

        if (node == null) { // If the node is not in the graph
            Node newNode = new Node(nodeToAdd); // Makes a new node
            graph.put(nodeToAdd, newNode); // Adds to graph

        } else {
            throw new GraphNodeException();
        }
    }

    /**
     * This will add a node to a graph with no connections
     *
     * @param nodeToAdd The node number
     * @param tile The {@code HexagonTile} it corresponds to
     * @throws GraphNodeException If the node already exists
     */
    public void addNode(int nodeToAdd, HexagonTile tile) throws GraphNodeException {

        Node node =
                graph.get(nodeToAdd); // Gets the connection if in the graph. If not there gets null

        if (node == null) { // If the node is not in the graph
            Node newNode = new Node(nodeToAdd); // Makes a new node
            graph.put(nodeToAdd, newNode); // Adds to graph

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
                graph.get(
                        originNode); // Gets the connection if in the graph. If not there gets null

        if (node == null) { // If the node is not in the graph
            addNode(originNode); // Adds the node to the graph
            node = graph.get(originNode); // Gets the actual edge
        }
        Connection foundConnection = findConnection(originNode, destinationNode);

        if (foundConnection == null) {
            node.addConnection(destinationNode, weight); // Adds the connection to the node

            Node nodeEnd = graph.get(destinationNode); // Gets the destination node

            if (nodeEnd == null) { // Adds the node to the graph if it does not exist

                addNode(destinationNode);
            }
        } else {
            throw new GraphException();
        }
    }

    /**
     * Removes a node from the graph
     *
     * @param nodeToRemove the node to remove
     * @throws GraphNodeException If the node does not exist
     */
    public void removeNode(int nodeToRemove) throws GraphNodeException {

        Deque<Integer> keys = new ArrayDeque<Integer>(); // WIll hold all the keys and nodes
        Deque<Node> nodes = new ArrayDeque<Node>();
        Node finalNode = null;

        for (Map.Entry<Integer, Node> entry :
                graph.entrySet()) { // Takes each pair of values in the graph

            Node node = entry.getValue(); // Gets the node
            int key = entry.getKey(); // Gets the key
            if (node.getNode() == nodeToRemove) {
                finalNode = node;
            }

            node.removeConnection(nodeToRemove); // Removes the connection from the node
            keys.add(key); // Adds both to respective queues
            nodes.add(node);
        }

        if (finalNode == null) {
            throw new GraphNodeException();
        } else {

            for (int i = 0; i < keys.size(); i++) { // For each node and key
                int key = keys.remove();
                Node node = nodes.remove();

                graph.replace(key, node); // Replaces the data
            }
        }
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
                graph.entrySet()) { // For each pair of values in the hash map

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
     * Removes a connection between nodes
     *
     * @param originNode The origin node
     * @param destinationNode The destination node
     * @throws GraphNodeException If the origin does not exist
     */
    public void removeConnection(int originNode, int destinationNode) throws GraphNodeException {

        Node origin = graph.get(originNode); // Gets the node

        if (origin == null) {
            throw new GraphNodeException();
        } else {

            origin.removeConnection(destinationNode); // Removes the connection from the node
        }
    }

    /**
     * The number of nodes in the graph
     *
     * @return the size of the graph
     */
    public int getNumberOfNodes() {

        return graph.size();
    }

    /**
     * Returns the connections of that node
     *
     * @param nodeNum The node to find
     * @return The connections
     * @throws GraphNodeException If the node does not exist
     */
    public ArrayList<Connection> getConnection(int nodeNum) throws GraphNodeException {

        Node node = graph.get(nodeNum);
        if (node == null) {
            throw new GraphNodeException();
        } else {
            return graph.get(nodeNum).getConnections();
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

        Node node = graph.get(nodeToGet);
        if (node == null) {
            throw new GraphNodeException();
        } else {

            return graph.get(nodeToGet).getExtraInfo();
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

        Node node = graph.get(nodeToChange);
        if (node == null) {
            throw new GraphNodeException();
        }
        node.setExtraInfo(newInfo);
        graph.replace(nodeToChange, node);
    }

    /**
     * Will return all the node numbers in the graph
     *
     * @return An integer array which has all the Nodes used
     */
    public Integer[] getNodes() {

        ArrayList<Integer> nodes = new ArrayList<Integer>();

        for (Map.Entry<Integer, Node> entry : graph.entrySet()) {
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
     * If the graph contains that node
     *
     * @param node The node to look for
     * @return True if the node is in the graph false if not
     */
    public boolean inGraph(int node) {
        return graph.containsKey(node);
    }

    /**
     * Returns a node
     *
     * @param node The node to find
     * @return The actual {@code Node}
     */
    public Node getNode(Integer node) {
        return graph.get(node);
    }
}
