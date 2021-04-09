/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

/**
 * The class which holds the information for a connection and provides access to this data
 *
 * @author Dragonskulle
 */
public class Connection {

    protected int mOriginNode; // This is the origin node
    protected int mDestinationNode; // This is the destination node
    protected double mWeight; // This is the mWeight of the edge

    /**
     * The constructor for the class.
     *
     * @param node1 - The mOriginNode for the connection
     * @param node2 - The mDestinationNode for the connection
     * @param mWeight - The mWeight of the node
     */
    public Connection(int node1, int node2, double mWeight) {
        this.mOriginNode = node1;
        this.mDestinationNode = node2;
        this.mWeight = mWeight;
    }

    /**
     * A setter which changes the mWeight of the connection
     *
     * @param newWeight - The new mWeight for the node
     */
    public void changeWeight(int newWeight) {
        mWeight = newWeight;
    }

    /**
     * A getter which gets the origin Node
     *
     * @return - Integer which is the origin Node of the connection
     */
    public int getOriginNode() {
        return mOriginNode;
    }

    /**
     * A getter which gets the destination node
     *
     * @return - Integer which is the destination node of the connection#
     */
    public int getDestinationNode() {
        return mDestinationNode;
    }

    /**
     * A getter which gets the mWeight of the node
     *
     * @return - {@code double} which is the mWeight of the node
     */
    public double getWeight() {
        return mWeight;
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
