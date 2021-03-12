/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.dragonskulle.utils.Env.*;

import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.components.HumanPlayer;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.UnlitMaterial;
import org.dragonskulle.ui.*;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

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

        Transform tr = camera.getTransform();
        // Set where it's at
        tr.setPosition(0f, 0f, 1f);
        tr.rotateDeg(30f, 0f, 0f);
        tr.translateLocal(0f, -8f, 0f);
        // Make sure it's an actual camera
        camera.addComponent(new Camera());

        // And it needs to be in the game
        mainScene.addRootObject(camera);

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

        // Create a cube. This syntax is slightly different
        // This here, will allow you to "build" the cube in one go
        GameObject cube =
                new GameObject(
                        "cube",
                        (go) -> {
                            go.addComponent(new Renderable(Mesh.CUBE, new UnlitMaterial()));
                            // You spin me right round...
                            go.addComponent(new Spinner(-360.f, 1000.f, 0.1f));
                        });

        // Aaand, spawn it!
        mainScene.addRootObject(cube);

        // Create a monstrocity
        //
        // This demonstrates use of nested game objects. It's extremely powerful to compose objects
        // like so, and the lambda syntax is rather intuitive to build this hierarchy tree with.
        GameObject monstrocity =
                new GameObject(
                        "monstrocity",
                        (inMonstrocity) -> {
                            inMonstrocity.getTransform().setPosition(3.f, 3.f, 0.f);
                            inMonstrocity.addComponent(
                                    new Renderable(Mesh.CUBE, new UnlitMaterial()));
                            inMonstrocity.addComponent(new Spinner(360.f, 1000.f, 0.1f));

                            // Add a "arm" on the left side
                            inMonstrocity.buildChild(
                                    "leftSide",
                                    (side) -> {
                                        side.getTransform().setPosition(-1.5f, 0.f, 1.5f);
                                        side.addComponent(
                                                new Renderable(Mesh.CUBE, new UnlitMaterial()));
                                        side.addComponent(new Spinner(-180.f, 100.f, 1.f));

                                        // assume this is a hand
                                        side.buildChild(
                                                "leftHand",
                                                (tip) -> {
                                                    tip.getTransform().setPosition(0.f, 0.f, 0.5f);
                                                    tip.getTransform().scale(0.5f, 0.5f, 3.f);
                                                    tip.addComponent(
                                                            new Renderable(
                                                                    Mesh.CUBE,
                                                                    new UnlitMaterial()));
                                                    tip.addComponent(
                                                            new Spinner(360.f, 100.f, .1f));
                                                });
                                    });

                            // Do the same thing on the other side
                            inMonstrocity.buildChild(
                                    "rightSide",
                                    (side) -> {
                                        side.getTransform().setPosition(1.5f, 0.f, 1.5f);
                                        side.addComponent(
                                                new Renderable(Mesh.CUBE, new UnlitMaterial()));
                                        side.addComponent(new Spinner(180.f, 100.f, 1.f));

                                        // assume this is a hand
                                        side.buildChild(
                                                "rightHand",
                                                (tip) -> {
                                                    tip.getTransform().setPosition(0.f, 0.f, 0.5f);
                                                    tip.getTransform().scale(0.5f, 0.5f, 3.f);
                                                    tip.addComponent(
                                                            new Renderable(
                                                                    Mesh.CUBE,
                                                                    new UnlitMaterial()));
                                                    tip.addComponent(
                                                            new Spinner(-360.f, 100.f, .1f));
                                                });
                                    });
                        });

        mainScene.addRootObject(monstrocity);

        // UI Example:
        GameObject uiPlayer =
                new GameObject(
                        "ui",
                        new UITransform(false),
                        (root) -> {
                            root.addComponent(new HumanPlayer());
                            root.getTransform().scale(0.95f, 0.95f, 1f);
                        });

        /*ui.buildChild(
                "square",
                new UITransform(true),
                (square) -> {
                    square.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 1f)));
                    square.buildChild(
                            "square2",
                            new UITransform(true),
                            (square2) -> {
                                square2.addComponent(
                                        new UIRenderable(
                                                new Vector4f(0.6f, 0.6f, 0.6f, 1f),
                                                new SampledTexture("test_cc0_texture.jpg")));
                                square2.getTransform().scale(0.3f, 0.3f, 0.3f);
                                square2.getTransform().translate(0f, -1.8f, 0f);
                                UIButton uiButton =
                                        new UIButton(
                                                (button, __) -> {
                                                    button.getGameObject()
                                                            .getTransform()
                                                            .rotateDeg(0f, 0f, 15f);
                                                },
                                                null,
                                                null,
                                                (button, deltaTime) -> {
                                                    button.getGameObject()
                                                            .getTransform()
                                                            .rotateDeg(0f, 0f, -60f * deltaTime);
                                                });
                                square2.addComponent(uiButton);
                            });

                    square.buildChild(
                            "button1",
                            new UITransform(true),
                            (button) -> {
                                button.addComponent(
                                        new UIRenderable(new SampledTexture("ui/wide_button.png")));
                                button.getTransform().scale(0.8f, 0.8f, 1f);
                                button.getTransform().translate(0f, 0.4f, 0f);

                                Reference<Renderable> uiRef = ui.getComponent(Renderable.class);

                                UIButton newButton =
                                        new UIButton(
                                                (uiButton, __) -> {
                                                    if (uiRef.isValid()) {
                                                        Renderable uiRend = uiRef.get();
                                                        Transform uiTransform =
                                                                uiRend.getGameObject()
                                                                        .getTransform();
                                                        if (uiRend.isEnabled()) {
                                                            uiTransform.scale(0.1f, 0.1f, 1f);
                                                        } else {
                                                            uiTransform.setPosition(0f, 0f, 0f);
                                                            uiTransform.scale(10f, 10f, 1f);
                                                        }
                                                        uiRend.setEnabled(!uiRend.isEnabled());
                                                    }
                                                });

                                button.addComponent(newButton);
                            });
                });
*/
        mainScene.addRootObject(uiPlayer);

        // Run the game
        Engine.getInstance().start("Germany", new GameBindings(), mainScene);
    }
}
