/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.dragonskulle.utils.Env.*;

import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.camera.KeyboardMovement;
import org.dragonskulle.game.camera.ScrollTranslate;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.components.*;
import org.dragonskulle.renderer.materials.IColouredMaterial;
import org.dragonskulle.renderer.materials.UnlitMaterial;
import org.joml.Math;

public class App {

    private static final int INSTANCE_COUNT = envInt("INSTANCE_COUNT", 50);
    private static final int INSTANCE_COUNT_ROOT = Math.max((int) Math.sqrt(INSTANCE_COUNT), 1);

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) {
        // Create a scene
        Scene mainScene = setupGameScene();

        // Load the main scene as the presentation scene
        Engine.getInstance().loadPresentationScene(mainScene);

        // Run the game
        Engine.getInstance().start("Germany", new GameBindings());
    }

    private static Scene setupGameScene() {
        // Create a scene
        Scene mainScene = new Scene("game");

        GameObject cameraRig =
                new GameObject(
                        "mainCamera",
                        (rig) -> {
                            KeyboardMovement keyboardMovement = new KeyboardMovement();
                            rig.addComponent(keyboardMovement);

                            rig.getTransform(Transform3D.class).setPosition(0, -4, 1.5f);

                            rig.buildChild(
                                    "rotationRig",
                                    (pitchRig) -> {
                                        pitchRig.getTransform(Transform3D.class)
                                                .rotateDeg(-45f, 0f, 0f);
                                        pitchRig.buildChild(
                                                "camera",
                                                (camera) -> {
                                                    ScrollTranslate scroll =
                                                            new ScrollTranslate(keyboardMovement);
                                                    scroll.getStartPos().set(0f, -5f, 0f);
                                                    scroll.getEndPos().set(0f, -100f, 0f);
                                                    camera.addComponent(scroll);

                                                    // Make sure it's an actual camera
                                                    Camera cam = new Camera();
                                                    cam.farPlane = 200;
                                                    camera.addComponent(cam);
                                                });
                                    });
                        });

        mainScene.addRootObject(GameObject.instantiate(cameraRig));

        GameObject hexagonMap =
                new GameObject(
                        "hexagon map",
                        new Transform3D(),
                        (map) -> {
                            map.addComponent(new HexagonMap(51));
                        });

        mainScene.addRootObject(hexagonMap);

        // Create a cube. This syntax is slightly different
        // This here, will allow you to "build" the cube in one go
        GameObject cube =
                new GameObject(
                        "cube",
                        new Transform3D(0f, -4f, 1.5f),
                        (go) -> {
                            go.addComponent(new Renderable(Mesh.CUBE, new UnlitMaterial()));
                            go.getComponent(Renderable.class)
                                    .get()
                                    .getMaterial(IColouredMaterial.class)
                                    .setAlpha(0.7f);
                            // You spin me right round...
                            go.addComponent(new Spinner(-360.f, 1000.f, 0.1f));
                        });

        // Aaand, spawn it!
        mainScene.addRootObject(cube);

        cube =
                new GameObject(
                        "cube",
                        new Transform3D(0f, -2f, 1.5f),
                        (go) -> {
                            go.addComponent(new Renderable(Mesh.CUBE, new UnlitMaterial()));
                            go.getComponent(Renderable.class)
                                    .get()
                                    .getMaterial(IColouredMaterial.class)
                                    .setAlpha(0.7f);
                            // You spin me right round...
                            go.addComponent(new Spinner(-360.f, 1000.f, 0.1f));
                        });

        // Aaand, spawn it!
        mainScene.addRootObject(cube);

        return mainScene;
    }
}
