/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.network.*;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.renderer.components.*;

/** @author Oscar L */
public class ServerApp {

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) throws Exception {
        LogManager.getLogManager().reset();
        final AtomicInteger mNetworkObjectCounter = new AtomicInteger(0);
        StartServer serverInstance = new StartServer(mNetworkObjectCounter, true, true);

        // Create a scene
        Scene mainScene = new Scene("mainScene");

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

        // attaching server fixed update to game

        GameObject networkManagerGO =
                new GameObject(
                        "server_network_manager",
                        (go) ->
                                go.addComponent(
                                        new NetworkManager(
                                                serverInstance.mServer::fixedBroadcastUpdate)));
        mainScene.addRootObject(networkManagerGO);

        serverInstance.linkToScene(mainScene);
        Engine.getInstance().loadPresentationScene(mainScene);
        Engine.getInstance().start("Server", new GameBindings());
    }
}
