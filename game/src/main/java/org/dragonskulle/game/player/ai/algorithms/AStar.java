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

    private ArrayList<int[]> mFrontier = new ArrayList<int[]>();; // This will hold the nodes to be mVisited
    private Set<Integer> mVisited = new HashSet<Integer>(); // This will hold the nodes which has been mVisited
    private Graph mGraph; // This will hold the mGraph being processed
    @Getter private Deque<Integer> mAnswerOfNodes = new ArrayDeque<Integer>(); // This hold the solution of which nodes to visit

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

            sort(); // Sorts the mFrontier

            if (!mFrontier.isEmpty()) { // As long as the mFrontier is not empty
                int[] nextNode = mFrontier.remove(0); // Removes the first element
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

    /** Performs a sort on the mFrontier */
    private void sort() {

        mergesort(0, mFrontier.size() - 1);
    }

    /**
     * Performs a merge sort on the data
     *
     * @param data The data to be sorted
     * @param left The left index
     * @param right the right index
     * @return the data sorted
     */
    private void mergesort(int left, int right) {

        if (left < right) { // While the left and right points are the correct ends

            int mid = (left + right) / 2; // Finds the mid index
            mergesort(left, mid); // Sorts the left side
            mergesort(mid + 1, right); // Sorts the right side
            merge(left, mid, right); // Merges the 2 sides together
        }
    }

    /**
     * Merges the data in mergesort
     *
     * @param a The data to be merged
     * @param left the left index
     * @param mid the middle of the data
     * @param right The right index
     * @return the data sorted
     */
    private void merge(int left, int mid, int right) {

        int[][] b = new int[right - left + 1][2]; // The array which will be sorted
        int bcount = 0; // Where you are in the b array
        int lcount = left; // Where you are in the left side
        int rcount = mid + 1; // Where you are in the right side

        while ((lcount <= mid) && (rcount <= right)) { // Whilst both sides are not sorted

            if (mFrontier.get(lcount)[1]
                    <= mFrontier
                            .get(rcount)[
                            1]) { // If the data on the left side is smaller than the data on the
                // right side
                b[bcount] =
                        mFrontier.get(
                                lcount); // Put that data in the first available space in the b
                // array
                bcount++; // Increase the b and l pointer
                lcount++;
            } else { // If the data on the right side is larger
                b[bcount] =
                        mFrontier.get(
                                rcount); // Put that data in the first available space in the b
                // array
                bcount++; // Increase the b and l pointer
                rcount++;
            }
        }

        if (lcount > mid) { // If the l count is larger than mid (eg the left side is sorted)

            while (rcount <= right) { // Add all the data from the right side

                b[bcount] = mFrontier.get(rcount);
                bcount++;
                rcount++;
            }
        } else { // If the l count is smaller

            while (lcount <= mid) { // Add all the data from the left side

                b[bcount] = mFrontier.get(lcount);
                bcount++;
                lcount++;
            }
        }

        // Adds all the data sorted back into the array
        for (bcount = 0; bcount < right - left + 1; bcount++) {

            mFrontier.remove(left + bcount);
            mFrontier.add(left + bcount, b[bcount]);
        }
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
