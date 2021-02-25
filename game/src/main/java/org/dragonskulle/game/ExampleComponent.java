/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.joml.Random;
import org.joml.Vector3f;

/**
 * An example component for the game engine
 *
 * @author Harry Stoltz
 *     <p>Simple component that chooses a random direction every fixed update and moves 2 units in
 *     that direction
 */
class ExampleComponent extends Component implements IFixedUpdate, IOnStart {

    final float VELOCITY = 2.f;
    Random r;

    long counter;
    long seed;

    ExampleComponent(long seed) {
        this.seed = seed;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        // Generate random direction and normalize it
        Vector3f dir = new Vector3f(r.nextFloat(), r.nextFloat(), r.nextFloat()).normalize();

        // Scale the normalized direction by VELOCITY to get the translation
        // Velocity is in units per second, so multiply by deltaTime to get the
        // movement per "step"
        Vector3f translation = dir.mul(VELOCITY * deltaTime);

        // When translation is done we would be able to make object "look" at the direction

        // Perform the translation
        mGameObject.getTransform().translate(translation);

        counter++;

        if (counter % 30 == 0) {
            System.out.println(this.toString());
            System.out.println("\nLocal Pos: \n" + mGameObject.getTransform().getLocalMatrix());
            System.out.println("\nWorld Pos: \n" + mGameObject.getTransform().getWorldMatrix());
        }
    }

    @Override
    public void onStart() {
        r = new Random(seed);
        counter = 0;
    }
}
