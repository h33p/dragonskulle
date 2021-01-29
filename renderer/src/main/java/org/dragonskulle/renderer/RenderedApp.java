/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class RenderedApp {

    private long window;

    /** Entrypoint of the app instance */
    public void run(int width, int height, String appName) {
        initWindow(width, height, appName);
        initVulkan();
        mainLoop();
        cleanup();
    }

    /** Creates a GLFW window */
    private void initWindow(int width, int height, String appName) {
        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(width, height, appName, NULL, NULL);

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
}
