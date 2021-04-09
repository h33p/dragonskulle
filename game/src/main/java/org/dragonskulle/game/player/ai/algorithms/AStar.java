/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.dragonskulle.game.player.ai.algorithms.exceptions.GraphNodeException;
import org.dragonskulle.game.player.ai.algorithms.graphs.Connection;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Class which performs the A* Algorithm
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
public class AStar {

    private ArrayList<double[]> mFrontier; // This will hold the nodes to be mVisited
    private Set<Integer> mVisited; // This will hold the nodes which has been mVisited
    private Graph mGraph; // This will hold the mGraph being processed
    @Getter private Deque<Integer> mAnswerOfNodes; // This hold the solution of which nodes to visit
    private Deque<Connection>
            mAnswerOfConnections; // This holds the connections which need to be mVisited

    /**
     * The constructor which allows you to make the object.
     *
     * @param mGraph
     */
    public AStar(Graph mGraph) {
        this.mGraph = mGraph;
        // Initialises all the needed variables
        mFrontier = new ArrayList<double[]>();
        mVisited = new HashSet<Integer>();
        mAnswerOfNodes = new ArrayDeque<Integer>();
        mAnswerOfConnections = new ArrayDeque<Connection>();
    }

    /**
     * This will perform the A* Search
     *
     * @param currentNode The node to start from
     * @param endNode The goal node
     * @throws GraphNodeException If there is a problem with the mGraph
     */
    public void aStarAlgorithm(int currentNode, int endNode) throws GraphNodeException {

        boolean finished = false; // This checks if it finished
        double oldFNode = 0; // This is what the previous f node value was
        ArrayList<int[]> connectionsFinal =
                new ArrayList<int[]>(); // This will hold the spare data which is needed

        while (!finished) {
            ArrayList<Connection> connections =
                    mGraph.getConnection(currentNode); // Gets all the connections needed

            mVisited.add(currentNode); // Adds the current node to the mVisited stack

            for (int i = 0; i < connections.size(); i++) { // Go through each connection

                Connection connection = connections.get(i);
                int child = connection.getDestinationNode(); // Gets the destination node
                double destinationInfo = mGraph.getNodeSpecial(child); // Gets the heuristic info
                double weight =
                        connection.getWeight()
                                + oldFNode; // Gets the weight of the node and add the old
                // weights known

                double fNode = destinationInfo + weight; // This is the fnode known

                if (!mVisited.contains(child)) { // If the child is not already mVisited
                    if (search(child) == -1) { // If it is not in the mFrontier
                        double[] toAdd = {child, fNode, weight, currentNode}; // Info to be added
                        mFrontier.add(toAdd); // Added to mFrontier

                    } else {
                        int index = search(child); // Find the index of child
                        double[] oldInfo = mFrontier.get(index); // Get the info

                        if (oldInfo[1] > fNode) { // If the new info is smaller than the old info

                            double[] toAdd = {child, fNode, weight, currentNode}; // The data to add
                            mFrontier.remove(index); // Remove the current data
                            mFrontier.add(toAdd); // Add the new data
                        }
                    }
                }
            }

            sort(); // Sorts the mFrontier

            if (!mFrontier.isEmpty()) { // As long as the mFrontier is not empty
                double[] nextNode = mFrontier.remove(0); // Removes the first element
                int[] connectionHere = {(int) nextNode[3], (int) nextNode[0]}; // The connection
                connectionsFinal.add(connectionHere); // Add it to the final connections
                if ((int) nextNode[0] == endNode) { // If it ends at the final node
                    finished = true; // Finish the loop
                    currentNode = (int) nextNode[0]; // Set the current node to the next node

                } else { // If it is not the end
                    oldFNode = nextNode[2]; // Gets the weight
                    currentNode = (int) nextNode[0]; // Set the current node to the next node
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
     * This will return a Connection Array with the connections for the solution
     *
     * @return A array of length 0 if it does not exist else a Connection array
     */
    public Connection[] solutionActions() {

        Integer[] answer = mAnswerOfNodes.toArray(new Integer[0]); // Gets the nodes to visit

        for (int i = 1; i < answer.length; i++) {

            // Gets the correct connection
            int originNode = answer[i - 1];
            int endNode = answer[i];
            ArrayList<Connection> connection = null;
            try {
                connection = mGraph.getConnection(originNode);
            } catch (GraphNodeException e) {

                e.printStackTrace();
            }

            Connection finalConnection = null; // This will be the correct connection
            for (Connection connectionMaybe : connection) {
                if (originNode == connectionMaybe.getOriginNode()
                        && connectionMaybe.getDestinationNode() == endNode) {
                    finalConnection = connectionMaybe;
                }
            }

            mAnswerOfConnections.add(finalConnection); // Add it to the data structire
        }

        return mAnswerOfConnections.toArray(new Connection[0]);
    }

    /** Performs a sort on the mFrontier */
    private void sort() {

        mFrontier = mergesort(mFrontier, 0, mFrontier.size() - 1);
    }

    /**
     * Performs a merge sort on the data
     *
     * @param data The data to be sorted
     * @param left The left index
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
     * @param a The data to be merged
     * @param left the left index
     * @param mid the middle of the data
     * @param right The right index
     * @return the data sorted
     */
    private ArrayList<double[]> merge(ArrayList<double[]> a, int left, int mid, int right) {

        double[][] b = new double[right - left + 1][2]; // The array which will be sorted
        int bcount = 0; // Where you are in the b array
        int lcount = left; // Where you are in the left side
        int rcount = mid + 1; // Where you are in the right side

        while ((lcount <= mid) && (rcount <= right)) { // Whilst both sides are not sorted

            if (a.get(lcount)[1]
                    <= a.get(rcount)[
                            1]) { // If the data on the left side is smaller than the data on the
                // right side
                b[bcount] =
                        a.get(lcount); // Put that data in the first available space in the b array
                bcount++; // Increase the b and l pointer
                lcount++;
            } else { // If the data on the right side is larger
                b[bcount] =
                        a.get(lcount); // Put that data in the first available space in the b array
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

        for (int i = 0; i < mFrontier.size(); i++) { // Goes through each element

            if ((int) mFrontier.get(i)[0]
                    == node) { // If the element is what you're looking for return the index

                return i;
            }
        }
        return -1; // If not found return -1 (Cannot use it)
    }
}
