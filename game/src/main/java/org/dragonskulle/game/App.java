/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.dragonskulle.utils.Env.*;

import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.UnlitMaterial;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class App {

    private static final int INSTANCE_COUNT = envInt("INSTANCE_COUNT", 5);

    private static final Vector3fc[] COLOURS = {
        new Vector3f(1.f, 0.f, 0.f),
        new Vector3f(0.f, 1.f, 0.f),
        new Vector3f(0.f, 0.f, 1.f),
        new Vector3f(1.f, 0.5f, 0.f),
        new Vector3f(0.f, 1.f, 0.5f),
        new Vector3f(0.5f, 0.f, 1.f),
        new Vector3f(1.f, 1.f, 0.f),
        new Vector3f(0.f, 1.f, 1.f),
        new Vector3f(1.f, 0.f, 1.f),
    };

    private static final float SHIFT_FACTOR = (float) Math.pow(2.0, 1.0 / (float) INSTANCE_COUNT);

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) throws Exception {
        // Create a scene
        Scene mainScene = new Scene("mainScene");

        // The game needs a camera!
        GameObject camera = new GameObject("mainCamera");
        camera.addComponent(new Camera());

        // And it needs to be somewhere!
        Transform cameraTransform =
                new Transform(
                        new Matrix4f()
                                .lookAt(
                                        new Vector3f(2.0f, 2.0f, 2.0f),
                                        new Vector3f(0.0f, 0.0f, -0.05f),
                                        new Vector3f(0.0f, 0.0f, 1.0f)));

        // And it needs to be in the game
        mainScene.addRootObject(GameObject.instantiate(camera, cameraTransform));

        // Create a hexagon template
        GameObject hexagon = new GameObject("hexagon");

        // Add a renderable to it
        hexagon.addComponent(new Renderable());
        Reference<Renderable> hexRenderer = hexagon.getComponent(Renderable.class);
        UnlitMaterial hexMaterial = hexRenderer.get().getMaterial(UnlitMaterial.class);

        // Add spin and wobble components
        hexagon.addComponent(new Spinner());
        Reference<Spinner> hexSpinner = hexagon.getComponent(Spinner.class);
        hexagon.addComponent(new Wobbler());

        // Create instances, change up some parameters
        for (int i = 0; i < INSTANCE_COUNT; i++) {
            hexMaterial.colour.set(COLOURS[i % COLOURS.length]);
            hexSpinner.get().spinSpeed *= SHIFT_FACTOR;
            hexSpinner.get().sineAmplitude *= SHIFT_FACTOR;

            // Actually create an instance
            mainScene.addRootObject(GameObject.instantiate(hexagon));
        }

        // Create a cube
        GameObject cube = new GameObject("cube");
        cube.addComponent(new Renderable(Mesh.CUBE, new UnlitMaterial()));

        // You spin me right round...
        cube.addComponent(new Spinner(-360.f, 1000.f, 0.1f));

        // Aaand, spawn it!
        mainScene.addRootObject(cube);

        // Run the game
        Engine.getInstance().start("Germany", new GameBindings(), mainScene);
    }
}
