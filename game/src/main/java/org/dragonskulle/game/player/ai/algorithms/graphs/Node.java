/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonTile;

/**
 * This is a class which contains all the info needed for a Node This has been adapted from
 * Nathaniel Lowis's (one of our group members) repository:
 * https://github.com/low101043/aiProjectComputer
 *
 * @author Dragonskulle
 */
@Accessors(prefix = "m")
public class Node {

    @Getter private int mNode; // The node number
    @Getter @Setter private int mExtraInfo; // The extra info for that node
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
    public void addConnection(int destinationNode, int weight) {

        mConnections.add(new Connection(mNode, destinationNode, weight));
    }
}
