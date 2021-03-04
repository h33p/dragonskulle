/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.dragonskulle.utils.Env.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.dragonskulle.components.Camera;
import org.dragonskulle.components.Renderable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class RenderedApp {

    private static final Logger LOGGER = Logger.getLogger("render");

    public static final boolean DEBUG_MODE = envBool("DEBUG_RENDERER", false);
    private static final int INSTANCE_COUNT = envInt("INSTANCE_COUNT", 2);

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

        Renderable[] renderables = new Renderable[INSTANCE_COUNT];

        for (int i = 0; i < INSTANCE_COUNT; i++) renderables[i] = new Renderable();

        renderables[0].setMesh(Mesh.CUBE);

        final Vector3f[] colors = {
            new Vector3f(1.f, 0.f, 0.f),
            new Vector3f(0.f, 1.f, 0.f),
            new Vector3f(0.f, 0.f, 1.f),
            new Vector3f(1.f, 0.5f, 0.f),
            new Vector3f(0.f, 1.f, 0.5f),
            new Vector3f(0.5f, 0.f, 1.f),
            new Vector3f(1.f, 1.f, 0.f),
            new Vector3f(0.f, 1.f, 1.f),
            new Vector3f(1.f, 0.f, 1.f),
        };

        for (int i = 1; i < INSTANCE_COUNT; i++)
            renderables[i].getMaterial(UnlitMaterial.class).color = colors[i % colors.length];

        List<Renderable> renderableList = Arrays.asList(renderables);

        Camera cam = new Camera();

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

                for (int i = 0; i < renderables.length; i++) {
                    Matrix4f matrix = renderables[i].matrix;

                    float rotFactor = curtime * (float) (i * 2 - 1);

                    matrix.rotationXYZ(
                            rotFactor * 0.9f * 0,
                            (float) java.lang.Math.sin(rotFactor) * 0.1f * (float) (i * 2 - 1),
                            rotFactor);

                    matrix.setTranslation(
                            0.f,
                            0.f,
                            (float) i * 1f * (float) java.lang.Math.sin(-curtime * 0.05f));
                }

                mRenderer.render(cam, renderableList);
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

        for (Renderable r : renderables) r.free();
    }
}
