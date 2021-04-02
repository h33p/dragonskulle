package com.natlowis.ai.graphs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import com.natlowis.ai.exceptions.GraphException;
import com.natlowis.ai.exceptions.GraphNodeException;

/**
 * 
 * Graph which implements {@code GraphInterface}. Will implement a directed
 * Graph data structure
 *
 * @author low101043
 *
 */
public class Graph implements GraphInterface {

	protected Map<Integer, Node> graph; // The hash map which will have the integer to the Node

	/**
	 * Constructor which assumes no data to add at start
	 */
	public Graph() {
		graph = new HashMap<Integer, Node>();
	}

	/**
	 * This one assumes it takes data from a file
	 * 
	 * @param data
	 * @throws GraphNodeException
	 */
	public Graph(ArrayList<ArrayList<String>> data) throws NumberFormatException, GraphException, GraphNodeException {

		graph = new HashMap<Integer, Node>();

		for (ArrayList<String> list : data) { // For each line in the arraylist will add it as a connection

			addConnection(Integer.parseInt(list.get(0)), Integer.parseInt(list.get(1)),
					Double.parseDouble(list.get(2)));
		}
	}

	/**
	 * Used if the data has special info for each node
	 * 
	 * @param dataNodes       The nodes to add with the special info
	 * @param dataConnections The connections for the graph
	 */
	public Graph(ArrayList<ArrayList<String>> dataNodes, ArrayList<ArrayList<String>> dataConnections)
			throws NumberFormatException, GraphException, GraphNodeException {

		graph = new HashMap<Integer, Node>();

		for (ArrayList<String> list : dataNodes) { // WIll add all the nodes with the special info
			addNode(Integer.parseInt(list.get(0)));
			setNodeSpecial(Integer.parseInt(list.get(0)), Double.parseDouble(list.get(1)));
		}

		for (ArrayList<String> list : dataConnections) { // For each line in the arraylist will add it as a connection

			addConnection(Integer.parseInt(list.get(0)), Integer.parseInt(list.get(1)),
					Double.parseDouble(list.get(2)));
		}
	}

	@Override
	public void addNode(int nodeToAdd) throws GraphNodeException {

		Node node = graph.get(nodeToAdd); // Gets the connection if in the graph. If not there gets null

		if (node == null) { // If the node is not in the graph
			Node newNode = new Node(nodeToAdd); // Makes a new node
			graph.put(nodeToAdd, newNode); // Adds to graph

		} else {
			throw new GraphNodeException();
		}

	}

	@Override
	public void addConnection(int originNode, int destinationNode, double weight)
			throws GraphException, GraphNodeException {

		Node node = graph.get(originNode); // Gets the connection if in the graph. If not there gets null

		if (node == null) { // If the node is not in the graph
			addNode(originNode); // Adds the node to the graph
			node = graph.get(originNode); // Gets the actual edge
		}
		Connection foundConnection = findConnection(originNode, destinationNode);

		if (foundConnection == null) {
			node.addConnection(destinationNode, weight); // Adds the connection to the node

			Node nodeEnd = graph.get(destinationNode); // Gets the destination node

			if (nodeEnd == null) { // Adds the node to the graph if it does not exist

				addNode(destinationNode);
			}
		} else {
			throw new GraphException();
		}
	}

	@Override
	public void removeNode(int nodeToRemove) throws GraphNodeException {

		Deque<Integer> keys = new ArrayDeque<Integer>(); // WIll hold all the keys and nodes
		Deque<Node> nodes = new ArrayDeque<Node>();
		Node finalNode = null;

		for (Map.Entry<Integer, Node> entry : graph.entrySet()) { // Takes each pair of values in the graph

			Node node = entry.getValue(); // Gets the node
			int key = entry.getKey(); // Gets the key
			if (node.getNode() == nodeToRemove) {
				finalNode = node;
			}

			node.removeConnection(nodeToRemove); // Removes the connection from the node
			keys.add(key); // Adds both to respective queues
			nodes.add(node);

		}

		if (finalNode == null) {
			throw new GraphNodeException();
		} else {

			for (int i = 0; i < keys.size(); i++) { // For each node and key
				int key = keys.remove();
				Node node = nodes.remove();

				graph.replace(key, node); // Replaces the data
			}
		}

	}

