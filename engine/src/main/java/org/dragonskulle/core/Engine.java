/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.ArrayList;
import java.util.HashSet;
import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.components.Camera;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.Renderable;
import org.dragonskulle.input.Bindings;

/**
 * Engine core
 *
 * @author Harry Stoltz
 *     <p>The core of the engine, contains the main loop which executes all game logic. Gives all
 *     components access to engine components such as the AudioManager and InputManager.
 */
public class Engine {
    private static final Engine ENGINE_INSTANCE = new Engine();

    // TODO: Choose a number of updates per second that we want to have
    private static final int UPDATES_PER_SECOND = 30; // Target number of fixed updates per second
    private static final float UPDATE_TIME = 1 / (float) UPDATES_PER_SECOND;

    private static final int WINDOW_WIDTH = 1600;
    private static final int WINDOW_HEIGHT = 900;

    private boolean mIsRunning = false;

    protected final HashSet<GameObject> mDestroyedObjects = new HashSet<>();
    protected final HashSet<Component> mDestroyedComponents = new HashSet<>();

    private final HashSet<Scene> mScenesToEnable = new HashSet<>();
    private final HashSet<Scene> mScenesToDisable = new HashSet<>();
    private Scene mNewPresentationScene = null;

    private final HashSet<Scene> mInactiveScenes = new HashSet<>();
    private final HashSet<Scene> mActiveScenes = new HashSet<>();
    private Scene mPresentationScene = null;

    private GLFWState mGLFWState = null;
    private ArrayList<Renderable> mTmpRenderables = new ArrayList<>();

    private Engine() {}

    /**
     * Loads a new scene and start the engine
     *
     * @param gameName Name of the game
     * @param bindings User input bindings
     * @param scene Initial scene for the engine to run
     */
    public void start(String gameName, Bindings bindings, Scene scene) {
        loadScene(scene);

        // TODO: Any initialization of engine components like renderer, audio, input, etc done here

        mGLFWState = new GLFWState(WINDOW_WIDTH, WINDOW_HEIGHT, gameName, bindings);

        mIsRunning = true;
        mainLoop();
    }

    /**
     * Loads a new scene that will be run from the next frame onwards.
     *
     * @param scene Scene to be loaded
     */
    public void loadScene(Scene scene) {
        mScenesToEnable.add(scene);
    }

    /**
     * Load a new scene that will NOT be run until it is manually started.
     *
     * @param scene Scene to be loaded
     */
    public void loadInactiveScene(Scene scene) {
        mScenesToDisable.add(scene);
    }

    /**
     * Set a scene as the presentation scene, which is the only scene rendered to the end user.
     *
     * @param scene Scene to be rendered to the user
     */
    public void setPresentationScene(Scene scene) {
        mScenesToEnable.add(scene);
        mNewPresentationScene = scene;
    }

    /**
     * Enable all scenes with a matching name. Scenes aren't enabled until the start of the next
     * frame
     *
     * @param name Name of the scene to enable.
     */
    public void enableScene(String name) {
        for (Scene s : mInactiveScenes) {
            if (s.getName().equals(name)) {
                mScenesToEnable.add(s);
            }
        }
    }

    /**
     * Disable all scenes with a matching name. Scenes aren't disable until the start of the next
     * frame
     *
     * @param name Name of the scene to disable.
     */
    public void disableScene(String name) {
        for (Scene s : mActiveScenes) {
            if (s.getName().equals(name)) {
                mScenesToDisable.add(s);
            }
        }
    }

    /** Stops the engine when the current frame has finished */
    public void stop() {
        mIsRunning = false;
    }

    /** Main loop of the engine */
    private void mainLoop() {

        float mPrevTime = Time.getTimeInSeconds();

        // Basic frame counter
        int frames = 0;
        float secondTimer = 0;
        float cumulativeTime = 0;

        while (mIsRunning) {
            // Calculate time for last frame
            float mCurTime = Time.getTimeInSeconds();
            float deltaTime = mCurTime - mPrevTime;
            mPrevTime = mCurTime;

            cumulativeTime += deltaTime;
            secondTimer += deltaTime;

            // Switch any scenes
            switchScenes();

            // Update all component lists in scenes
            updateScenesComponentsList();

            // Wake up all components that aren't awake
            wakeComponents();

            // Start all enabled components
            startEnabledComponents();

            // TODO: Process inputs here before any updates are performed
            mIsRunning = mGLFWState.processEvents();

            // Call FrameUpdate on the presentation scene
            frameUpdate(deltaTime);

            // Perform all updates that we can fit in the time since last frame
            // Means that multiple fixed updates can happen before the next frame
            // if rendering to screen is taking a very long time
            while (cumulativeTime > UPDATE_TIME) {
                cumulativeTime -= UPDATE_TIME;

                fixedUpdate();
            }

            // Call LateFrameUpdate on the presentation scene
            lateFrameUpdate(deltaTime);

            renderFrame();

            // Destroy all objects and components that were destroyed this frame
            destroyObjectsAndComponents();

            frames++;
            if (secondTimer > 1.0) {
                // One second has elapsed so frames contains the FPS

                // Have no use for this currently besides printing it to console
                System.out.println("FPS:" + frames);
                secondTimer = 0;
                frames = 0;
            }
        }

        cleanup();
    }

