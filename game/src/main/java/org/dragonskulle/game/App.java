/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.dragonskulle.utils.Env.*;

import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.UnlitMaterial;
import org.dragonskulle.ui.*;
import org.joml.Math;
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

    private static final int INSTANCE_COUNT_ROOT = Math.max((int) Math.sqrt(INSTANCE_COUNT), 1);

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) {
        // Create a scene
        Scene mainScene = new Scene("mainScene");

        // The game needs a camera!
        GameObject camera = new GameObject("mainCamera");

        Transform3D tr = (Transform3D) camera.getTransform();
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

        // Add wobble components
        hexagon.addComponent(new Wobbler());
        Reference<Wobbler> hexWobbler = hexagon.getComponent(Wobbler.class);

        GameObject hexRoot = new GameObject("hexRoot");
        hexRoot.addComponent(new Spinner(30, 10, 0.1f));

        // Create instances, change up some parameters
        for (int q = -INSTANCE_COUNT_ROOT / 2; q <= INSTANCE_COUNT_ROOT / 2; q++) {
            for (int r = -INSTANCE_COUNT_ROOT / 2; r <= INSTANCE_COUNT_ROOT / 2; r++) {
                int idx = q * r % COLOURS.length;
                if (idx < 0) idx += COLOURS.length;
                hexWobbler
                        .get()
                        .setPhaseShift((Math.abs(q) + Math.abs(r) + Math.abs(-q - r)) * 0.1f);
                hexMaterial.colour.set(COLOURS[idx]);
                hexRoot.addChild(GameObject.instantiate(hexagon, new TransformHex(q, r)));
            }
        }

        mainScene.addRootObject(hexRoot);

        // Create a cube. This syntax is slightly different
        // This here, will allow you to "build" the cube in one go
        GameObject cube =
                new GameObject(
                        "cube",
                        new Transform3D(0f, 0f, 1.5f),
                        (go) -> {
                            go.addComponent(new Renderable(Mesh.CUBE, new UnlitMaterial()));
                            // You spin me right round...
                            go.addComponent(new Spinner(-360.f, 1000.f, 0.1f));
                        });

        // Aaand, spawn it!
        mainScene.addRootObject(cube);

        // UI Example:
        GameObject ui =
                new GameObject(
                        "ui",
                        new UITransform(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(UITransform.class).setParentAnchor(0.01f);
                        });

        ui.buildChild(
                "square",
                new UITransform(true),
                (square) -> {
                    square.addComponent(new UIRenderable(new Vector4f(0.3f, 0.3f, 0.3f, 0.5f)));

                    square.getTransform(UITransform.class)
                            .setParentAnchor(0.01f, 0.1f, 0.49f, 0.98f);

                    square.buildChild(
                            "square2",
                            new UITransform(true),
                            (square2) -> {
                                square2.addComponent(
                                        new UIRenderable(
                                                new Vector4f(0.6f, 0.6f, 0.6f, 0.9f),
                                                new SampledTexture("test_cc0_texture.jpg")));
                                // square2.getTransform(UITransform.class).translate(0f, -0.3f);
                                square2.getTransform(UITransform.class)
                                        .setParentAnchor(0.3f, 0.05f, 0.7f, 0.5f);
                                square2.getTransform(UITransform.class).setTargetAspectRatio(1f);
                                square2.getTransform(UITransform.class).setMaintainAspect(true);
                                UIButton uiButton =
                                        new UIButton(
                                                null,
                                                (button, __) -> {
                                                    button.getGameObject()
                                                            .getTransform(UITransform.class)
                                                            .rotateDeg(15f);
                                                },
                                                null,
                                                null,
                                                (button, deltaTime) -> {
                                                    button.getGameObject()
                                                            .getTransform(UITransform.class)
                                                            .rotateDeg(-60f * deltaTime);
                                                });
                                square2.addComponent(uiButton);
                            });

                    square.buildChild(
                            "button1",
                            new UITransform(true),
                            (button) -> {
                                button.addComponent(
                                        new UIRenderable(new SampledTexture("ui/wide_button.png")));
                                button.getTransform(UITransform.class)
                                        .setParentAnchor(0.2f, 0.5f, 0.8f, 0.5f);
                                button.getTransform(UITransform.class).setMargin(0f, 0f, 0f, 0.2f);
                                button.getTransform(UITransform.class).setMaintainAspect(true);
                                button.getTransform(UITransform.class).setTargetAspectRatio(2f);
                                // button.getTransform(UITransform.class).translate();

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(1f, 0.5f, 0.05f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Click me!"),
                                                (uiButton, __) -> {
                                                    uiButton.getLabelText()
                                                            .get()
                                                            .setText(
                                                                    uiButton.getLabelText()
                                                                                    .get()
                                                                                    .getText()
                                                                            + "a");
                                                });

                                button.addComponent(newButton);
                            });
                });

        mainScene.addRootObject(ui);

        // Load the main scene as the presentation scene
        Engine.getInstance().loadPresentationScene(mainScene);

        // Run the game
        Engine.getInstance().start("Germany", new GameBindings());
    }
}
