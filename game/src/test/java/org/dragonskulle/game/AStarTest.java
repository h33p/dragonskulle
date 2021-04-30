/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertEquals;

import java.util.Deque;
import org.dragonskulle.game.player.ai.algorithms.AStar;
import org.dragonskulle.game.player.ai.algorithms.graphs.Graph;
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

        Graph graph = new Graph(true);

        AStar aStar = new AStar(graph, 4, 5);

        Deque<Integer> answers = aStar.getPath();
        assertEquals(3, answers.size());

        assertEquals(4, (int) answers.pop());
        assertEquals(2, (int) answers.pop());
        assertEquals(5, (int) answers.pop());
    }
}
