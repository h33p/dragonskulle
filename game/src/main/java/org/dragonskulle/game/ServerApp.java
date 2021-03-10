/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.util.concurrent.atomic.AtomicInteger;
import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.network.*;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.UnlitMaterial;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** @author Oscar L */
public class ServerApp {

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) throws Exception {
        final AtomicInteger mNetworkObjectCounter = new AtomicInteger(0);
        final AtomicInteger mNetworkComponentCounter = new AtomicInteger(0);
        StartServer serverInstance = new StartServer(true, true);

        // Create a scene
        Scene mainScene = new Scene("mainScene");

        GameObject camera = new GameObject("mainCamera");
        camera.addComponent(new Camera());
        Transform cameraTransform =
                new Transform(
                        new Matrix4f()
                                .lookAt(
                                        new Vector3f(2.0f, 2.0f, 2.0f),
                                        new Vector3f(0.0f, 0.0f, -0.05f),
                                        new Vector3f(0.0f, 0.0f, 1.0f)));
        mainScene.addRootObject(GameObject.instantiate(camera, cameraTransform));

        NetworkObject networkCube = new NetworkObject(allocateId(mNetworkObjectCounter), true);

        networkCube.addNetworkableComponent(
                new NetworkedTransform(
                        networkCube.getId(), allocateId(mNetworkComponentCounter), true));
        networkCube.addComponent(new Renderable(Mesh.CUBE, new UnlitMaterial()));
        //        networkCube.addComponent(new NetworkedSpinner());

        spawnNetworkable(serverInstance.server, mainScene, networkCube);

        Engine.getInstance().start("Germany", new GameBindings(), mainScene);
    }

    private static void spawnNetworkable(Server server, Scene mainScene, NetworkObject object) {
        server.spawnObject(object);
        mainScene.addRootObject(GameObject.instantiate(object));
    }

    /**
     * Allocate id int.
     *
     * @return the int
     */
    private static int allocateId(AtomicInteger counter) {
        return counter.getAndIncrement();
    }
}