    /** Iterate through a list of components that aren't awake and wake them */
    private void wakeComponents() {
        for (Scene s : mActiveScenes) {
            for (Component component : s.getNotAwakeComponents()) {
                if (component instanceof IOnAwake) {
                    ((IOnAwake) component).onAwake();
                }
                component.setAwake(true);
            }
        }
    }

    /**
     * Iterate through a list of components that are enabled but haven't been started and start them
     */
    private void startEnabledComponents() {
        for (Scene s : mActiveScenes) {
            for (Component component : s.getEnabledButNotStartedComponents()) {
                if (component instanceof IOnStart) {
                    ((IOnStart) component).onStart();
                }
                component.setStarted(true);
            }
        }
    }

    /**
     * Do all frameUpdates on components that implement it. Only components in the presentation
     * scene have frame update called
     *
     * @param deltaTime Time change since last frame
     */
    private void frameUpdate(float deltaTime) {
        for (Component component : mPresentationScene.getEnabledComponents()) {
            if (component instanceof IFrameUpdate) {
                ((IFrameUpdate) component).frameUpdate(deltaTime);
            }
        }
    }

    /** Do all Fixed Updates on components that implement it */
    private void fixedUpdate() {
        for (Scene s : mActiveScenes) {
            for (Component component : s.getEnabledComponents()) {
                if (component instanceof IFixedUpdate) {
                    ((IFixedUpdate) component).fixedUpdate(UPDATE_TIME);
                }
            }
        }
    }

    /**
     * Do all Late Frame Updates on components that implement it
     *
     * @param deltaTime Time change since last frame
     */
    private void lateFrameUpdate(float deltaTime) {
        for (Component component : mPresentationScene.getEnabledComponents()) {
            if (component instanceof ILateFrameUpdate) {
                ((ILateFrameUpdate) component).lateFrameUpdate(deltaTime);
            }
        }
    }

    /** Destroy all GameObjects and Components that need to be destroyed */
    private void destroyObjectsAndComponents() {
        // Destroy all game objects that need to be destroyed
        for (GameObject object : mDestroyedObjects) {
            object.engineDestroy();
        }
        mDestroyedObjects.clear();

        // Destroy all components that need to be destroyed
        for (Component component : mDestroyedComponents) {
            component.engineDestroy();
        }
        mDestroyedComponents.clear();
    }

    private void renderFrame() {
        mTmpRenderables.clear();
        for (Component component : mPresentationScene.getEnabledComponents()) {
            if (component instanceof Renderable) {
                mTmpRenderables.add((Renderable) component);
            }
        }

        Camera mainCamera = Camera.getMainCamera();

        if (mainCamera != null) mGLFWState.getRenderer().render(mainCamera, mTmpRenderables);
    }

    /**
     * Switch all scenes. Disabling any scenes that need to be disabled, enabling all those that
     * should be enabled and switching the presentation scene if necessary.
     */
    private void switchScenes() {
        // Load the new presentation scene
        if (mNewPresentationScene != null) {
            if (mPresentationScene != null) {
                mInactiveScenes.add(mPresentationScene);
                mActiveScenes.remove(mPresentationScene);
            }
            mActiveScenes.add(mNewPresentationScene);
            mPresentationScene = mNewPresentationScene;
            mNewPresentationScene = null;
        }

        // Disable any scenes we need to
        for (Scene s : mScenesToDisable) {
            mActiveScenes.remove(s);
            mInactiveScenes.add(s);
        }

        for (Scene s : mScenesToEnable) {
            mInactiveScenes.remove(s);
            mActiveScenes.add(s);
        }
    }

    /** Update the component lists in every active scene */
    private void updateScenesComponentsList() {
        for (Scene s : mActiveScenes) {
            s.updateComponentsList();
        }
    }

    /** Cleans up all resources used by the engine on shutdown */
    private void cleanup() {
        // TODO: Release all resources that are still used at the time of shutdown here

        AudioManager.getInstance().cleanup();
        mGLFWState.free();
    }

    /**
     * Add a component to the set of all components to be destroyed
     *
     * @param component Component to be destroyed at the end of the current frame
     */
    public void addDestroyedComponent(Component component) {}

    /**
     * Getter for mActiveScene
     *
     * @return mActiveScene
     */
    public ArrayList<Scene> getActiveScenes() {
        return new ArrayList<Scene>(mActiveScenes);
    }

    /**
     * Get the single instance of the engine
     *
     * @return The Engine instance
     */
    public static Engine getInstance() {
        return ENGINE_INSTANCE;
    }
}
