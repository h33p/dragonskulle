package com.natlowis.ai.graphs;

/**
 * The class which holds the information for a connection and provides access to
 * this data
 * 
 * @author low101043
 */
public class Connection {

	protected int originNode; // This is the origin node
	protected int destinationNode; // This is the destination node
	protected double weight; // This is the weight of the edge
	protected double specialInfo; // This is the special info

	/**
	 * The constructor for the class.
	 * 
	 * @param node1  - The originNode for the connection
	 * @param node2  - The destinationNode for the connection
	 * @param weight - The weight of the node
	 */
	public Connection(int node1, int node2, double weight) {
		this.originNode = node1;
		this.destinationNode = node2;
		this.weight = weight;
		this.specialInfo = 1;

	}

	/**
	 * A setter which changes the weight of the connection
	 * 
	 * @param newWeight - The new weight for the node
	 */
	public void changeWeight(int newWeight) {
		weight = newWeight;
	}

	/**
	 * A getter which gets the origin Node
	 * 
	 * @return - Integer which is the origin Node of the connection
	 */
	public int getOriginNode() {
		return originNode;
	}

	/**
	 * A getter which gets the destination node
	 * 
	 * @return - Integer which is the destination node of the connection#
	 */
	public int getDestinationNode() {
		return destinationNode;
	}

	/**
	 * A getter which gets the weight of the node
	 * 
	 * @return - {@code double} which is the weight of the node
	 */
	public double getWeight() {
		return weight;
	}

	/**
	 * This will return the special info for that node
	 * 
	 * @return The special info.
	 */
	public double getSpecial() {
		return specialInfo;
	}

	/**
	 * Sets the special info for that node
	 * 
	 * @param newSpecial {@code double} which is the new special info
	 */
	public void setSpecial(double newSpecial) {
		this.specialInfo = newSpecial;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + destinationNode;
		result = prime * result + originNode;
		long temp;
		temp = Double.doubleToLongBits(weight);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Connection other = (Connection) obj;
		if (destinationNode != other.destinationNode)
			return false;
		if (originNode != other.originNode)
			return false;
		if (Double.doubleToLongBits(weight) != Double.doubleToLongBits(other.weight))
			return false;
		return true;
	}

}
