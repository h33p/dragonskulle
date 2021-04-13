/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The class which holds the information for a connection and provides access to this data This has
 * been adapted from Nathaniel Lowis's (one of our group members) repository:
 * https://github.com/low101043/aiProjectComputer
 *
 * @author Dragonskulle
 */
@Accessors(prefix = "m")
public class Connection {

    @Getter private int mOriginNode; // This is the origin node
    @Getter private int mDestinationNode; // This is the destination node
    @Getter private int mWeight; // This is the mWeight of the edge

    /**
     * The constructor for the class.
     *
     * @param node1 - The mOriginNode for the connection
     * @param node2 - The mDestinationNode for the connection
     * @param mWeight - The mWeight of the node
     */
    public Connection(int node1, int node2, int mWeight) {
        this.mOriginNode = node1;
        this.mDestinationNode = node2;
        this.mWeight = mWeight;
    }
}
