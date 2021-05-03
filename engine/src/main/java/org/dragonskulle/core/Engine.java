/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import com.rits.cloning.Cloner;
import java.util.ArrayList;
import java.util.HashSet;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.ILateNetworkUpdate;
import org.dragonskulle.components.INetworkUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.input.Bindings;
import org.dragonskulle.network.UPnP;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.ui.UIManager;

/**
 * Engine core.
 *
 * @author Harry Stoltz
 *     <p>The core of the engine, contains the main loop which executes all game logic. Gives all
 *     components access to engine components such as the AudioManager and InputManager.
 */
@Accessors(prefix = "m")
@Log
public class Engine {
    private static final Engine ENGINE_INSTANCE = new Engine();

    private static final int UPDATES_PER_SECOND = 32; // Target number of fixed updates per second
    private static final float UPDATE_TIME = 1 / (float) UPDATES_PER_SECOND;

    private static final int WINDOW_WIDTH = 1600;
    private static final int WINDOW_HEIGHT = 900;

    @Accessors(prefix = "s")
    @Getter
    private static final Cloner sCloner = new Cloner();

    private boolean mIsRunning = false;

    protected final HashSet<GameObject> mDestroyedObjects = new HashSet<>();
    protected final HashSet<Component> mDestroyedComponents = new HashSet<>();

    private final HashSet<Scene> mScenesToActivate = new HashSet<>();
    private final HashSet<Scene> mScenesToDeactivate = new HashSet<>();
    private final HashSet<Scene> mScenesToUnload = new HashSet<>();
    private Scene mNewPresentationScene = null;

    private final HashSet<Scene> mInactiveScenes = new HashSet<>();
    private final HashSet<Scene> mActiveScenes = new HashSet<>();
    @Getter private Scene mPresentationScene = null;

    /** Engine's GLFW window state. */
    @Getter private GLFWState mGLFWState = null;

    private ArrayList<IScheduledEvent> mFrameEvents = new ArrayList<>();
    private ArrayList<IScheduledEvent> mEndOfLoopEvents = new ArrayList<>();
    private ArrayList<IScheduledEvent> mFixedUpdateEvents = new ArrayList<>();
    private ArrayList<IScheduledEvent> mEventsToConsume = new ArrayList<>();

    @Getter private float mCurTime = 0f;

    private final ArrayList<Renderable> mTmpRenderables = new ArrayList<>();
    private final ArrayList<Light> mTmpLights = new ArrayList<>();

    public interface IEngineExitCondition {
        boolean shouldExit();
    }

    public interface IScheduledEvent {
        void invoke();
    }

    private Engine() {}

    /**
     * Loads a new scene and start the engine.
     *
     * @param gameName Name of the game
     * @param bindings User input bindings
     */
    public void start(String gameName, Bindings bindings) {
        // TODO: Any initialization of engine components like renderer, audio, input, etc done here

        UPnP.initialise();

        mGLFWState = new GLFWState(WINDOW_WIDTH, WINDOW_HEIGHT, gameName, bindings);

        mIsRunning = true;
        mainLoop(mGLFWState::processEvents, true);

        cleanup();
    }

    /**
     * Starts only fixed updates of the engine, allows for custom quit condition.
     *
     * <p>Note that this method will not destroy game object references!
     *
     * @param exitCondition customizable exit condition
     */
    public synchronized void startFixedDebug(IEngineExitCondition exitCondition) {
        mIsRunning = true;
        mainLoop(exitCondition, false);
    }

    /**
     * Load a scene, choosing whether or not it should be active from the next frame or not.
     *
     * @param scene Scene to load
     * @param active Whether the scene will be active
     */
    public void loadScene(Scene scene, boolean active) {
        if (active) {
            mScenesToActivate.add(scene);
        } else {
            mScenesToDeactivate.add(scene);
        }
    }

    /**
     * If scene is already loaded, it will be activated from the next frame. If it isn't loaded, it
     * will be loaded and started on the next frame
     *
     * @param scene Scene to be loaded
     */
    public void activateScene(Scene scene) {
        mScenesToActivate.add(scene);
    }

