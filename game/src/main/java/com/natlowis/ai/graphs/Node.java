package com.natlowis.ai.graphs;

import java.util.ArrayList;

/**
 * This is a class which contains all the info needed for a Node
 * 
 * @author low101043
 *
 */
public class Node {

	private int nodeNum; // The node number
	private double extraInfo; // The extra info for that node
	private ArrayList<Connection> connections; // The Connections from that node

	/**
	 * This constructor assumes you know the extra info you want to be set
	 * 
	 * @param num  the number for the node
	 * @param info the extra info for that node
	 */
	public Node(int num, int info) {

		connections = new ArrayList<Connection>();
		nodeNum = num;
		extraInfo = info;
	}

	/**
	 * The constructor if you do not have any special info
	 * 
	 * @param num The number for that node
	 */
	public Node(int num) {

		nodeNum = num;
		extraInfo = 0;
		connections = new ArrayList<Connection>();
	}

	/**
	 * Returns the node number
	 * 
	 * @return An <Code> int </Code> which is the number for that node
	 */
	public int getNode() {

		return nodeNum;
	}

	/**
	 * A getter which returns the extra info
	 * 
	 * @return An <Code> int </Code> which is the extra info for that node
	 */
	public double getExtraInfo() {

		return extraInfo;
	}

	/**
	 * A setter which edits the extra info
	 * 
	 * @param newInfo The new info you want to edit
	 */
	public void setExtraInfo(double newInfo) {

		extraInfo = newInfo;
	}

	/**
	 * A Setter which adds a new <Code> Connection </Code> to the node
	 * 
	 * @param destinationNode The Destination node
	 * @param weight          the weight of the connection
	 */
	public void addConnection(int destinationNode, double weight) {

		connections.add(new Connection(nodeNum, destinationNode, weight));
	}

	/**
	 * Removes a <Code> Connection </Code> from the Node
	 * 
	 * @param destinationNode The destination node for the connection to remove
	 */
	public void removeConnection(int destinationNode) {

		// TODO assumption only 1 connection between each node. Need to enforce!

		Connection connectionToDelete = null; // Sets up the connection
		for (Connection edge : connections) {

			if (edge.getDestinationNode() == destinationNode) { // If the connection exists say so
				connectionToDelete = edge;
			}

		}

		if (connectionToDelete != null) { // Removes the connection
			connections.remove(connectionToDelete);
		}
	}

	/**
	 * A getter which returns all the Connections
	 * 
	 * @return the connections for that node
	 */
	public ArrayList<Connection> getConnections() {

		return connections;
	}
}