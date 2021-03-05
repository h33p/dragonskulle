package org.dragonskulle.game.components;

import org.dragonskulle.components.IFixedUpdate;

/**
 * This base class will allow AI players to be created and used throughout the game.
 * @author Oscar L
 */
public class AiPlayer extends Player implements IFixedUpdate {
    @Override
    protected void onDestroy() {

    }

    @Override
    public void fixedUpdate(float deltaTime) {

    }

    /**
     * This will simulate the action to be done by the AI player.  For the base class this will be done using probability
     */
    private void simulateInput(){

    }
}
