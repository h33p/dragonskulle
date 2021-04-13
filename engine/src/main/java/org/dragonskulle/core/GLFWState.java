/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import static org.dragonskulle.utils.Env.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.Input;
import org.dragonskulle.renderer.Renderer;
import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

/**
 * Handles all GLFW state, like input, and window, and stores renderer
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
public class GLFWState implements NativeResource {
    private long mWindow;
    /** Window size in screen coordinates */
    @Getter private final Vector2i mWindowSize = new Vector2i();

    @Getter private Renderer mRenderer;
    private boolean mFramebufferResized = false;

    public static final boolean DEBUG_MODE = envBool("DEBUG_GLFW", false);

    public static final boolean LOAD_RENDERDOC = envBool("LOAD_RENDERDOC", false);

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

        if (LOAD_RENDERDOC) {
            System.loadLibrary("renderdoc");
        }

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

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);
            glfwGetWindowSize(mWindow, width, height);
            mWindowSize.set(width.get(0), height.get(0));
        }

        return !glfwWindowShouldClose(mWindow);
    }

    @Override
    public void free() {
        mRenderer.free();
    }

    /** Creates a GLFW window */
    private void initWindow(int width, int height, String appName) {
        log.info("Initialize GLFW window");

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
