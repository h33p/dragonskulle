package com.natlowis.ai.search;

import com.natlowis.ai.exceptions.GraphNodeException;
import com.natlowis.ai.graphs.Connection;

/**
 * An interface which is what a search algorithm should be like
 * 
 * @author low101043
 *
 */
public interface SearchAlgorithm {

	/**
	 * This is the search algorithm for that class.
	 * 
	 * @param startNode The start node for the graph
	 * @param endNode   The end node for the graph
	 * @throws GraphNodeException
	 */
	public void algorithmToImplement(int startNode, int endNode) throws GraphNodeException; 
	
	/**
	 * This will give the nodes to visit for the solution which has been found by
	 * that algorithm
	 * 
	 * @return 2D array with the nodes to visit from the start node to the end node
	 */
	public Integer[] nodesToVisit();

	/**
	 * This will give the connections for the solution given by the algorithm
	 * 
	 * @return 2D array with the connections to visit from the start node to the end
	 *         node
	 */
	public Connection[] solutionActions();

}
