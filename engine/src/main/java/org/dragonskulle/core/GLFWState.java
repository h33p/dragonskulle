/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import static org.dragonskulle.utils.Env.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.Input;
import org.dragonskulle.renderer.Renderer;
import org.lwjgl.system.NativeResource;

/**
 * Handles all GLFW state, like input, and window, and stores renderer
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class GLFWState implements NativeResource {
    private long mWindow;
    @Getter private Renderer mRenderer;
    private boolean mFramebufferResized = false;

    private static final Logger LOGGER = Logger.getLogger("GLFW");
    public static final boolean DEBUG_MODE = envBool("DEBUG_GLFW", false);

    /**
     * Entrypoint of the app instance
     *
     * @param width initial window width
     * @param height initial window height
     * @param appName name of the app
     * @param bindings input bindings
     * @throws RuntimeException if initialization fails
     */
    public GLFWState(int width, int height, String appName, Bindings bindings)
            throws RuntimeException {
        DEBUG.set(DEBUG_MODE);

        initWindow(width, height, appName);
        mRenderer = new Renderer(appName, mWindow);

        // Start detecting user input from the specified window, based on the bindings.
        Input.initialise(mWindow, bindings);
    }

    /**
     * Process GLFW events and check if the app should close.
     *
     * @return {@code true} if the app should stay running, {@code false} if the app should close.
     */
    public boolean processEvents() {
        Input.beforePoll();
        glfwPollEvents();

        if (mFramebufferResized) {
            mFramebufferResized = false;
            mRenderer.onResize();
        }

        return !glfwWindowShouldClose(mWindow);
    }

    @Override
    public void free() {
        mRenderer.free();
    }

    /** Creates a GLFW window */
    private void initWindow(int width, int height, String appName) {
        LOGGER.info("Initialize GLFW window");

        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        mWindow = glfwCreateWindow(width, height, appName, NULL, NULL);

        if (mWindow == NULL) {
            throw new RuntimeException("Cannot create window");
        }

        glfwSetFramebufferSizeCallback(mWindow, this::onFramebufferResize);
    }

    private void onFramebufferResize(long window, int width, int height) {
        mFramebufferResized = true;
    }
}
