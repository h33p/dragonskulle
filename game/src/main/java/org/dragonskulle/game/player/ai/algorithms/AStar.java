/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.game.player.ai.algorithms.exceptions.GraphNodeException;
import org.dragonskulle.game.player.ai.algorithms.graphs.Connection;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;

/**
 * Class which performs the A* Algorithm This has been adapted from Nathaniel Lowis's (one of our
 * group members) repository: https://github.com/low101043/aiProjectComputer
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class AStar {

    /** This will hold the nodes to be visited */
    private ArrayList<int[]> mFrontier = new ArrayList<int[]>();

    /** This will hold the nodes which has been visited */
    private Set<Integer> mVisited = new HashSet<Integer>();

    /** This will hold the mGraph being processed */
    private Graph mGraph;

    /** This hold the solution of which nodes to visit */
    @Getter private Deque<Integer> mAnswerOfNodes = new ArrayDeque<Integer>();

    /**
     * The constructor which allows you to make the object.
     *
     * @param graph The {@code Graph} to use A* on
     */
    public AStar(Graph graph) {
        this.mGraph = graph;
    }

    /**
     * This will perform the A* Search
     *
     * @param currentNode The node to start from
     * @param endNode The goal node
     * @throws GraphNodeException If there is a problem with the graph -- shouldn't happen
     */
    public void aStarAlgorithm(int currentNode, int endNode) throws GraphNodeException {

        boolean finished = false; // This checks if it finished
        int oldFNode = 0; // This is what the previous f node value was
        ArrayList<int[]> connectionsFinal = new ArrayList<int[]>(); // This will hold the route

        while (!finished) {
            ArrayList<Connection> connections =
                    mGraph.getConnection(currentNode); // Gets all the connections needed

            mVisited.add(currentNode); // Adds the current node to the mVisited stack

            for (int i = 0; i < connections.size(); i++) { // Go through each connection

                Connection connection = connections.get(i);
                int child = connection.getDestinationNode(); // Gets the destination node
                int destinationInfo = mGraph.getNodeSpecial(child); // Gets the heuristic info
                int weight =
                        connection.getWeight()
                                + oldFNode; // Gets the weight of the node and add the old
                // weights known

                int fNode = destinationInfo + weight; // This is the fnode known

                if (!mVisited.contains(child)) { // If the child is not already mVisited
                    if (search(child) == -1) { // If it is not in the mFrontier
                        int[] toAdd = {child, fNode, weight, currentNode}; // Info to be added
                        mFrontier.add(toAdd); // Added to mFrontier

                    } else {
                        int index = search(child); // Find the index of child
                        int[] oldInfo = mFrontier.get(index); // Get the info

                        if (oldInfo[1] > fNode) { // If the new info is smaller than the old info

                            int[] toAdd = {child, fNode, weight, currentNode}; // The data to add
                            mFrontier.remove(index); // Remove the current data
                            mFrontier.add(toAdd); // Add the new data
                        }
                    }
                }
            }

            int nextNodeIndex = nextNode();

            if (nextNodeIndex != -1) { // As long as the mFrontier is not empty
                int[] nextNode =
                        mFrontier.remove(
                                nextNodeIndex); // Removes the element with the smallest fNode
                int[] connectionHere = {nextNode[3], nextNode[0]}; // The connection
                connectionsFinal.add(connectionHere); // Add it to the final connections
                if (nextNode[0] == endNode) { // If it ends at the final node
                    finished = true; // Finish the loop
                    currentNode = nextNode[0]; // Set the current node to the next node

                } else { // If it is not the end
                    oldFNode = nextNode[2]; // Gets the weight
                    currentNode = nextNode[0]; // Set the current node to the next node
                }
            } else { // If the mFrontier is empty
                finished = true; // Finish the loop
            }
        }

        if (currentNode == endNode) { // If we have reached the end
            mAnswerOfNodes.push(endNode); // Push the end Node

            for (int i = connectionsFinal.size() - 1;
                    i >= 0;
                    i--) { // Keeps pushing the next node on

                if (connectionsFinal.get(i)[1]
                        == currentNode) { // If the node on this connection is the right one

                    mAnswerOfNodes.push(connectionsFinal.get(i)[0]);
                    currentNode = connectionsFinal.get(i)[0];
                }
            }
        }
    }

    /**
     * This will go through mFrontier and will find the next node to expand by checking what the
     * fNode is.
     *
     * @return the index of the next node to check or -1 if the list is empty
     */
    private int nextNode() {
        int smallest = Integer.MAX_VALUE;
        int index = -1;
        int i = 0;
        for (int[] node : mFrontier) {
            if (node[1] < smallest) {
                smallest = node[1];
                index = i;
            }
            i++;
        }

        return index;
    }

    /**
     * Performs a linear search on the data
     *
     * @param node The node to find
     * @return The index if the node is there or -1 if it is not
     */
    private int search(int node) {

        for (int i = 0; i < mFrontier.size(); i++) { // Goes through each element

            if (mFrontier.get(i)[0]
                    == node) { // If the element is what you're looking for return the index

                return i;
            }
        }
        return -1; // If not found return -1 (Cannot use it)
    }
}
