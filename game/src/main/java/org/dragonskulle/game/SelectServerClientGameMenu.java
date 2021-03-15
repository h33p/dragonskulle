/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.network.*;
import org.dragonskulle.renderer.*;
import org.dragonskulle.ui.*;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class SelectServerClientGameMenu {

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) {
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

        // UI Example:
        GameObject menu_ui =
                new GameObject(
                        "ui",
                        new UITransform(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.2f)));
                            root.getTransform().scale(0.95f, 0.95f, 1f);
                        });

        menu_ui.buildChild(
                "root_menu_outer_square",
                new UITransform(true),
                (square) -> {
                    square.addComponent(
                            new UIRenderable(
                                    new Vector4f(0.6f, 0.6f, 0.6f, 0.9f),
                                    new SampledTexture("test_cc0_texture.jpg")));
                    // square2.getTransform(UITransform.class).translate(0f, -0.3f);
                    square.getTransform(UITransform.class).setParentAnchor(0.3f, 0.05f, 0.7f, 0.5f);
                    square.getTransform(UITransform.class).setTargetAspectRatio(1f);
                    square.getTransform(UITransform.class).setMaintainAspect(true);

                    square.buildChild(
                            "server_connect_button",
                            new UITransform(true),
                            (self) -> {
                                self.addComponent(
                                        new UIRenderable(new SampledTexture("ui/wide_button.png")));
                                self.getTransform(UITransform.class)
                                        .setParentAnchor(0.15f, 1f, 0.8f, 1f);
                                self.getTransform(UITransform.class).setMargin(0f, 0f, 0f, 0.2f);
                                self.getTransform(UITransform.class).setMaintainAspect(true);
                                self.getTransform(UITransform.class).setTargetAspectRatio(2f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(1f, 0.5f, 0.05f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Start a Server"),
                                                (uiButton, __) -> {
                                                    mainScene.destroyRootObject(menu_ui);
                                                    Server.startServerGame(mainScene);
                                                });

                                self.addComponent(newButton);
                            });

                    square.buildChild(
                            "client_connect_button",
                            new UITransform(true),
                            (self) -> {
                                self.addComponent(
                                        new UIRenderable(new SampledTexture("ui/wide_button.png")));
                                self.getTransform(UITransform.class)
                                        .setParentAnchor(0.15f, 1.25f, 0.8f, 1.25f);
                                self.getTransform(UITransform.class).setMargin(0f, 0f, 0f, 0.2f);
                                self.getTransform(UITransform.class).setMaintainAspect(true);
                                self.getTransform(UITransform.class).setTargetAspectRatio(2f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(1f, 0.5f, 0.05f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Join a Client Game"),
                                                (uiButton, __) -> {
                                                    mainScene.destroyRootObject(menu_ui);
                                                    NetworkClient.startClientGame(
                                                            mainScene, "127.0.0.1", 7000);
                                                });

                                self.addComponent(newButton);
                            });
                });

        mainScene.addRootObject(menu_ui);

        // Load the main scene as the presentation scene
        Engine.getInstance().loadPresentationScene(mainScene);

        // Run the game
        Engine.getInstance().start("Germany", new GameBindings());
    }
}
