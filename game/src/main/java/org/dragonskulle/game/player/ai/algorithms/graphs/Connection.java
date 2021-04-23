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

    /** This is the origin node */
    @Getter private int mOriginNode;

    /** This is the destination node */
    @Getter private int mDestinationNode;

    /** This is the mWeight of the edge */
    @Getter private int mWeight;

    /**
     * The constructor for the class.
     *
     * @param originNode The mOriginNode for the connection
     * @param destinationNode The mDestinationNode for the connection
     * @param weight The mWeight of the node
     */
    public Connection(int originNode, int destinationNode, int weight) {
        this.mOriginNode = originNode;
        this.mDestinationNode = destinationNode;
        this.mWeight = weight;
    }
}
