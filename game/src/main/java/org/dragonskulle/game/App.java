/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Scene;
import org.dragonskulle.renderer.RenderedApp;

public class App extends RenderedApp {
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) {
        Scene scene = new Scene("Main");

        GameObject root = new GameObject("root");

        GameObject obj1 = new GameObject("Object 1");
        GameObject obj2 = new GameObject("Object 2");

        obj1.addComponent(new ExampleComponent(1));
        obj2.addComponent(new ExampleComponent(2));

        obj1.addChild(obj2);
        root.addChild(obj1);

        scene.addRootObject(root);

        Engine.getInstance().start(scene);
    }
}
