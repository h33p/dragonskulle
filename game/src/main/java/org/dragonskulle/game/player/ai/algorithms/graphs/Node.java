/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayList;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonTile;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * This is a class which contains all the info needed for a Node
 *
 * @author Dragonskulle
 */
@Accessors(prefix = "m")
public class Node {

    @Getter private int mNode; // The node number
    @Getter @Setter private double mExtraInfo; // The extra info for that node
    @Getter private ArrayList<Connection> mConnections; // The Connections from that node
    @Getter private Reference<HexagonTile> mHexTile;

    /**
     * This constructor assumes you know the extra info you want to be set
     *
     * @param num the number for the node
     * @param info the extra info for that node
     */
    public Node(int num, int info) {

        
        mExtraInfo = info;
    }

    /**
     * The constructor if you do not have any special info
     *
     * @param num The number for that node
     */
    public Node(int num) {

        mNode = num;
        mExtraInfo = 0;
        mConnections = new ArrayList<Connection>();
    }

    public Node(int nodeToAdd, HexagonTile tile) {
    	mConnections = new ArrayList<Connection>();
        mNode = nodeToAdd;
        mHexTile = new Reference<HexagonTile>(tile);
	}

	/**
     * A Setter which adds a new <Code> Connection </Code> to the node
     *
     * @param destinationNode The Destination node
     * @param weight the weight of the connection
     */
    public void addConnection(int destinationNode, double weight) {

        mConnections.add(new Connection(mNode, destinationNode, weight));
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

}
