/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.dragonskulle.utils.Env.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.logging.Logger;
import org.lwjgl.system.MemoryStack;

public class RenderedApp {

    public static final Logger LOGGER = Logger.getLogger("render");

    public static final boolean DEBUG_MODE = envBool("DEBUG_RENDERER", false);

    private long mWindow;
    private Renderer mRenderer;
    private boolean mFramebufferResized = false;

    /// Main functions

    /**
     * Entrypoint of the app instance
     *
     * @param width initial window width
     * @param height initial window height
     * @param appName name of the app
     * @throws Exception if initialization fails
     */
    public void run(int width, int height, String appName) throws Exception {
        DEBUG.set(DEBUG_MODE);
        initWindow(width, height, appName);
        mRenderer = new Renderer(appName, mWindow);
        mainLoop();
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

    private void mainLoop() {
        LOGGER.info("Enter main loop");
        int frameCounter = 0;
        int seconds = 0;
        int startSecondFrame = 0;
        double lastElapsed = (double) System.currentTimeMillis() * 0.001;
        long startTime = System.currentTimeMillis();
        while (!glfwWindowShouldClose(mWindow)) {
            try (MemoryStack stack = stackPush()) {
                long timerTime = System.currentTimeMillis();
                long elapsedMillis = timerTime - startTime;
                float curtime = (float) elapsedMillis * 0.001f;
                glfwPollEvents();

                if (mFramebufferResized) {
                    mFramebufferResized = false;
                    mRenderer.onResize();
                }

                mRenderer.render(null, null, curtime);
            }

            // Debug FPS counter
            frameCounter++;
            long curtime = System.currentTimeMillis();
            long elapsedMillis = curtime - startTime;
            double elapsed = elapsedMillis * 0.001;
            int curSeconds = (int) (elapsedMillis / 1000);
            if (curSeconds > seconds) {
                LOGGER.info(
                        String.format(
                                "FPS: %.2f",
                                (double) (frameCounter - startSecondFrame)
                                        / (elapsed - lastElapsed)));
                seconds = curSeconds;
                startSecondFrame = frameCounter;
                lastElapsed = elapsed;
            }
        }
    }
}
