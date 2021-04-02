package com.natlowis.ai.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Graph which implements {@code GraphInterface}. Will implement a directed
 * Graph data structure
 * 
 * @author low101043
 *
 */
public class GraphOriginal implements GraphInterface {

	protected Map<Integer, ArrayList<Connection>> graph; // The hash map which will have the integer to the arraylist of
															// connections

	/**
	 * Default Constructor for Graph Class. Initialises the empty graph
	 */
	public GraphOriginal() {
		graph = new HashMap<Integer, ArrayList<Connection>>(); // Initialises an empty hash map
	}

	/**
	 * This one assumes it takes data from a file
	 * 
	 * @param data
	 */
	public GraphOriginal(ArrayList<ArrayList<String>> data) {

		graph = new HashMap<Integer, ArrayList<Connection>>();
		for (ArrayList<String> list : data) { // For each line in the arraylist will add it as a connection

			addConnection(Integer.parseInt(list.get(0)), Integer.parseInt(list.get(1)),
					Double.parseDouble(list.get(2)));
		}
	}

	@Override
	public void addNode(int nodeToAdd) {

		ArrayList<Connection> connectionsToAdd = new ArrayList<Connection>(); // Creates an arraylist needed

		graph.put(nodeToAdd, connectionsToAdd); // Adds it to the hashmap
	}

	@Override
	public void addConnection(int originNode, int destinationNode, double weight) {

		List<Connection> edgeList = graph.get(originNode); // Gets the connection if in the graph. If not there gets
															// null

		if (edgeList == null) { // If the node is not in the graph
			addNode(originNode); // Adds the node to the graph
			edgeList = graph.get(originNode); // Gets the actual edge
		}
		edgeList.add(new Connection(originNode, destinationNode, weight)); // adds a new connection to the set of
																			// connections

		// now add the other node if not there
		edgeList = graph.get(destinationNode);
		if (edgeList == null) {
			addNode(destinationNode);
		}

	}

	@Override
	public void removeNode(int nodeToRemove) {

		graph.remove(nodeToRemove); // Removes the node from the graph

		// This will remove all other connections to that node in the graph
		LinkedList<Object[]> dataToRemove = new LinkedList<Object[]>();

		for (Map.Entry<Integer, ArrayList<Connection>> entry : graph.entrySet()) { // Takes each pair of values in the
																					// graph

			int node = entry.getKey(); // Gets the node

			for (Connection edge : entry.getValue()) { // Takes each connection foor that node

				if (edge.getDestinationNode() == nodeToRemove) { // If the destination node is equal to the node to
																	// remove it will be removed

					double weight = edge.getWeight(); // Gets the weigh
					Object[] connectionToRemove = { node, nodeToRemove, weight }; // The array which has the values to
																					// be removed
					dataToRemove.add(connectionToRemove); // Added to the linked list

				}
			}
		}

		int sizeOfLinkedList = dataToRemove.size(); // Finds the size and then goes through the linked list of data
													// items

		for (int i = 0; i < sizeOfLinkedList; i++) {

			Object[] dataToDelete = dataToRemove.remove(); // removes it from the linked list
			int originNode = (int) dataToDelete[0]; // Gets all the data from the array
			int destinationNode = (int) dataToDelete[1];
			double weight = (double) dataToDelete[2];
			removeConnection(originNode, destinationNode, weight); // removes the connection
		}
	}

	/**
	 * This will remove a connection if you know the connections exact distance
	 * 
	 * @param originNode      - The origin node of the connection
	 * @param destinationNode - the destination node of the connection
	 * @param distance        - the weight of the connection
	 */
	public void removeConnection(int originNode, int destinationNode, double distance) {

		// TODO what to do if weight is wrong!

		ArrayList<Connection> dataToLose = graph.get(originNode); // Gets the node to lose
		Connection dataToBeLost = new Connection(originNode, destinationNode, distance); // makes a new connection
		dataToLose.remove(dataToBeLost); // removes the data from the connection
		graph.put(originNode, dataToLose); // adds it to the graph again

	}

	@Override
	public void removeConnection(int originNode, int destinationNode) {

		// TODO assumption only 1 connection between each node. Need to enforce!

		ArrayList<Connection> dataWillBeHere = graph.get(originNode); // Gets the data
		Connection connectionToDelete = null; // Sets up the connection
		for (Connection edge : dataWillBeHere) {

			if (edge.getDestinationNode() == destinationNode) { // If the connection exists say so
				connectionToDelete = edge;
			}

		}

		if (connectionToDelete != null) { // Removes the connection
			dataWillBeHere.remove(connectionToDelete);
		}
	}

	@Override
	public Object[][] getConnections() {

		ArrayList<Object[]> connections = new ArrayList<>(); // The object which will have all the connections

		for (Map.Entry<Integer, ArrayList<Connection>> entry : graph.entrySet()) { // For each pair of values in the
																					// hash map

			for (Connection edge : entry.getValue()) // Will add it to the connections

				connections.add(new Object[] { edge.getOriginNode(), edge.getDestinationNode(), edge.getWeight() });
		}

		return connections.toArray(new Object[0][0]);
	}

	@Override
	public int getNumberOfNodes() {
		return graph.size();
	}

	@Override
	public ArrayList<Connection> getConnection(int nodeNum) {
		return graph.get(nodeNum);
	}

	@Override
	public boolean inGraph(int node) {
		// TODO Auto-generated method stub
		return false;
	}

}