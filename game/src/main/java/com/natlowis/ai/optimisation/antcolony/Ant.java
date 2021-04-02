package com.natlowis.ai.optimisation.antcolony;

import java.util.ArrayList;
import java.util.Random;

import com.natlowis.ai.exceptions.GraphNodeException;
import com.natlowis.ai.graphs.Connection;
import com.natlowis.ai.graphs.Graph;

/**
 * A Class which behaves like an Ant in Ant Colony Optimisations
 * 
 * @author low101043
 *
 */
public class Ant extends Thread {

	// private static Logger logger = Logger.getLogger(Ant.class);
	@SuppressWarnings("unused")
	private int id; // The id number as uses threads
	private Graph graph; // The graph to be traversed
	private ArrayList<Integer> route; // The final route found
	private int currentNode; // The current node the ant will be
	private int endNode; // The end node
	private Random rand; // The random object

	/**
	 * The Constructor which sets up the Ant
	 * 
	 * @param id        Unique Id
	 * @param graph     The graph to be traversed
	 * @param startNode The start node
	 * @param endNode   The end Node
	 */
	public Ant(int id, Graph graph, int startNode, int endNode) {

		this.id = id;
		this.graph = graph;
		this.currentNode = startNode;
		this.endNode = endNode;

	}

	@Override
	public void run() {

		route = new ArrayList<Integer>(); // Sets the route
		construct();
	}

	/**
	 * Will construct the route to take.
	 */
	private void construct() {

		while (currentNode != endNode) { // While the current node is not equal to the end node
			int nextNode = chooseNext(); // Chooses the next node
			route.add(nextNode); // Adds this to the route
			currentNode = nextNode; // sets this to be the next node
		}
		eliminateLoops(); // Eliminates the loops in the route

		// TODO Kill ants if take too long
	}

	/**
	 * Chooses the next node to go to probabilistically.
	 * 
	 * @return the next node to go to
	 */
	private int chooseNext() {

		ArrayList<Connection> connections = null;
		try {
			connections = graph.getConnection(currentNode);
		} catch (GraphNodeException e) {
			e.printStackTrace();
		} // Gets the Connections for the node

		rand = new Random(); // Creates new random object

		double denominator = 0; // The denominator for the probabilities
		for (Connection connection : connections) { // Each connection from that node

			double pheromone = connection.getSpecial(); // Get its special info and add to the denomiator
			denominator += pheromone;
		}

		double randomNumber = rand.nextDouble(); // Gets a random number which is between 0 and 1

		int destination = -1; // The final destination
		double finalProbability = 0; // The lower probability

		for (Connection connection : connections) { // For each connection

			int currentDestination = connection.getDestinationNode(); // Gets the destination
			double pheromone = connection.getSpecial(); // Gets the pheromone level
			double upperProbability = (pheromone / denominator) + finalProbability; // Gets the upper proababilty
			if (randomNumber <= upperProbability && randomNumber >= finalProbability) { // If the number is in between
																						// these two probabilities it
																						// should update itself
				destination = currentDestination;

			}
			finalProbability = upperProbability;
		}

		return destination;
	}

	/**
	 * Removes loops in the route chosen. Taken from my DSA assignment 2
	 */
	private void eliminateLoops() {

		boolean goneThroughWholeList = false; // Checks if gone through the whole list

		while (!goneThroughWholeList) {

			boolean foundData = false; // Checks if found the data
			int startPoint = -1;
			int finalPoint = -1;

			for (Integer node : route) { // For each node in the route

				int firstPoint = route.indexOf(node); // Gets the index
				int lastIndexOfData = -1;
				boolean found = false;

				for (int i = route.size() - 1; i > firstPoint; i--) { // This will check if the data is in the rest of
																		// the data
					int currentNode = route.get(i);

					if (!found && node == currentNode) {
						lastIndexOfData = i;
						found = true;
					}
				}

				if (lastIndexOfData != -1 && !foundData && lastIndexOfData != firstPoint) { // If it is the rest of the
																							// route AND is not at the
																							// same point
					startPoint = firstPoint; // Sets the first and last points
					finalPoint = lastIndexOfData;
					foundData = true;
				}
			}

			if (foundData == true) { // Removes loop WILL only remove one loop

				ArrayList<Integer> firstHalf = new ArrayList<Integer>(route.subList(0, startPoint));
				ArrayList<Integer> secondHalf = new ArrayList<Integer>(route.subList(finalPoint, route.size()));

				route = firstHalf;
				route.addAll(secondHalf);

			} else {
				goneThroughWholeList = true;
			}
		}
	}

	/**
	 * Returns the final route
	 * 
	 * @return An {@code ArrayList of Integers} which shows the route taken
	 */
	public ArrayList<Integer> returnRoute() {

		return route;
	}

}
