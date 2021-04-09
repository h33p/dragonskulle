/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayList;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonTile;

/**
 * This is a class which contains all the info needed for a Node
 *
 * @author Dragonskulle
 */
public class Node {

    private int mNodeNum; // The node number
    private double mExtraInfo; // The extra info for that node
    private ArrayList<Connection> mConnections; // The Connections from that node
    private Reference<HexagonTile> mTile;

    /**
     * This constructor assumes you know the extra info you want to be set
     *
     * @param num the number for the node
     * @param info the extra info for that node
     */
    public Node(int num, int info) {

        mConnections = new ArrayList<Connection>();
        mNodeNum = num;
        mExtraInfo = info;
    }

    /**
     * The constructor if you do not have any special info
     *
     * @param num The number for that node
     */
    public Node(int num) {

        mNodeNum = num;
        mExtraInfo = 0;
        mConnections = new ArrayList<Connection>();
    }

    /**
     * Returns the node number
     *
     * @return An <Code> int </Code> which is the number for that node
     */
    public int getNode() {

        return mNodeNum;
    }

    /**
     * A getter which returns the extra info
     *
     * @return An <Code> int </Code> which is the extra info for that node
     */
    public double getExtraInfo() {

        return mExtraInfo;
    }

    /**
     * A setter which edits the extra info
     *
     * @param newInfo The new info you want to edit
     */
    public void setExtraInfo(double newInfo) {

        mExtraInfo = newInfo;
    }

    /**
     * A Setter which adds a new <Code> Connection </Code> to the node
     *
     * @param destinationNode The Destination node
     * @param weight the weight of the connection
     */
    public void addConnection(int destinationNode, double weight) {

        mConnections.add(new Connection(mNodeNum, destinationNode, weight));
    }

    /**
     * Removes a <Code> Connection </Code> from the Node
     *
     * @param destinationNode The destination node for the connection to remove
     */
    public void removeConnection(int destinationNode) {

        // TODO assumption only 1 connection between each node. Need to enforce!

        Connection connectionToDelete = null; // Sets up the connection
        for (Connection edge : mConnections) {

            if (edge.getDestinationNode() == destinationNode) { // If the connection exists say so
                connectionToDelete = edge;
            }
        }

        if (connectionToDelete != null) { // Removes the connection
            mConnections.remove(connectionToDelete);
        }
    }

    /**
     * A getter which returns all the Connections
     *
     * @return the mConnections for that node
     */
    public ArrayList<Connection> getConnections() {

        return mConnections;
    }

    /**
     * Returns a reference to the hexagon mTile this node corresponds to
     *
     * @return The hexagon mTile this node corresponds to
     */
    public Reference<HexagonTile> getHexTile() {
        return mTile;
    }
}
