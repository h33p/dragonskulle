/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.renderer.RenderedApp;

public class App extends RenderedApp {
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) {
        App app = new App();
        app.run(WIDTH, HEIGHT, "CS:J");
    }
}