    /**
     * Activate an already loaded scene, with a given name.
     *
     * @param name Name of the scene to activate
     */
    public void activateScene(String name) {
        for (Scene s : mInactiveScenes) {
            if (s.getName().equals(name)) {
                mScenesToActivate.add(s);
            }
        }
    }

    /**
     * If scene is already loaded, it will be deactivated from the next frame. If it isn't loaded,
     * it will be loaded but not started.
     *
     * @param scene Scene to be loaded
     */
    public void deactivateScene(Scene scene) {
        mScenesToDeactivate.add(scene);
    }

    /**
     * Deactivate an already loaded scene, with a given name.
     *
     * @param name Name of the scene to activate
     */
    public void deactivateScene(String name) {
        for (Scene s : mActiveScenes) {
            if (s.getName().equals(name)) {
                mScenesToDeactivate.add(s);
            }
        }
    }

    /**
     * Completely unload a scene from the engine. This will remove the scene regardless of whether
     * it is active, inactive or the presentation scene. No references to the scene will be kept in
     * the engine.
     *
     * @param scene Scene to unload
     */
    public void unloadScene(Scene scene) {
        mScenesToUnload.add(scene);
    }

    /**
     * Set a scene as the presentation scene, which is the only scene rendered to the end user.
     *
     * @param scene Scene to be rendered to the user
     */
    public void loadPresentationScene(Scene scene) {
        mScenesToActivate.add(scene);
        mNewPresentationScene = scene;
    }

    /**
     * Load the first active or inactive scene found as the presentation scene. This might not give
     * the expected results if there are multiple scenes with the same name. It is also not the most
     * efficient, so if possible maintain a reference to scenes and load them directly
     *
     * @param name Name of the scene to set as the presentation scene
     */
    public void loadPresentationScene(String name) {
        for (Scene s : mActiveScenes) {
            if (s.getName().equals(name)) {
                loadPresentationScene(s);
                return;
            }
        }
        for (Scene s : mInactiveScenes) {
            if (s.getName().equals(name)) {
                loadPresentationScene(s);
                return;
            }
        }
    }

    /** Schedule an event for the next frame update. */
    public void scheduleFrameEvent(IScheduledEvent event) {
        mFrameEvents.add(event);
    }

    /** Schedule an event for the next fixed update. */
    public void scheduleFixedUpdateEvent(IScheduledEvent event) {
        mFixedUpdateEvents.add(event);
    }

    /** Schedule an event for the end of main loop iteration. */
    public void scheduleEndOfLoopEvent(IScheduledEvent event) {
        mEndOfLoopEvents.add(event);
    }

    /** Stops the engine when the current frame has finished. */
    public void stop() {
        mIsRunning = false;
    }

    /** Main loop of the engine. */
    private void mainLoop(IEngineExitCondition exitCondition, boolean present) {

        double prevTime = Time.getPreciseTimeInSeconds();

        // Basic frame counter
        double cumulativeTime = 0;

        mCurTime = 0;

        while (mIsRunning) {
            // Calculate time for last frame
            double curTime = Time.getPreciseTimeInSeconds();
            double deltaTime = curTime - prevTime;
            prevTime = curTime;
            double cumulativeDeltaTime = deltaTime;

            cumulativeTime += deltaTime;

            boolean triggerFixedUpdate = cumulativeTime > UPDATE_TIME;

            // Update scenes
            switchScenes();

            // Update all component lists in active scenes
            updateScenesComponentsList();

            // Wake up all components that aren't awake (Called on all active scenes)
            wakeComponents();

            // Start all enabled components (Called on all active scenes)
            startEnabledComponents();

            mIsRunning = exitCondition.shouldExit();

            if (present) {
                Scene.setActiveScene(mPresentationScene);
                UIManager.getInstance().updateHover(mPresentationScene.getEnabledComponents());

                // Call FrameUpdate on the presentation scene
                frameUpdate((float) deltaTime);
                Scene.setActiveScene(null);
            }

            if (triggerFixedUpdate) {
                networkUpdate();

                do {
                    cumulativeTime -= UPDATE_TIME;
                    mCurTime += UPDATE_TIME;
                    cumulativeDeltaTime -= UPDATE_TIME;

                    fixedUpdate();
                    AudioManager.getInstance().update();
                } while (cumulativeTime > UPDATE_TIME);
            }

            mCurTime += cumulativeDeltaTime;

            if (present) {
                Scene.setActiveScene(mPresentationScene);
                AudioManager.getInstance().updateAudioListener();

                // Call LateFrameUpdate on the presentation scene
                lateFrameUpdate((float) deltaTime);

                renderFrame();
                Scene.setActiveScene(null);
            }

            if (triggerFixedUpdate) {
                lateNetworkUpdate();
            }

            ArrayList<IScheduledEvent> toConsume = mEndOfLoopEvents;
            mEndOfLoopEvents = mEventsToConsume;
            consumeEvents(toConsume);

            // Destroy all objects and components that were destroyed this frame
            destroyObjectsAndComponents();
        }
    }

