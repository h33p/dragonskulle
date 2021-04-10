/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The class which holds the information for a connection and provides access to this data
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mDestinationNode;
        result = prime * result + mOriginNode;
        long temp;
        temp = Double.doubleToLongBits(mWeight);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Connection other = (Connection) obj;
        if (mDestinationNode != other.mDestinationNode) return false;
        if (mOriginNode != other.mOriginNode) return false;
        if (Double.doubleToLongBits(mWeight) != Double.doubleToLongBits(other.mWeight)) return false;
        return true;
    }
}
