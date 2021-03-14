/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.util.Scanner;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.dragonskulle.components.Camera;
import org.dragonskulle.components.Renderable;
import org.dragonskulle.components.Transform;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.network.ClientEars;
import org.dragonskulle.network.ClientListener;
import org.dragonskulle.network.NetworkClient;
import org.dragonskulle.network.components.ClientNetworkManager;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.UnlitMaterial;

/** @author Oscar L */
public class ClientApp {
    /**
     * Entrypoint of the program. Creates and runs one app instance
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        LogManager.getLogManager().reset();
        System.out.println("A server should be setup before running. Continue?");
        new Scanner(System.in).nextLine();
        ClientListener clientListener = new ClientEars();

        // Create a scene
        System.out.println("Creating main scene");
        Scene mainScene = new Scene("mainScene");
        System.out.println("Creating client instance");
        NetworkClient clientInstance =
                new NetworkClient("127.0.0.1", 7000, clientListener, false, mainScene);
        System.out.println("Created client instance");

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

        // attaching server fixed update to game
        GameObject networkManagerGO =
                new GameObject(
                        "client_network_manager",
                        (go) ->
                                go.addComponent(
                                        new ClientNetworkManager(
                                                clientInstance::processRequests,
                                                clientInstance::sendBytes)));
        mainScene.addRootObject(networkManagerGO);

        issue35Workaround(mainScene);
        Engine.getInstance().loadPresentationScene(mainScene);
        Engine.getInstance().start("Client", new GameBindings());
    }

    public static void setLoggingLevel(Level targetLevel) {
        Logger root = Logger.getLogger("");
        root.setLevel(targetLevel);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(targetLevel);
        }
        System.out.println("level set: " + targetLevel.getName());
    }

    public static void issue35Workaround(Scene mainScene) {
        // <<Issue #35 Workaround
        // Create a cube
        GameObject cube = new GameObject("cube");
        cube.addComponent(new Renderable(Mesh.CUBE, new UnlitMaterial()));
        cube.getTransform().translate(1000, 1000, 1000);

        // Aaand, spawn it!
        mainScene.addRootObject(cube);
        // Issue #35 Workaround>>
    }
}
