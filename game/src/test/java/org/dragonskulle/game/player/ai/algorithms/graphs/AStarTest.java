/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import static org.junit.Assert.assertEquals;

import java.util.Deque;
import org.dragonskulle.game.player.ai.algorithms.AStar;
import org.junit.Test;

/**
 * This will test the A* Algorithm
 *
 * @author DragonSkulle
 */
public class AStarTest {

    /** A Small test which shows the A* algorithm work on a small graph */
    @Test
    public void aStar() {

        Graph graph = basicGraph();

        AStar aStar = new AStar(graph, 4, 5);

        Deque<Integer> answers = aStar.getPath();
        assertEquals(3, answers.size());

        assertEquals(4, (int) answers.pop());
        assertEquals(2, (int) answers.pop());
        assertEquals(5, (int) answers.pop());
    }

    /**
     * A Basic Constructor which is used for testing Creates a Graph from A* lecture by Dr Miqing Li
     *
     * @return The {@code Graph} to test
     */
    public Graph basicGraph() {

        Graph graph = new Graph();

        graph.addNode(0, null);
        graph.addNode(1, null);
        graph.addNode(2, null);
        graph.addNode(3, null);
        graph.addNode(4, null);
        graph.addNode(5, null);

        graph.setNodeHeuristic(0, 10);
        graph.addConnection(0, 1, 4);
        graph.addConnection(0, 2, 10);

        graph.setNodeHeuristic(1, 14);

        graph.setNodeHeuristic(2, 4);
        graph.addConnection(2, 5, 6);

        graph.setNodeHeuristic(3, 1);
        graph.addConnection(3, 2, 3);
        graph.addConnection(3, 5, 4);

        graph.setNodeHeuristic(4, 8);
        graph.addConnection(4, 0, 2);
        graph.addConnection(4, 2, 4);
        graph.addConnection(4, 3, 8);

        graph.setNodeHeuristic(5, 0);

        return graph;
    }
}
