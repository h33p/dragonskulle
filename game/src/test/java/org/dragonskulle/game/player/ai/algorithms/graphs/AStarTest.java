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

    /** A Test which tests the A* algorithm on a larger {@link Graph} */
    @Test
    public void advancedAStar() {

        Graph graph = advancedGraph();

        AStar aStar = new AStar(graph, 1, 10);

        Deque<Integer> answers = aStar.getPath();

        assertEquals(5, answers.size());

        assertEquals(1, (int) answers.pop());
        assertEquals(6, (int) answers.pop());
        assertEquals(7, (int) answers.pop());
        assertEquals(9, (int) answers.pop());
        assertEquals(10, (int) answers.pop());
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

    /**
     * This will create a more complex {@link Graph}. Graph taken from
     * https://www.gatevidyalay.com/a-algorithm-a-algorithm-example-in-ai/
     *
     * @return The {@link Graph}
     */
    public Graph advancedGraph() {

        Graph graph = new Graph();

        for (int i = 1; i <= 10; i++) {
            graph.addNode(i, null);
        }

        graph.setNodeHeuristic(1, 10);
        graph.addConnection(1, 6, 3);
        graph.addConnection(1, 2, 6);

        graph.setNodeHeuristic(2, 8);
        graph.addConnection(2, 4, 2);
        graph.addConnection(2, 3, 3);
        graph.addConnection(2, 1, 6);

        graph.setNodeHeuristic(3, 5);
        graph.addConnection(3, 4, 1);
        graph.addConnection(3, 5, 5);
        graph.addConnection(3, 2, 3);

        graph.setNodeHeuristic(4, 7);
        graph.addConnection(4, 2, 2);
        graph.addConnection(4, 3, 1);
        graph.addConnection(4, 5, 3);

        graph.setNodeHeuristic(5, 3);
        graph.addConnection(5, 3, 5);
        graph.addConnection(5, 4, 8);
        graph.addConnection(5, 9, 5);
        graph.addConnection(5, 10, 5);

        graph.setNodeHeuristic(6, 6);
        graph.addConnection(6, 1, 3);
        graph.addConnection(6, 7, 1);
        graph.addConnection(6, 8, 7);

        graph.setNodeHeuristic(7, 5);
        graph.addConnection(7, 6, 1);
        graph.addConnection(7, 9, 3);

        graph.setNodeHeuristic(8, 3);
        graph.addConnection(8, 6, 7);
        graph.addConnection(8, 9, 2);

        graph.setNodeHeuristic(9, 1);
        graph.addConnection(9, 5, 5);
        graph.addConnection(9, 7, 3);
        graph.addConnection(9, 8, 2);
        graph.addConnection(9, 10, 3);

        graph.setNodeHeuristic(10, 0);
        graph.addConnection(10, 5, 5);
        graph.addConnection(10, 9, 3);

        return graph;
    }
}
