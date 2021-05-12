/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import com.rits.cloning.Cloner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.dragonskulle.core.futures.Future;
import org.dragonskulle.input.Bindings;
import org.dragonskulle.network.UPnP;
import org.dragonskulle.renderer.RendererException;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.settings.Settings;
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

    /** Contains list of objects that are to be destroyed at the end of loop iteration. */
    protected final HashSet<GameObject> mDestroyedObjects = new HashSet<>();
    /** Contains list of components that are to be destroyed at the end of loop iteration. */
    protected final HashSet<Component> mDestroyedComponents = new HashSet<>();
    /** Contains a map of objects that are to be enabled/disabled at the end of loop iteration. */
    protected final HashMap<GameObject, Boolean> mDisabledObjects = new HashMap<>();

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
    @Getter private float mFrameDeltaTime = 0f;

    private final ArrayList<Renderable> mTmpRenderables = new ArrayList<>();
    private final ArrayList<Light> mTmpLights = new ArrayList<>();

    /** Interface used for supplying an exit condition to the main game loop. */
    public interface IEngineExitCondition {
        /**
         * Called in every single iteration of the game loop. Used for checking whether the game
         * loop should exit or not
         *
         * @return false if the game loop should exist, true if it should keep running.
         */
        boolean shouldExit();
    }

    /** Interface used for scheduling events for a later time. */
    public interface IScheduledEvent {
        /** Calls the scheduled event. */
        void invoke();
    }

    /** Default constructor. */
    private Engine() {}

    /**
     * Loads a new scene and start the engine.
     *
     * @param gameName Name of the game
     * @param bindings User input bindings
     * @param settings Settings instance to use
     */
    public void start(String gameName, Bindings bindings, Settings settings) {
        // TODO: Any initialization of engine components like renderer, audio, input, etc done here

        try {
            mGLFWState = new GLFWState(WINDOW_WIDTH, WINDOW_HEIGHT, gameName, bindings, settings);
        } catch (RendererException e) {
            log.severe("Renderer exception occured!");
            e.printStackTrace();
            return;
        }

        UPnP.initialise();

        mIsRunning = true;
        mainLoop(mGLFWState::processEvents, true);

        cleanup();
    }

    /**
     * Runs the engine with a list of futures, until they all finish.
     *
     * <p>Each future will have a scene assigned to them, and be executed at the same time. Note,
     * that while inside the future context, {@code Scene.getActiveScene()} will always return
     * {@code null}, so you can use the input argument as active scene.
     *
     * @param futures a list of futures to execute.
     */
    public synchronized void startWithFutures(Future... futures) {
        int cnt = 0;
        int[] loadedScenes = {0};

        for (Future future : futures) {
            Scene scene = new Scene("future" + cnt);
            future.then(this::unloadScene).then((__) -> loadedScenes[0]--).schedule(scene);
            cnt++;
            loadedScenes[0]++;
            activateScene(scene);
        }

        mIsRunning = true;
        mainLoop(() -> loadedScenes[0] != 0, false);

        cleanup();
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

    /**
     * Schedule an event for the next frame update.
     *
     * @param event Event to schedule.
     */
    public void scheduleFrameEvent(IScheduledEvent event) {
        mFrameEvents.add(event);
    }

    /**
     * Schedule an event for the next fixed update.
     *
     * @param event Event to schedule.
     */
    public void scheduleFixedUpdateEvent(IScheduledEvent event) {
        mFixedUpdateEvents.add(event);
    }

    /**
     * Schedule an event for the end of main loop iteration.
     *
     * @param event Event to schedule.
     */
    public void scheduleEndOfLoopEvent(IScheduledEvent event) {
        mEndOfLoopEvents.add(event);
    }

    /** Stops the engine when the current frame has finished. */
    public void stop() {
        mIsRunning = false;
    }

    /**
     * Main loop of the engine.
     *
     * @param exitCondition Exit condition that should be checked every iteration.
     * @param present Whether any rendering should be done or not.
     */
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

            mFrameDeltaTime = (float) deltaTime;

            cumulativeTime += deltaTime;

            boolean triggerFixedUpdate = cumulativeTime > UPDATE_TIME;

            // Update scenes
            switchScenes();

            // Wake up all components that aren't awake (Called on all active scenes)
            wakeComponents();

            // Start all enabled components (Called on all active scenes)
            startEnabledComponents();

            mIsRunning = exitCondition.shouldExit();

            if (present) {
                Scene.setActiveScene(mPresentationScene);
                UIManager.getInstance().uiUpdate(mPresentationScene.getEnabledComponents());

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

            // Disable all objects that have been deferred to do so
            disableObjects();

            // Destroy all objects and components that were destroyed this frame
            destroyObjectsAndComponents();
        }
    }

    /** Iterate through a list of components that aren't awake and wake them. */
    private void wakeComponents() {
        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);

            List<Component> list = s.getNotAwakeComponents();

            for (Component component : list) {
                if (component instanceof IOnAwake) {
                    ((IOnAwake) component).onAwake();
                }
                component.setAwake(true);
            }

            if (list.size() > 0) {
                s.dirtyToStartComponents();
            }

            list.clear();
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

            List<Component> list = s.getEnabledButNotStartedComponents();

            for (Component component : list) {
                if (component instanceof IOnStart) {
                    ((IOnStart) component).onStart();
                }
                component.setStarted(true);
            }

            if (list.size() > 0) {
                s.dirtyEnabledComponents();
            }

            list.clear();
        }
        Scene.setActiveScene(null);
    }

    /**
     * Invoke all events passed to the method.
     *
     * @param toConsume List containing all events that should be invoked.
     */
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

        for (IFrameUpdate component : mPresentationScene.getComponentsByIface(IFrameUpdate.class)) {
            component.frameUpdate(deltaTime);
        }
    }

    /** Do all Fixed Updates on components that implement it. */
    private void fixedUpdate() {
        ArrayList<IScheduledEvent> toConsume = mFixedUpdateEvents;
        mFixedUpdateEvents = mEventsToConsume;
        consumeEvents(toConsume);

        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);
            for (IFixedUpdate component : s.getComponentsByIface(IFixedUpdate.class)) {
                component.fixedUpdate(UPDATE_TIME);
            }
        }
        Scene.setActiveScene(null);
    }

    /** Do all Network Updates on components that implement it. */
    private void networkUpdate() {
        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);
            for (INetworkUpdate component : s.getComponentsByIface(INetworkUpdate.class)) {
                component.networkUpdate();
            }
        }
        Scene.setActiveScene(null);
    }

    /** Do all Late Network Updates on components that implement it. */
    private void lateNetworkUpdate() {
        for (Scene s : mActiveScenes) {
            Scene.setActiveScene(s);
            for (ILateNetworkUpdate component : s.getComponentsByIface(ILateNetworkUpdate.class)) {
                component.lateNetworkUpdate();
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
        for (ILateFrameUpdate component :
                mPresentationScene.getComponentsByIface(ILateFrameUpdate.class)) {
            component.lateFrameUpdate(deltaTime);
        }
    }

    /** Commit object enabled state changes. */
    private void disableObjects() {
        mDisabledObjects.forEach((k, v) -> k.setEnabledImmediate(v));
        mDisabledObjects.clear();
    }

    /** Destroy all GameObjects and Components that need to be destroyed. */
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

    /** Render a single frame. */
    private void renderFrame() {
        mTmpRenderables.clear();
        mTmpLights.clear();

        for (Renderable component : mPresentationScene.getComponentsByIface(Renderable.class)) {
            mTmpRenderables.add(component);
        }

        for (Light component : mPresentationScene.getComponentsByIface(Light.class)) {
            mTmpLights.add(component);
        }

        Camera mainCamera = mPresentationScene.getSingleton(Camera.class);

        if (mainCamera != null) {
            try {
                mGLFWState.getRenderer().render(mainCamera, mTmpRenderables, mTmpLights);
            } catch (RendererException e) {
                log.severe("Renderer exception! " + e);
                mIsRunning = false;
            }
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

    /** Cleans up all resources used by the engine on shutdown. */
    private void cleanup() {
        // TODO: Release all resources that are still used at the time of shutdown here

        UPnP.deleteAllMappings();
        destroyAllObjects();

        if (mGLFWState != null) {
            mGLFWState.free();
            mGLFWState = null;
        }
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
