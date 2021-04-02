package com.natlowis.ai.search.uninformed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.natlowis.ai.exceptions.GraphException;
import com.natlowis.ai.exceptions.GraphNodeException;
import com.natlowis.ai.graphs.Connection;
import com.natlowis.ai.graphs.Graph;
import com.natlowis.ai.search.SearchAlgorithm;

/**
 * This class will implement the Breadth First Algorithm
 * 
 * @author low101043
 *
 */
public class BreadthFirstSearch implements SearchAlgorithm {

	private Deque<Integer> frontier; // This will hold the nodes to be visited
	private Set<Integer> visited; // This will hold the nodes which has been visited
	private Graph graph; // This will hold the graph being processed
	private Graph tree; // This will hold the tree which is created by the algorithm
	private Deque<Integer> answerOfNodes; // This hold the solution of which nodes to visit
	private Deque<Connection> answerOfConnections; // This holds the connections which need to be visited

	/**
	 * This will construct the object.
	 * 
	 * @param graphToImplement This is the graph to do a Breadth First Search on.
	 */
	public BreadthFirstSearch(Graph graphToImplement) {

		// This just initialises all the needed variables
		graph = graphToImplement;
		frontier = new ArrayDeque<Integer>();
		visited = new HashSet<Integer>();
		answerOfNodes = new ArrayDeque<Integer>();
		answerOfConnections = new ArrayDeque<Connection>();
		tree = new Graph();

	}

	@Override
	public void algorithmToImplement(int currentNode, int endNode) throws GraphNodeException {

		ArrayList<Connection> connectionsToUse = graph.getConnection(currentNode); // This will get the connections for
																					// that node
		visited.add(currentNode); // This will add the node to the visited set

		for (Connection item : connectionsToUse) { // For each connection from the current node

			int child = item.getDestinationNode(); // It gets the destination node

			if (!visited.contains(child) && !frontier.contains(child)) { // Checks if the destination node is in the
																			// frontier or in the visited set

				// If it is not
				try {
					tree.addConnection(currentNode, child, 1);
				} catch (GraphException e) {
					
					e.printStackTrace();
				} // Adds the connection to the tree
				frontier.addLast(child); // Adds the node to the end of the frontier queue
			}

		}

		if (frontier.contains(endNode) == true) { // If the frontier now contains the end node

			// The end node and the current node is pushed to the answerOfNodes
			answerOfNodes.push(endNode);
			answerOfNodes.push(currentNode);

			// FInds the connection
			Connection finalNode = null;
			for (Connection connection : connectionsToUse) {
				if (connection.getDestinationNode() == endNode) {
					finalNode = connection;
				}
			}
			answerOfConnections.add(finalNode);

		} else if (!frontier.isEmpty()) { // If the frontier is not empty //TODO Fix cos if it is not down the first
											// tree to look it will break very quickly

			algorithmToImplement(frontier.removeFirst(), endNode); // Will do BFS on the next node in the frontier
			ArrayList<Connection> childrenOfTree = tree.getConnection(currentNode); // Gets the connections for the
																					// current node

			for (Connection item : childrenOfTree) { // Will go through each connection

				if (item.getDestinationNode() == answerOfNodes.peek()) { // If the connection links to the last node in
																			// the answerOfNodes
					answerOfNodes.push(currentNode); // Will be added to both stacks
					answerOfConnections.push(item);
				}
			}
		}
	}

	@Override
	public Integer[] nodesToVisit() {

		return answerOfNodes.toArray(new Integer[0]);
	}

	@Override
	public Connection[] solutionActions() {

		return answerOfConnections.toArray(new Connection[0]);
	}

}