	@Override
	public Object[][] getConnections() {

		ArrayList<Object[]> connections = new ArrayList<>(); // The object which will have all the connections

		for (Map.Entry<Integer, Node> entry : graph.entrySet()) { // For each pair of values in the hash map

			ArrayList<Connection> connection = entry.getValue().getConnections();

			for (Connection edge : connection) // Will add it to the connections

				connections.add(new Object[] { edge.getOriginNode(), edge.getDestinationNode(), edge.getWeight() });
		}

		return connections.toArray(new Object[0][0]);
	}

	@Override
	public void removeConnection(int originNode, int destinationNode) throws GraphNodeException {

		Node origin = graph.get(originNode); // Gets the node

		if (origin == null) {
			throw new GraphNodeException();
		} else {

			origin.removeConnection(destinationNode); // Removes the connection from the node
		}

	}

	@Override
	public int getNumberOfNodes() {

		return graph.size();
	}

	@Override
	public ArrayList<Connection> getConnection(int nodeNum) throws GraphNodeException {

		Node node = graph.get(nodeNum);
		if (node == null) {
			throw new GraphNodeException();
		} else {
			return graph.get(nodeNum).getConnections();
		}
	}

	/**
	 * Returns the special info for that node
	 * 
	 * @param nodeToGet The node which has the special info to get
	 * @return the extra info for that node
	 * @throws GraphNodeException
	 */
	public double getNodeSpecial(int nodeToGet) throws GraphNodeException {

		Node node = graph.get(nodeToGet);
		if (node == null) {
			throw new GraphNodeException();
		} else {

			return graph.get(nodeToGet).getExtraInfo();
		}
	}

	/**
	 * A setter which sets the extra info
	 * 
	 * @param nodeToChange The node to change
	 * @param newInfo      The extra info to change
	 * @throws GraphNodeException
	 */
	public void setNodeSpecial(int nodeToChange, double newInfo) throws GraphNodeException {

		Node node = graph.get(nodeToChange);
		if (node == null) {
			throw new GraphNodeException();
		}
		node.setExtraInfo(newInfo);
		graph.replace(nodeToChange, node);
	}

	/**
	 * Will return all the node numbers in the graph
	 * 
	 * @return An integer array which has all the Nodes used
	 */
	public Integer[] getNodes() {

		ArrayList<Integer> nodes = new ArrayList<Integer>();

		for (Map.Entry<Integer, Node> entry : graph.entrySet()) {
			Integer node = entry.getKey();
			nodes.add(node);
		}

		return nodes.toArray(new Integer[0]);
	}

	/**
	 * Will set the special info for a connection
	 * 
	 * @param originNode      The origin node for the connection
	 * @param destinationNode The destination node for the connection
	 * @param newSpecial      The new special info needed
	 * @throws GraphNodeException
	 */
	public void setSpecial(int originNode, int destinationNode, double newSpecial) throws GraphNodeException {

		ArrayList<Connection> connections = this.getConnection(originNode);
		Connection connectionToFind = null;

		for (Connection connection : connections) { // Finds the connections
			if (connection.getDestinationNode() == destinationNode) {
				connectionToFind = connection;
			}
		}
		connectionToFind.setSpecial(newSpecial);

	}

	/**
	 * This will find a specific connection
	 * 
	 * @param node        The node to start with
	 * @param destination the destination node
	 * @return The Connection if found
	 * @throws GraphNodeException
	 */
	private Connection findConnection(int node, int destination) throws GraphNodeException {

		ArrayList<Connection> connections = getConnection(node);
		for (Connection connection : connections) {
			if (connection.getDestinationNode() == destination) {
				return connection;
			}
		}
		return null;
	}

	@Override
	public boolean inGraph(int node) {
		return graph.containsKey(node);
	}

}
