/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.dragonskulle.utils.Env.*;

import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.camera.KeyboardMovement;
import org.dragonskulle.game.camera.ScrollTranslate;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.network.NetworkClient;
import org.dragonskulle.network.Server;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.components.*;
import org.dragonskulle.renderer.materials.IColouredMaterial;
import org.dragonskulle.renderer.materials.UnlitMaterial;
import org.dragonskulle.ui.*;
import org.joml.*;
import org.joml.Math;

public class App {

    private static final int INSTANCE_COUNT = envInt("INSTANCE_COUNT", 50);
    private static final int INSTANCE_COUNT_ROOT = Math.max((int) Math.sqrt(INSTANCE_COUNT), 1);

    private static final Vector4fc[] COLOURS = {
        new Vector4f(1.f, 0.f, 0.f, 1f),
        new Vector4f(0.f, 1.f, 0.f, 1f),
        new Vector4f(0.f, 0.f, 1.f, 1f),
        new Vector4f(1.f, 0.5f, 0.f, 1f),
        new Vector4f(0.f, 1.f, 0.5f, 1f),
        new Vector4f(0.5f, 0.f, 1.f, 1f),
        new Vector4f(1.f, 1.f, 0.f, 1f),
        new Vector4f(0.f, 1.f, 1.f, 1f),
        new Vector4f(1.f, 0.f, 1.f, 1f),
    };

    private static Scene createMainScene() {
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

                                                    camera.addComponent(new MapEffects());
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
                                    .setAlpha(1f);
                            // You spin me right round...
                            go.addComponent(new Spinner(-180.f, 1000.f, 0.1f));
                        });

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

    private static Scene createMainMenu(Scene mainScene) {
        Scene mainMenu = new Scene("mainMenu");

        GameObject camera = new GameObject("mainCamera");
        Transform3D tr = (Transform3D) camera.getTransform();
        // Set where it's at
        tr.setPosition(0f, 0f, 2f);
        tr.rotateDeg(-30f, 0f, 70f);
        tr.translateLocal(0f, -8f, 0f);
        camera.addComponent(new Camera());
        mainMenu.addRootObject(camera);

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
        hexRoot.addComponent(new Spinner(10, 10, 0.1f));

        // Create instances, change up some parameters
        for (int q = -INSTANCE_COUNT_ROOT / 2; q <= INSTANCE_COUNT_ROOT / 2; q++) {
            for (int r = -INSTANCE_COUNT_ROOT / 2; r <= INSTANCE_COUNT_ROOT / 2; r++) {
                int idx = q * r % COLOURS.length;
                if (idx < 0) idx += COLOURS.length;
                hexWobbler
                        .get()
                        .setPhaseShift((Math.abs(q) + Math.abs(r) + Math.abs(-q - r)) * 0.1f);
                hexMaterial.getColour().set(COLOURS[idx]);
                hexRoot.addChild(GameObject.instantiate(hexagon, new TransformHex(q, r)));
            }
        }

        // Create a cube. This syntax is slightly different
        // This here, will allow you to "build" the cube in one go
        GameObject cube =
                new GameObject(
                        "cube",
                        new Transform3D(0f, 0f, 1.5f),
                        (go) -> {
                            go.addComponent(new Renderable(Mesh.CUBE, new UnlitMaterial()));
                            go.getComponent(Renderable.class)
                                    .get()
                                    .getMaterial(IColouredMaterial.class)
                                    .setAlpha(1f);
                            // You spin me right round...
                            go.addComponent(new Spinner(-180.f, 1000.f, 0.1f));
                        });

        mainMenu.addRootObject(cube);
        mainMenu.addRootObject(hexRoot);

        GameObject mainUI =
                new GameObject(
                        "mainUI",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject joinUI =
                new GameObject(
                        "joinUI",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject hostUI =
                new GameObject(
                        "hostUI",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        mainUI.buildChild(
                "bg",
                new TransformUI(false),
                (bg) -> {
                    bg.addComponent(new UIRenderable(new Vector4f(0.1f, 0.1f, 0.1f, 0f)));

                    bg.getTransform(TransformUI.class).setParentAnchor(0f, 0f, 0.5f, 1.f);

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Join Game"),
                                                (uiButton, __) -> {
                                                    mainUI.setEnabled(false);
                                                    joinUI.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "hostButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.15f, 0.5f, 0.15f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Host Game"),
                                                (uiButton, __) -> {
                                                    mainUI.setEnabled(false);
                                                    hostUI.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "settingsButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.25f, 0.5f, 0.25f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Settings"),
                                                (uiButton, __) -> {
                                                    // TODO: Settings Menu
                                                });

                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "quitButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Quit"),
                                                (uiButton, __) -> Engine.getInstance().stop());

                                button.addComponent(newButton);
                            });
                });
        joinUI.buildChild(
                "bg",
                new TransformUI(false),
                (bg) -> {
                    bg.addComponent(new UIRenderable(new Vector4f(0.1f, 0.1f, 0.1f, 0f)));

                    bg.getTransform(TransformUI.class).setParentAnchor(0f, 0f, 0.5f, 1.f);

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Join (Temporary)"),
                                                (uiButton, __) -> {
                                                    NetworkClient.startClientGame(
                                                            mainScene, "127.0.0.1", 7000);
                                                    Engine.getInstance()
                                                            .loadPresentationScene(mainScene);
                                                });

                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "cancelButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Cancel"),
                                                (uiButton, __) -> {
                                                    joinUI.setEnabled(false);
                                                    mainUI.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });
                });

        hostUI.buildChild(
                "bg",
                new TransformUI(false),
                (bg) -> {
                    bg.addComponent(new UIRenderable(new Vector4f(0.1f, 0.1f, 0.1f, 0f)));

                    bg.getTransform(TransformUI.class).setParentAnchor(0f, 0f, 0.5f, 1.f);

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Host (Temporary)"),
                                                (uiButton, __) -> {
                                                    Server.startServerGame(mainScene);
                                                    Engine.getInstance()
                                                            .loadPresentationScene(mainScene);
                                                });

                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "cancelButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Cancel"),
                                                (uiButton, __) -> {
                                                    hostUI.setEnabled(false);
                                                    mainUI.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });
                });

        joinUI.setEnabled(false);
        hostUI.setEnabled(false);

        mainMenu.addRootObject(GameObject.instantiate(hexRoot));
        mainMenu.addRootObject(GameObject.instantiate(cube));

        mainMenu.addRootObject(hostUI);
        mainMenu.addRootObject(joinUI);
        mainMenu.addRootObject(mainUI);

        return mainMenu;
    }

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) {
        // Create a scene
        Scene mainScene = createMainScene();

        // Create the main menu
        Scene mainMenu = createMainMenu(mainScene);

        // Load the mainScene as an inactive scene
        Engine.getInstance().loadScene(mainScene, false);

        // Load the mainMenu as the presentation scene
        Engine.getInstance().loadPresentationScene(mainMenu);

        // Run the game
        Engine.getInstance().start("Hex Wars", new GameBindings());
    }
}
