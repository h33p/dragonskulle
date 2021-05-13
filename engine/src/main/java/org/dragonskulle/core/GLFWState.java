/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import static org.dragonskulle.utils.Env.envBool;
import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_DONT_CARE;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.Input;
import org.dragonskulle.renderer.Renderer;
import org.dragonskulle.renderer.RendererException;
import org.dragonskulle.renderer.RendererSettings;
import org.dragonskulle.settings.Settings;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

/**
 * Handles all GLFW state, like input, and window, and stores renderer.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
public class GLFWState implements NativeResource {
    @Getter private long mWindow;
    /** Window size in screen coordinates. */
    @Getter private final Vector2i mWindowSize = new Vector2i();

    private final Settings mSettings;

    @Getter private Renderer mRenderer;
    private boolean mFramebufferResized = false;

    private boolean mFullscreen = false;
    private int mOldX = 0;
    private int mOldY = 0;
    private int mOldWidth = 1600;
    private int mOldHeight = 900;

    private static final String FULLSCREEN = "fullscreen";

    public static final boolean DEBUG_MODE = envBool("DEBUG_GLFW", false);

    public static final boolean LOAD_RENDERDOC = envBool("LOAD_RENDERDOC", false);

    /**
     * Entrypoint of the app instance.
     *
     * @param width initial window width
     * @param height initial window height
     * @param appName name of the app
     * @param bindings input bindings
     * @param settings renderer settings to load from
     * @throws RuntimeException if initialization fails
     */
    public GLFWState(int width, int height, String appName, Bindings bindings, Settings settings)
            throws RendererException {
        DEBUG.set(DEBUG_MODE);

        if (LOAD_RENDERDOC) {
            System.loadLibrary("renderdoc");
        }

        mSettings = settings;

        initWindow(width, height, appName);
        mRenderer = new Renderer(appName, mWindow, new RendererSettings(settings));

        // Start detecting user input from the specified window, based on the bindings.
        Input.initialise(mWindow, bindings);
    }

    /**
     * Set fullscreen mode for the current window.
     *
     * @param fullscreen whether to make the window fullscreen or not. If set to true, the window
     *     will be made full screen, if set to false, the window will no longer be fullscreen. An
     *     attempt will be made to restore the window to the same position and size it was before
     *     entering full screen mode.
     */
    public void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            long primaryMonitor = glfwGetPrimaryMonitor();
            GLFWVidMode mode = glfwGetVideoMode(primaryMonitor);

            int[] xpos = {0};
            int[] ypos = {0};
            glfwGetWindowPos(mWindow, xpos, ypos);
            mOldX = xpos[0];
            mOldY = ypos[0];

            int[] width = {0};
            int[] height = {0};
            glfwGetWindowSize(mWindow, width, height);
            mOldWidth = width[0];
            mOldHeight = height[0];

            glfwSetWindowMonitor(
                    mWindow, primaryMonitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
            mFullscreen = true;
        } else if (mFullscreen) {
            glfwSetWindowMonitor(mWindow, 0, mOldX, mOldY, mOldWidth, mOldHeight, GLFW_DONT_CARE);
            mFullscreen = false;
        }

        mSettings.saveValue(FULLSCREEN, mFullscreen, true);
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
            try {
                mRenderer.onResize();
            } catch (RendererException e) {
                log.severe("Renderer exception in onResize! " + e.toString());
                return false;
            }
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

    /**
     * Creates a GLFW window. @param width the width
     *
     * @param width the width
     * @param height the height
     * @param appName the app name
     */
    private void initWindow(int width, int height, String appName) {
        log.fine("Initialize GLFW window");

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

        setFullscreen(mSettings.retrieveBoolean(FULLSCREEN, false));
    }

    /**
     * Event called when framebuffer is resized.
     *
     * @param window window that got resized.
     * @param width new width.
     * @param height new height.
     */
    private void onFramebufferResize(long window, int width, int height) {
        mFramebufferResized = true;
    }
}
