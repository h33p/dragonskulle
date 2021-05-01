/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.game.player.ai.algorithms.graphs.Connection;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;

/**
 * Class which performs the A* Algorithm. This has been adapted from Nathaniel Lowis's (one of our
 * group members) repository: https://github.com/low101043/aiProjectComputer.
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class AStar {

    /** This will hold the nodes to be visited with the nodeId being the key. */
    private HashMap<Integer, Frontier> mFrontier = new HashMap<Integer, Frontier>();

    /**
     * A class which holds the variables for a node in the frontier.
     *
     * @author DragonSkulle
     */
    private class Frontier {

        /** The child node. */
        private int mChild;

        /** The cost of getting to the child node. */
        private int mFNode;

        /** The weight of the connection. */
        private int mWeight;

        /** The parent node. */
        private int mParent;

        /**
         * Simple constructor.
         *
         * @param child The child node.
         * @param fNode The cost of getting to the child node.
         * @param weight The weight from parent to child.
         * @param currentNode The parent node.
         */
        public Frontier(int child, int fNode, int weight, int currentNode) {
            mChild = child;
            mFNode = fNode;
            mWeight = weight;
            mParent = currentNode;
        }
    }

    /**
     * Holds the details of the connections.
     *
     * @author DragonSkulle
     */
    private class ConnectionIds {

        /** The parent node. */
        private int mCurrentNode;

        /** The child node. */
        private int mChildNode;

        /**
         * Simple constructor.
         *
         * @param currentNode The parent node.
         * @param childNode the child node.
         */
        private ConnectionIds(int currentNode, int childNode) {
            mCurrentNode = currentNode;
            mChildNode = childNode;
        }
    }

    /** This will hold the node ids which has been visited. */
    private Set<Integer> mVisited = new HashSet<Integer>();

    /** This will hold the Graph being processed. */
    private Graph mGraph;

    /** This hold the solution of which nodes to visit. */
    @Getter private Deque<Integer> mPath = new ArrayDeque<Integer>();

    /**
     * The constructor which allows you to make the object.
     *
     * @param graph The {@link Graph} to use A* on.
     * @param currentNode The start {@link Node}'s nodeId.
     * @param endNode The end {@link Node}'s nodeId.
     */
    public AStar(Graph graph, int currentNode, int endNode) {
        this.mGraph = graph;
        aStarAlgorithm(currentNode, endNode);
    }

    /**
     * This will perform the A* Search.
     *
     * @param currentNode The {@link Node}'s nodeId to start from.
     * @param endNode The goal node's nodeId.
     */
    private void aStarAlgorithm(int currentNode, int endNode) {

        boolean finished = false; // This checks if it finished
        int oldFNode = 0; // This is what the previous f node value was
        ArrayList<ConnectionIds> connectionsFinal =
                new ArrayList<ConnectionIds>(); // This will hold the route

        while (!finished) {
            ArrayList<Connection> connections =
                    mGraph.getConnection(currentNode); // Gets all the connections needed

            mVisited.add(currentNode); // Adds the current node to the mVisited stack

            checkConnections(currentNode, connections, oldFNode);

            int nextNodeIndex = nextNode();

            if (nextNodeIndex != -1) { // As long as the mFrontier is not empty
                Frontier nextNode =
                        mFrontier.remove(
                                nextNodeIndex); // Removes the element with the smallest fNode
                ConnectionIds connectionHere = new ConnectionIds(nextNode.mParent, nextNode.mChild);

                connectionsFinal.add(connectionHere); // Add it to the final connections
                if (nextNode.mChild == endNode) { // If it ends at the final node
                    finished = true; // Finish the loop
                    currentNode = nextNode.mChild; // Set the current node to the next node

                } else { // If it is not the end
                    oldFNode = nextNode.mWeight; // Gets the weight
                    currentNode = nextNode.mChild; // Set the current node to the next node
                }
            } else { // If the mFrontier is empty
                finished = true; // Finish the loop
            }
        }

        if (currentNode == endNode) { // If we have reached the end
            mPath.push(endNode); // Push the end Node

            for (int i = connectionsFinal.size() - 1;
                    i >= 0;
                    i--) { // Keeps pushing the next node on

                // If the node on this connection is the right one
                if (connectionsFinal.get(i).mChildNode == currentNode) {

                    mPath.push(connectionsFinal.get(i).mCurrentNode);
                    currentNode = connectionsFinal.get(i).mCurrentNode;
                }
            }
        }
    }

    /**
     * This will check through all the connections and add to the frontier.
     *
     * @param currentNode The current node we are at.
     * @param connections An {@link ArrayList} of {@link Connections}'s which come off the current
     *     node.
     * @param oldFNode The weight to get to the currentNode.
     */
    private void checkConnections(
            int currentNode, ArrayList<Connection> connections, int oldFNode) {

        for (Connection connection : connections) { // Go through each connection

            int child = connection.getDestinationNode(); // Gets the destination node
            int heurusticInfo = mGraph.getNodeHeuristic(child); // Gets the heuristic info
            int weight =
                    connection.getWeight()
                            + oldFNode; // Gets the weight of the node and add the old
            // weights known

            int fNode = heurusticInfo + weight; // This is the fnode known

            if (!mVisited.contains(child)) { // If the child is not already mVisited
                if (!mFrontier.containsKey(child)) { // If it is not in the mFrontier

                    // Info to be added
                    Frontier toAdd = new Frontier(child, fNode, weight, currentNode);

                    mFrontier.put(child, toAdd); // Added to mFrontier

                } else {
                    Frontier oldInfo = mFrontier.get(child); // Get the info

                    if (oldInfo.mFNode > fNode) { // If the new info is smaller than the old info

                        Frontier toAdd = new Frontier(child, fNode, weight, currentNode);

                        mFrontier.remove(child); // Remove the current data
                        mFrontier.put(child, toAdd); // Add the new data
                    }
                }
            }
        }
    }

    /**
     * This will go through mFrontier and will find the next node to expand by checking what the
     * fNode is.
     *
     * @return the index of the next node to check or -1 if the list is empty.
     */
    private int nextNode() {
        int smallest = Integer.MAX_VALUE;
        int index = -1;
        for (Frontier node : mFrontier.values()) {
            if (node.mFNode < smallest) {
                smallest = node.mFNode;
                index = node.mChild;
            }
        }

        return index;
    }
}
