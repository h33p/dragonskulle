/* (C) 2021 DragonSkulle */
package org.dragonskulle;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Hello world! */
public class App {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private long window;

    /** Entrypoint of the app instance */
    public void run() {
        initWindow();
        initVulkan();
        mainLoop();
        cleanup();
    }

    /** Creates a GLFW window */
    private void initWindow() {
        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "CS:J", NULL, NULL);

        if (window == NULL) {
            throw new RuntimeException("Cannot create window");
        }
    }

    private void initVulkan() {}

    private void mainLoop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    /** Entrypoint of the program. Creates and runs one app instance */
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }
}
