package com.natlowis.ai.unsupervised;

import java.util.ArrayList;
import java.util.Random;

import com.natlowis.ai.exceptions.GraphNodeException;
import com.natlowis.ai.graphs.Connection;
import com.natlowis.ai.graphs.Graph;

/**
 * This will perform Q-learning a Reinforcement learning algorithm
 * 
 * @author low101043
 *
 */
public class QLearning {

	private double[][] qTable; // This will hold the final Q table
	private Graph graph; // This will hold the graph being used
	private int endState; // Will hold the end state used

	/**
	 * The Constructor for QLearning. Takes a graph as a parameter
	 * 
	 * @param graph the graph to perform Q Learning on
	 */
	public QLearning(Graph graph) {

		this.graph = graph;
	}

	/**
	 * This will perform Q learning.
	 * 
	 * @param discountRate The discount rate for the algorithm. Higher value will
	 *                     mean more emphasis on the future actions
	 * @param learningRate The learning rate for the algorithm. Higher value will
	 *                     focus on the reward and the next action. Lower values
	 *                     will focus on what the current Q value is
	 * @param epoch        The amount of episodes to do.
	 * @param endState     What the destination node for the algorithm is
	 * @return A 2D array which is the final Q table with the above variables and
	 *         the given graph
	 */
	public double[][] qLearning(double discountRate, double learningRate, int epoch, int endState) {

		this.endState = endState; // Sets the end state

		qTable = new double[graph.getNumberOfNodes()][graph.getNumberOfNodes()]; // Initialises the Q table

		for (int i = 0; i < epoch; i++) { // For each episode

			Random rand = new Random(); // Creates a Random Object
			int stateInt = rand.nextInt(graph.getNumberOfNodes()); // Gets a random state to start

			do { // Completes one episode

				// Gets all the data needed
				ArrayList<Connection> data = null;
				try {
					data = graph.getConnection(stateInt);
				} catch (GraphNodeException e) {
					
					e.printStackTrace();
				}
				int nextActionIndex = rand.nextInt(data.size()); // TODO Error here if no connections in graph!!
				Connection nextAction = data.get(nextActionIndex);
				double reward = nextAction.getWeight();

				int nextState = nextAction.getDestinationNode();
				ArrayList<Connection> nextActions = null;
				try {
					nextActions = graph.getConnection(nextState);
				} catch (GraphNodeException e) {
					
					e.printStackTrace();
				}
				double nextQValueFinal = Double.NEGATIVE_INFINITY;

				// Finds the connection with largest Q score in q Table
				for (Connection nextStates : nextActions) {
					double nextQValue = qTable[nextStates.getOriginNode()][nextStates.getDestinationNode()];
					if (nextQValue > nextQValueFinal) {
						nextQValueFinal = nextQValue;
					}
				}

				double oldQValue = qTable[stateInt][nextState];
				double newQValue = ((1 - learningRate) * oldQValue)
						+ (learningRate * (reward + (discountRate * nextQValueFinal)));
				qTable[stateInt][nextState] = newQValue; // Updates Bellman equation

				qTable[stateInt][nextState] = newQValue; // Updates table

				stateInt = nextState;

			} while (endState != stateInt);
		}

		return qTable;

	}

	/**
	 * This will work out the final solution for the graph from the stated startNode
	 * and what has been given as the end state
	 * 
	 * @param startNode The node to start from
	 * @return An array of which nodes to go to
	 */
	public Integer[] nodesToFinish(int startNode) {

		ArrayList<Integer> finalList = new ArrayList<Integer>();
		int currentNode = startNode;

		while (currentNode != endState) { // whilst the current node is not at the end state

			double[] nextValues = qTable[currentNode]; // Gets the array which has all the q values from that node
			int nextNode = 0;

			for (int index = 0; index < nextValues.length; index++) { // finds the largest q value

				if (nextValues[nextNode] < nextValues[index]) {
					nextNode = index;
				}
			}

			finalList.add(nextNode); // Adds this to the table
			currentNode = nextNode;
		}

		return finalList.toArray(new Integer[0]);

	}

	@Override
	public String toString() {

		String finalString = ""; // The final string to output

		int numOfDigits = (int) Math.log10(graph.getNumberOfNodes()) + 1; // Work out how many spaces there should be at
																			// the start

		for (int i = 0; i < numOfDigits; i++) { // Adds those space
			finalString += " ";
		}

		finalString += " ";

		for (int i = 0; i < graph.getNumberOfNodes(); i++) { // Adds all the numbers
			finalString += i + " ";
		}

		finalString += "\n";

		for (int i = 0; i < graph.getNumberOfNodes(); i++) { // This will add all the q values for each line
			finalString += i + " ";

			for (int j = 0; j < graph.getNumberOfNodes(); j++) {
				finalString += qTable[i][j] + " ";
			}

			finalString += "\n";
		}

		return finalString;
	}

}