    /** Iterate through a list of components that aren't awake and wake them. */
    private void wakeComponents() {
        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);
            for (Component component : s.getNotAwakeComponents()) {
                if (component instanceof IOnAwake) {
                    ((IOnAwake) component).onAwake();
                }
                component.setAwake(true);
            }
        }
        Scene.setActiveScene(null);
    }

    /**
     * Iterate through a list of components that are enabled but haven't been started and start
     * them.
     */
    private void startEnabledComponents() {
        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);
            for (Component component : s.getEnabledButNotStartedComponents()) {
                if (component instanceof IOnStart) {
                    ((IOnStart) component).onStart();
                }
                component.setStarted(true);
            }
        }
        Scene.setActiveScene(null);
    }

    private void consumeEvents(ArrayList<IScheduledEvent> toConsume) {
        mEventsToConsume = toConsume;
        for (IScheduledEvent event : mEventsToConsume) {
            event.invoke();
        }
        mEventsToConsume.clear();
    }

    /**
     * Do all frameUpdates on components that implement it. Only components in the presentation
     * scene have frame update called
     *
     * @param deltaTime Time change since last frame
     */
    private void frameUpdate(float deltaTime) {
        ArrayList<IScheduledEvent> toConsume = mFrameEvents;
        mFrameEvents = mEventsToConsume;
        consumeEvents(toConsume);

        for (Component component : mPresentationScene.getEnabledComponents()) {
            if (component instanceof IFrameUpdate) {
                ((IFrameUpdate) component).frameUpdate(deltaTime);
            }
        }
    }

    /** Do all Fixed Updates on components that implement it. */
    private void fixedUpdate() {
        ArrayList<IScheduledEvent> toConsume = mFixedUpdateEvents;
        mFixedUpdateEvents = mEventsToConsume;
        consumeEvents(toConsume);

        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);
            for (Component component : s.getEnabledComponents()) {
                if (component instanceof IFixedUpdate) {
                    ((IFixedUpdate) component).fixedUpdate(UPDATE_TIME);
                }
            }
        }
        Scene.setActiveScene(null);
    }

    /** Do all Network Updates on components that implement it. */
    private void networkUpdate() {
        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);
            for (Component component : s.getEnabledComponents()) {
                if (component instanceof INetworkUpdate) {
                    ((INetworkUpdate) component).networkUpdate();
                }
            }
        }
        Scene.setActiveScene(null);
    }

    /** Do all Late Network Updates on components that implement it. */
    private void lateNetworkUpdate() {
        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);
            for (Component component : s.getEnabledComponents()) {
                if (component instanceof ILateNetworkUpdate) {
                    ((ILateNetworkUpdate) component).lateNetworkUpdate();
                }
            }
        }
        Scene.setActiveScene(null);
    }

    /**
     * Do all Late Frame Updates on components that implement it.
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

    /** Destroy all GameObjects and Components that need to be destroyed.s */
    private void destroyObjectsAndComponents() {
        // Destroy all game objects that need to be destroyed
        for (GameObject object : mDestroyedObjects) {
            object.engineDestroy();

            if (object.isRootObject()) {
                for (Scene s : mActiveScenes) {
                    s.removeRootObject(object);
                }
                for (Scene s : mInactiveScenes) {
                    s.removeRootObject(object);
                }
            }
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
        mTmpLights.clear();

        for (Component component : mPresentationScene.getEnabledComponents()) {
            if (component instanceof Renderable) {
                mTmpRenderables.add((Renderable) component);
            } else if (component instanceof Light) {
                mTmpLights.add((Light) component);
            }
        }

        Camera mainCamera = mPresentationScene.getSingleton(Camera.class);

        if (mainCamera != null) {
            mGLFWState.getRenderer().render(mainCamera, mTmpRenderables, mTmpLights);
        }
    }

    /**
     * Switch all scenes. Disabling any scenes that need to be disabled, enabling all those that
     * should be enabled and switching the presentation scene if necessary.
     */
    private void switchScenes() {

        // Load the new presentation scene
        if (mNewPresentationScene != null) {
            if (mPresentationScene != null) {

                // Deactivate the old presentation scene
                mScenesToDeactivate.add(mPresentationScene);
            }

            // And then load the new one
            mActiveScenes.add(mNewPresentationScene);
            mPresentationScene = mNewPresentationScene;
            mNewPresentationScene = null;
        }

        // Disable all scenes that need to be disabled
        for (Scene s : mScenesToDeactivate) {
            if (s == null) continue;
            mActiveScenes.remove(s);
            mInactiveScenes.add(s);
        }

        // Enable all scenes that need to be enabled
        for (Scene s : mScenesToActivate) {
            if (s == null) continue;
            mInactiveScenes.remove(s);
            mActiveScenes.add(s);
        }

        // Unload all scenes that need to be unloaded and flag all gameobjects for destruction
        for (Scene s : mScenesToUnload) {
            if (s == null) continue;
            mActiveScenes.remove(s);
            mInactiveScenes.remove(s);
            if (mPresentationScene != null && mPresentationScene == s) {
                mPresentationScene = null;
            }
            for (GameObject r : s.getGameObjects()) {
                r.destroy();
            }
        }
        mScenesToUnload.clear();
    }

    /** Destroy all game objects and components in all scenes. Used for cleanup */
    private void destroyAllObjects() {
        for (Scene s : mActiveScenes) {
            for (GameObject r : s.getGameObjects()) {
                r.engineDestroy();
            }
        }

        for (Scene s : mInactiveScenes) {
            for (GameObject r : s.getGameObjects()) {
                r.engineDestroy();
            }
        }

        for (Component c : mDestroyedComponents) {
            c.engineDestroy();
        }
    }

    /** Update the component lists in every active scene. */
    private void updateScenesComponentsList() {
        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);
            s.updateComponentsList();
        }
        Scene.setActiveScene(null);
    }

    /** Cleans up all resources used by the engine on shutdown. */
    private void cleanup() {
        // TODO: Release all resources that are still used at the time of shutdown here

        UPnP.deleteAllMappings();
        destroyAllObjects();
        mGLFWState.free();
    }

    /**
     * Add a component to the set of all components to be destroyed.
     *
     * @param component Component to be destroyed at the end of the current frame
     */
    public void addDestroyedComponent(Component component) {
        mDestroyedComponents.add(component);
    }

    /**
     * Getter for mInactiveScenes.
     *
     * @return mInactiveScenes
     */
    public ArrayList<Scene> getInactiveScenes() {
        return new ArrayList<>(mInactiveScenes);
    }

    /**
     * Getter for mActiveScenes.
     *
     * @return mActiveScenes
     */
    public ArrayList<Scene> getActiveScenes() {
        return new ArrayList<>(mActiveScenes);
    }

    /**
     * Get the single instance of the engine.
     *
     * @return The Engine instance
     */
    public static Engine getInstance() {
        return ENGINE_INSTANCE;
    }
}
