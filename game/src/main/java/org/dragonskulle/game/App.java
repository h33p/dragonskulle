/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.Time;
import org.dragonskulle.renderer.RenderedApp;
import org.joml.Random;
import org.joml.Vector3f;

public class App extends RenderedApp {
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) {
        // Just a simple component that every fixed update chooses a random direction
        // and moves 5 units in that direction
        class ExampleComponent extends Component implements IFixedUpdate, IOnStart {

            final float VELOCITY = 2.f;
            Random r;

            long counter;

            @Override
            protected void onDestroy() {
            }

            @Override
            public void fixedUpdate(float deltaTime) {
                // Generate random direction and normalize it
                Vector3f dir =
                        new Vector3f(r.nextFloat(), r.nextFloat(), r.nextFloat()).normalize();

                // Scale the normalized direction by VELOCITY to get the translation
                // Velocity is in units per second, so multiply by deltaTime to get the
                // movement per "step"
                Vector3f translation = dir.mul(VELOCITY * deltaTime);

                // When translation is done we would be able to make object "look" at the direction


                // Perform the translation
                mGameObject.getTransform().translate(translation);

                counter++;

                if (counter % 100 == 0) {
                    System.out.println(mGameObject.getTransform().getLocalPosition());
                }
            }

            @Override
            public void onStart() {
                r = new Random((long) Time.getTimeInSeconds());
                counter = 0;
                System.out.println("\nStarting component");
                System.out.println(mGameObject.getTransform().getLocalPosition());
            }
        }

        Scene scene = new Scene("Main");

        GameObject root = new GameObject("root");

        GameObject randomlyMovingObject = new GameObject("Random Movement");

        randomlyMovingObject.addComponent(new ExampleComponent());

        root.addChild(randomlyMovingObject);
        scene.addRootObject(root);

        Engine.getInstance().start(scene);
    }
}
