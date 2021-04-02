package com.natlowis.ai.optimisation.antcolony;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.natlowis.ai.exceptions.GraphNodeException;
import com.natlowis.ai.graphs.Connection;
import com.natlowis.ai.graphs.Graph;

/**
 * Class which performs Ant Colony Optimisation
 * @author low101043
 *
 */
public class AntColonyOptimisation {

	Graph graph; // The graph to be traversed
	Ant[] antsArray; // The ants to be used
	double[] newPheromoneLevel; // The pheromone to be updated for each connection
	HashMap<int[], Integer> indexMap; // INdex map for each connection
	ArrayList<Integer> finalRoute; // The final route

	private static Logger logger = Logger.getLogger(AntColonyOptimisation.class);

	/**
	 * The Constructor which makes the object
	 * 
	 * @param graph The graph to be traversed
	 */
	public AntColonyOptimisation(Graph graph) { 
		this.graph = graph;
	}

	/**
	 * The actual algorithm for Ant Colony Optimisation
	 * 
	 * @param startNode      The start node
	 * @param endNode        the end node
	 * @param epoch          the number of times you want it repeated
	 * @param pheromoneLevel the evaporation level
	 * @param numOfAnts      The number of ants to use
	 */
	public void AntColonyOptimisationAlgorithm(int startNode, int endNode, int epoch, double pheromoneLevel,
			int numOfAnts) { 

		antsArray = new Ant[numOfAnts]; // Sets up the ants array with the right number of arrays
		newPheromoneLevel = new double[graph.getConnections().length]; // Sets up the new pheromone array for all
																		// connections

		indexMap = new HashMap<int[], Integer>(); // This will mean each connection has a unique index

		Object[][] connections = graph.getConnections(); // Get the connections

		for (int i = 0; i < connections.length; i++) { // For each connection

			Object[] connection = connections[i];
			int origin = (int) connection[0];
			int destination = (int) connection[1];

			int[] node = { origin, destination };

			indexMap.put(node, i); // Adds connection to the position in list
		}

		for (int i = 0; i < epoch; i++) { // The algorithm

			construct(startNode, endNode, numOfAnts);
			update(pheromoneLevel, endNode);
		}

		int currentNode = startNode; // Starts at the start node
		finalRoute = new ArrayList<Integer>(); // The final route
		finalRoute.add(currentNode); // Adds the start node to the final route

		while (currentNode != endNode) { // Whilst the current node is not the end node

			int nextNode = chooseNext(currentNode); // Chooses the next node
			finalRoute.add(nextNode); // Adds it the the final route
			currentNode = nextNode; // Changes the current node to the next node
		}

	}

	/**
	 * Constructs the routes which the ants have traversed
	 * 
	 * @param startNode The start node
	 * @param endNode   The end node
	 * @param numOfAnts The number of ants to use
	 */
	private void construct(int startNode, int endNode, int numOfAnts) {

		for (int i = 0; i < newPheromoneLevel.length; i++) { // For each connection starts the new pheromone level to 0

			newPheromoneLevel[i] = 0;
		}
		for (int i = 0; i < numOfAnts; i++) { // Creates all the ants and get them to construct routes

			Ant antToAdd = new Ant(i, graph, startNode, endNode);
			antsArray[i] = antToAdd;
			// newPheromoneLevel[i] = 1;
			antToAdd.start();

		}

		for (int i = 0; i < numOfAnts; i++) { // Halts the main thread until all the ants have finished 
												
			Thread threadToWait = antsArray[i];

			try {
				threadToWait.join();
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
		}

		logger.trace("Finished");

	}

	/**
	 * Will update the pheromone levels
	 * 
	 * @param pheromoneLevel The evaporation rate
	 * @param endNode        the end node
	 */
	private void update(double pheromoneLevel, int endNode) {

		for (Ant ant : antsArray) { // For each ant in the array

			ArrayList<Integer> route = ant.returnRoute(); // Gets the route

			if (route.get(route.size() - 1) == endNode) { // Checks the ant has finished

				double pheromoneToAdd = 1.0 / route.size(); // The amount of pheromone to add

				for (int i = 0; i < route.size() - 2; i++) { // Gets the route

					int index = -1;

					for (Map.Entry<int[], Integer> entry : indexMap.entrySet()) { // Finds the index which corresponds
																					// to the connection

						int[] key = entry.getKey();

						if (key[0] == route.get(i) && key[1] == route.get(i + 1)) {
							index = entry.getValue();
						}
					}

					double pheromone = newPheromoneLevel[index]; // Updates that pheromone
					pheromone += pheromoneToAdd;
					newPheromoneLevel[index] = pheromone;
				}
			}
		}
		updateNode(pheromoneLevel); // Updates each connection

		// Sets all the ants to null (Allows garbage collector to come
		for (int i = 0; i < antsArray.length; i++) {
			antsArray[i] = null;
		}

		// Sets the pheromone level to 0 again
		for (int i = 0; i < newPheromoneLevel.length; i++) {
			newPheromoneLevel[i] = 0;
		}
	}

	/**
	 * Updates specific connection pheromone levels
	 * 
	 * @param pheromoneLevel The evaporation rate
	 */
	private void updateNode(double pheromoneLevel) {

		Integer[] nodes = graph.getNodes(); // Gets the nodes

		for (int node : nodes) {

			ArrayList<Connection> connections = null;
			try {
				connections = graph.getConnection(node);
			} catch (GraphNodeException e) {
			
				e.printStackTrace();
			} // Gets the connections for that node

			for (int i = 0; i < connections.size(); i++) { // For each connection

				Connection connection = connections.get(i);
				int destination = connection.getDestinationNode();

				double oldPheromone = connection.getSpecial();
				int index = -1;

				for (Map.Entry<int[], Integer> entry : indexMap.entrySet()) { // Finds the correct connection
					int[] key = entry.getKey();

					if (key[0] == node && key[1] == destination) {
						index = entry.getValue();
					}
				}

				double newPheromoneToAdd = newPheromoneLevel[index]; // Gets the pheromone for that connection

				double newPheromone = ((1 - pheromoneLevel) * oldPheromone) + newPheromoneToAdd; // Updates the
																									// pheromone level

				try {
					graph.setSpecial(connection.getOriginNode(), connection.getDestinationNode(), newPheromone);
				} catch (GraphNodeException e) {
					
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * Will choose the next node to go to
	 * 
	 * @param currentNode The current node the algorithm is at
	 * @return The next node to go to
	 */
	private int chooseNext(int currentNode) {

		ArrayList<Connection> connections = null;
		try {
			connections = graph.getConnection(currentNode);
		} catch (GraphNodeException e) {
			
			e.printStackTrace();
		} // Gets the connection for that node

		int destination = -1;
		double finalProbability = 0;

		// This will choose as next node the one with the most amount of pheromone
		for (Connection connection : connections) {
			int currentDestination = connection.getDestinationNode();
			double pheromone = connection.getSpecial();

			if (pheromone > finalProbability) {
				destination = currentDestination;
				finalProbability = pheromone;
			}

		}

		return destination;

	}

	/**
	 * Returns the final route
	 * 
	 * @return An ArrayList which is the final route
	 */
	public ArrayList<Integer> finalRoute() {

		return finalRoute;
	}
}
