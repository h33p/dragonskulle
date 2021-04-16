/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.HexagonTile.TileType;

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
    private Reference<HexagonTile> mTileAiming;

    
    /**
     * A constructor to change the whole map to a {@code Graph}
     * @param map The map 
     * @param tileAiming The tile being aimed at
     */
    public Graph(HexagonMap map, HexagonTile tileAiming) {
        mTileAiming = new Reference<HexagonTile>(tileAiming);
        mMap = map.getReference(HexagonMap.class);
        mNodeNum = 0;
        mGraph = new HashMap<Integer, Node>();
        map.getAllTiles().forEach(this::convertToNode);
        mNodeNum = 0;
        map.getAllTiles().forEach(this::addConnections);
    }
    
    /**
     * A Constructor to change the visible tiles into a map.
     * @param map The map
     * @param tileAiming The tile being aimed at
     * @param visibleTiles The part of the map which can be seen
     */
    public Graph(HexagonMap map, HexagonTile tileAiming, Stream<HexagonTile> visibleTiles) {
    	mTileAiming = new Reference<HexagonTile>(tileAiming);
        mMap = map.getReference(HexagonMap.class);
        mNodeNum = 0;
        mGraph = new HashMap<Integer, Node>();
        visibleTiles.forEach(this::convertToNode);
        mNodeNum = 0;
        visibleTiles.forEach(this::addConnections);
    }

    /**
     * This will convert a {@code HexagonTile} to a {@code Node} which can be used by a {@code
     * Graph}
     *
     * @param tile The {@code HexagonTile} to be made a node
     */
    private void convertToNode(HexagonTile tile) {

        if (tile.getTileType() == TileType.LAND) {

            addNode(mNodeNum, tile);

            int heuristic = tile.distTo(mTileAiming.get().getQ(), mTileAiming.get().getR());

            setNodeSpecial(mNodeNum, heuristic);

            mNodeNum++;
        } else {

            // log.warning("Tried to add somehting which isn't land");
            ;
        }
    }

    /**
     * This will add all the connections for a node
     *
     * @param tile The {@code HexagonTile} to add connections for in the graph
     */
    private void addConnections(HexagonTile tile) {
        if (tile.getTileType() == TileType.LAND) {
            ArrayList<HexagonTile> neighbourTiles = mMap.get().getTilesInRadius(tile, 1);  //TODO Can we make a way which does not need to use the full map??

            for (HexagonTile tileNeighbour : neighbourTiles) {
                for (Map.Entry<Integer, Node> mapEntry : mGraph.entrySet()) {
                    if (tileNeighbour.getQ() == mapEntry.getValue().getHexTile().get().getQ()
                            && tileNeighbour.getR()
                                    == mapEntry.getValue().getHexTile().get().getR()) {

                        addConnection(
                                mNodeNum, mapEntry.getValue().getNode(), 1); // Weight set to 1
                    }
                }
            }
            mNodeNum++;
        } else {;
        }
    }

    /**
     * This will add a node to a mGraph with no connections
     *
     * @param nodeToAdd The node number
     * @param tile The {@code HexagonTile} it corresponds to
     */
    public void addNode(int nodeToAdd, HexagonTile tile) {

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
     * Returns the special info for that node
     *
     * @param nodeToGet The node which has the special info to get
     * @return the extra info for that node
     */
    public int getNodeSpecial(int nodeToGet) {

        return mGraph.get(nodeToGet).getExtraInfo();
    }

    /**
     * A setter which sets the extra info
     *
     * @param nodeToChange The node to change
     * @param newInfo The extra info to change
     */
    public void setNodeSpecial(int nodeToChange, int newInfo) {

        Node node = mGraph.get(nodeToChange);

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
