/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonTile;

/**
 * This is a class which contains all the info needed for a Node. This has been adapted from
 * Nathaniel Lowis's (one of our group members) repository:
 * https://github.com/low101043/aiProjectComputer
 *
 * @author Dragonskulle
 */
@Accessors(prefix = "m")
public class Node {

    /** The node number. */
    @Getter private int mNodeId;

    /** The heuristic for that node. */
    @Getter @Setter private int mHeuristic;

    /** The Connections from this node. */
    @Getter private ArrayList<Connection> mConnections = new ArrayList<Connection>();

    /** The {@code HexagonTile} this refers to. */
    @Getter private Reference<HexagonTile> mHexTile;

    /**
     * The constructor.
     *
     * @param nodeToAdd The node number.
     * @param tile The {@code HexagonTile} this refers to.
     */
    public Node(int nodeToAdd, HexagonTile tile) {
        mNodeId = nodeToAdd;
        mHexTile = new Reference<HexagonTile>(tile);
    }

    /**
     * A Setter which adds a new <Code> Connection </Code> to the node.
     *
     * @param destinationNode The Destination node.
     * @param weight the weight of the connection.
     */
    public void addConnection(int destinationNode, int weight) {

        mConnections.add(new Connection(mNodeId, destinationNode, weight));
    }
}
