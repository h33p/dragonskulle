package com.natlowis.ai.search.informed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.natlowis.ai.exceptions.GraphNodeException;
import com.natlowis.ai.graphs.Connection;
import com.natlowis.ai.graphs.Graph;
import com.natlowis.ai.search.SearchAlgorithm;

/**
 * Class which performs the A* Algorithm
 * @author low101043
 *
 */
public class AStar implements SearchAlgorithm {

	private ArrayList<double[]> frontier; // This will hold the nodes to be visited
	private Set<Integer> visited; // This will hold the nodes which has been visited
	private Graph graph; // This will hold the graph being processed
	private Deque<Integer> answerOfNodes; // This hold the solution of which nodes to visit
	private Deque<Connection> answerOfConnections; // This holds the connections which need to be visited

	private static Logger logger = Logger.getLogger(AStar.class);

	/**
	 * The constructor which allows you to make the object.
	 * 
	 * @param graph
	 */
	public AStar(Graph graph) {
		this.graph = graph;
		// Initialises all the needed variables
		frontier = new ArrayList<double[]>();
		visited = new HashSet<Integer>();
		answerOfNodes = new ArrayDeque<Integer>();
		answerOfConnections = new ArrayDeque<Connection>();

	}

	@Override
	public void algorithmToImplement(int currentNode, int endNode) throws GraphNodeException { 
																								
																								

		boolean finished = false; // This checks if it finished
		double oldFNode = 0; // This is what the previous f node value was
		ArrayList<int[]> connectionsFinal = new ArrayList<int[]>(); // This will hold the spare data which is needed

		while (!finished) {
			ArrayList<Connection> connections = graph.getConnection(currentNode); // Gets all the connections needed

			visited.add(currentNode); // Adds the current node to the visited stack

			for (int i = 0; i < connections.size(); i++) { // Go through each connection

				Connection connection = connections.get(i);
				int child = connection.getDestinationNode(); // Gets the destination node
				double destinationInfo = graph.getNodeSpecial(child); // Gets the heuristic info
				double weight = connection.getWeight() + oldFNode; // Gets the weight of the node and add the old
																	// weights known

				double fNode = destinationInfo + weight; // This is the fnode known

				if (!visited.contains(child)) { // If the child is not already visited
					if (search(child) == -1) { // If it is not in the frontier
						double[] toAdd = { child, fNode, weight, currentNode }; // Info to be added
						frontier.add(toAdd); // Added to frontier

					} else {
						int index = search(child); // Find the index of child
						double[] oldInfo = frontier.get(index); // Get the info

						if (oldInfo[1] > fNode) { // If the new info is smaller than the old info

							double[] toAdd = { child, fNode, weight, currentNode }; // The data to add
							frontier.remove(index); // Remove the current data
							frontier.add(toAdd); // Add the new data

						}
					}
				}
			}

			sort(); // Sorts the frontier

			if (!frontier.isEmpty()) { // As long as the frontier is not empty
				double[] nextNode = frontier.remove(0); // Removes the first element
				int[] connectionHere = { (int) nextNode[3], (int) nextNode[0] }; // The connection
				connectionsFinal.add(connectionHere); // Add it to the final connections
				if ((int) nextNode[0] == endNode) { // If it ends at the final node
					finished = true; // Finish the loop
					currentNode = (int) nextNode[0]; // Set the current node to the next node

				} else { // If it is not the end
					oldFNode = nextNode[2]; // Gets the weight
					currentNode = (int) nextNode[0]; // Set the current node to the next node
				}
			} else { // If the frontier is empty
				finished = true; // Finish the loop
			}

		}

		if (currentNode == endNode) { // If we have reached the end
			answerOfNodes.push(endNode); // Push the end Node

			for (int i = connectionsFinal.size() - 1; i >= 0; i--) { // Keeps pushing the next node on

				if (connectionsFinal.get(i)[1] == currentNode) { // If the node on this connection is the right one

					answerOfNodes.push(connectionsFinal.get(i)[0]);
					currentNode = connectionsFinal.get(i)[0];// }
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

		Integer[] answer = nodesToVisit(); // Gets the nodes to visit

		logger.debug(answer.length);

		for (int i = 1; i < answer.length; i++) {

			// Gets the correct connection
			int originNode = answer[i - 1];
			int endNode = answer[i];
			ArrayList<Connection> connection = null;
			try {
				connection = graph.getConnection(originNode);
			} catch (GraphNodeException e) {
				
				e.printStackTrace();
			}

			Connection finalConnection = null; // This will be the correct connection
			for (Connection connectionMaybe : connection) {
				if (originNode == connectionMaybe.getOriginNode() && connectionMaybe.getDestinationNode() == endNode) {
					finalConnection = connectionMaybe;
				}
			}

			answerOfConnections.add(finalConnection); // Add it to the data structire

		}

		return answerOfConnections.toArray(new Connection[0]);
	}

	/**
	 * Performs a sort on the frontier
	 */
	private void sort() {

		frontier = mergesort(frontier, 0, frontier.size() - 1);
	}

	/**
	 * Performs a merge sort on the data
	 * 
	 * @param data  The data to be sorted
	 * @param left  The left index
	 * @param right the right index
	 * @return the data sorted
	 */
	private ArrayList<double[]> mergesort(ArrayList<double[]> data, int left, int right) {

		if (left < right) { // While the left and right points are the correct ends

			int mid = (left + right) / 2; // Finds the mid index
			data = mergesort(data, left, mid); // Sorts the left side
			data = mergesort(data, mid + 1, right); // Sorts the right side
			data = merge(data, left, mid, right); // Merges the 2 sides together

		}
		return data;
	}

	/**
	 * Merges the data in mergesort
	 * 
	 * @param a     The data to be merged
	 * @param left  the left index
	 * @param mid   the middle of the data
	 * @param right The right index
	 * @return the data sorted
	 */
	private ArrayList<double[]> merge(ArrayList<double[]> a, int left, int mid, int right) {

		double[][] b = new double[right - left + 1][2]; // The array which will be sorted
		int bcount = 0; // Where you are in the b array
		int lcount = left; // Where you are in the left side
		int rcount = mid + 1; // Where you are in the right side

		while ((lcount <= mid) && (rcount <= right)) { // Whilst both sides are not sorted

			if (a.get(lcount)[1] <= a.get(rcount)[1]) { // If the data on the left side is smaller than the data on the
														// right side
				b[bcount] = a.get(lcount); // Put that data in the first available space in the b array
				bcount++; // Increase the b and l pointer
				lcount++;
			} else { // If the data on the right side is larger
				b[bcount] = a.get(lcount); // Put that data in the first available space in the b array
				bcount++; // Increase the b and l pointer
				rcount++;
			}
		}

		if (lcount > mid) { // If the l count is larger than mid (eg the left side is sorted)

			while (rcount <= right) { // Add all the data from the right side

				b[bcount] = a.get(rcount);
				bcount++;
				rcount++;
			}
		} else { // If the l count is smaller

			while (lcount <= mid) { // Add all the data from the left side

				b[bcount] = a.get(lcount);
				bcount++;
				lcount++;
			}
		}

		// Adds all the data sorted back into the array
		for (bcount = 0; bcount < right - left + 1; bcount++) {

			a.remove(left + bcount);
			a.add(left + bcount, b[bcount]);
		}

		return a;
	}

	/**
	 * Performs a linear search on the data
	 * 
	 * @param node The node to find
	 * @return The index if the node is there or -1 if it is not
	 */
	private int search(int node) {

		for (int i = 0; i < frontier.size(); i++) { // Goes through each element

			if ((int) frontier.get(i)[0] == node) { // If the element is what you're looking for return the index

				return i;
			}
		}
		return -1; // If not found return -1 (Cannot use it)
	}

}
