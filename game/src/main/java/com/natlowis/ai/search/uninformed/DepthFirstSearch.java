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
 * This will implement the Depth First Search
 * 
 * @author low101043
 *
 */
public class DepthFirstSearch implements SearchAlgorithm {

	private Deque<Integer> frontier; // This will hold the nodes to be visited
	private Set<Integer> visited; // This will hold the nodes which has been visited
	private Graph graph; // This will hold the graph being processed
	private Graph tree; // This will hold the tree which is created by the algorithm
	private Deque<Integer> answerOfNodes; // This hold the solution of which nodes to visit
	private Deque<Connection> answerOfConnections; // This holds the connections which need to be visited

	/**
	 * This will construct the object. Needs a graph to be made
	 * 
	 * @param graphToImplement The graph to do the search on.
	 */
	public DepthFirstSearch(Graph graphToImplement) {

		// Initialises all the needed variables
		graph = graphToImplement;
		frontier = new ArrayDeque<Integer>();
		visited = new HashSet<Integer>();
		answerOfNodes = new ArrayDeque<Integer>();
		answerOfConnections = new ArrayDeque<Connection>();
		tree = new Graph();

	}

	@Override
	public void algorithmToImplement(int currentNode, int endNode) throws GraphNodeException {

		// If we have reached the end node
		if (currentNode == endNode) {
			answerOfNodes.add(currentNode); // Will add it to the answer of nodes

		} else { // If we are not at the end node

			ArrayList<Connection> connections = graph.getConnection(currentNode); // Gets the connections for the
																					// current node
			visited.add(currentNode); // Adds the current node to the visited set

			for (int i = connections.size() - 1; i >= 0; i--) { // Goes through all the connections in the list

				Connection connect = connections.get(i); // Gets the connections and the node number
				int child = connect.getDestinationNode();

				if (!visited.contains(child) && !frontier.contains(child)) { // If it has not been visited or not in the
																				// frontier

					try {
						tree.addConnection(currentNode, child, 1);
					} catch (GraphException e) {
						
						e.printStackTrace();
					} // Adds to the tree and frontier
					frontier.push(child);
				}
			}

			if (!frontier.isEmpty()) { // If the frontier is not empty //TODO Fix cos if it is not down the first tree
										// to look it will break very quickly
				algorithmToImplement(frontier.pop(), endNode); // Next node from the frontier is gone to

			}

			ArrayList<Connection> childrenOfTree = tree.getConnection(currentNode); // Gets all the connections for the
																					// current node in the tree

			for (Connection item : childrenOfTree) { // Goes through all connections

				if (item.getDestinationNode() == answerOfNodes.peek()) { // If the connection links to the next node in
																			// the tree it is added to the final
																			// solutions

					answerOfNodes.push(currentNode);
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