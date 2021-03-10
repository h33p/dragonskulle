/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.util.Scanner;
import java.util.logging.Handler;
import java.util.logging.Level;
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
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.UnlitMaterial;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** @author Oscar L */
public class ClientApp {
    /**
     * Entrypoint of the program. Creates and runs one app instance
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        setLoggingLevel(Level.WARNING);
        System.out.println("A server should be setup before running. Continue?");
        new Scanner(System.in).nextLine();
        ClientListener clientListener = new ClientEars();

        // Create a scene
        System.out.println("Creating main scene");
        Scene mainScene = new Scene("mainScene");
        System.out.println("Creating client instance");
        NetworkClient clientInstance =
                new NetworkClient("127.0.0.1", 7000, clientListener, true, mainScene);
        System.out.println("Created client instance");

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
        issue35Workaround(mainScene);
        Engine.getInstance().start("Germany", new GameBindings(), mainScene);
    }

    public static void setLoggingLevel(Level targetLevel) {
        Logger root = Logger.getLogger("");
        root.setLevel(targetLevel);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(targetLevel);
        }
        System.out.println("level set: " + targetLevel.getName());
    }

    private static void issue35Workaround(Scene mainScene) {
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
