/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Engine core
 *
 * @author Harry Stoltz
 *      <p>
 *      The core of the engine, contains the main loop which executes all game logic. Gives
 *      all components access to engine components such as the AudioManager and InputManager.
 *      </p>
 */
public class Engine {
    private static final Engine ENGINE_INSTANCE = new Engine();

    private static final int UPDATES_PER_SECOND = 30; // Target number of fixed updates per second
    private static final double UPDATE_TIME = 1 / (double)UPDATES_PER_SECOND;

    private boolean mIsRunning = false;

    private final HashSet<Scene> mInactiveScenes = new HashSet<>();
    private Scene mActiveScene = null;
    private Scene mNewScene = null;

    private Engine() {}

    /**
     * Loads a new scene and start the engine
     *
     * @param scene Initial scene for the engine to run
     */
    public void start(Scene scene) {
        loadScene(scene);

        // TODO: Any initialization of engine components like renderer, audio, input, etc done here

        mIsRunning = true;
        mainLoop();
    }

    /**
     * Loads a new scene that will be run from the next frame onwards
     *
     * @param scene Scene to be loaded
     */
    public void loadScene(Scene scene) {
        mNewScene = scene;
    }

    /**
     * Stops the engine when the current frame has finished
     */
    public void stop() {
        mIsRunning = false;
    }

    /**
     * Main loop of the engine
     */
    private void mainLoop() {

        double mPrevTime = Time.getTimeInSeconds();

        // Basic frame counter
        int frames = 0;
        double secondTimer = 0;
        double cumulativeTime = 0;


        // TODO: Make objects only be destroyed after all updates
        // TODO: Only initialize new components at the start of next frame
        // TODO: Only update the component list at the start of a frame
        //          -> Split into separate lists depending on interfaces?

        // For only destroying at end of frame have a flag on objects called mDestroy
        // If it's true at the end of the frame, call the destroy method

        // Likewise have a boolean for awake and start
        // if awake is false, call onAwake, if started is false and enabled is true, call onStart

        while (mIsRunning) {

            if (mNewScene != null) {
                switchToNewScene();
            }

            onStartAndAwake(mActiveScene.getNewComponents());

            // Calculate time for last frame
            double mCurTime = Time.getTimeInSeconds();
            double deltaTime = mCurTime - mPrevTime;
            mPrevTime = mCurTime;

            cumulativeTime += deltaTime;
            secondTimer += deltaTime;

            // TODO: Process inputs here before any updates are performed

            frameUpdate(deltaTime);

            // Perform all updates that we can fit in the time since last frame
            // Means that multiple fixed updates can happen before the next frame
            // if rendering to screen is taking a very long time
            while (cumulativeTime > UPDATE_TIME) {
                cumulativeTime -= UPDATE_TIME;

                fixedUpdate();
            }

            // TODO: Perform actual rendering after all updates

            lateFrameUpdate(deltaTime);

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

    /**
     * Call onStart and onAwake for all components that implement it
     *
     * @param components List of components to be checked
     */
    private void onStartAndAwake(ArrayList<Component> components) {

        // Iterate through them, calling onAwake on all that implement it
        for (Component component : components) {
            if (component instanceof IOnAwake) {
                ((IOnAwake)component).onAwake();
            }
        }

        // Then go through again, calling onStart on all the implement it
        for (Component component : components) {
            if (component instanceof IOnStart) {
                ((IOnStart)component).onStart();
            }
        }
    }

    /**
     * Do all frameUpdates on components that implement it
     * @param deltaTime Time change since last frame
     */
    private void frameUpdate(double deltaTime) {
        mActiveScene.updateComponentsList();

        for (Component component : mActiveScene.getEnabledComponents()) {
            if (component instanceof IFrameUpdate) {
                ((IFrameUpdate)component).frameUpdate(deltaTime);
            }
        }
    }

    /**
     * Do all Fixed Updates on components that implement it
     */
    private void fixedUpdate() {
        mActiveScene.updateComponentsList();

        for (Component component : mActiveScene.getEnabledComponents()) {
            if (component instanceof IFixedUpdate) {
                ((IFixedUpdate)component).fixedUpdate(UPDATE_TIME);
            }
        }
    }

    /**
     * Do all Late Frame Updates on components that implement it
     * @param deltaTime Time change since last frame
     */
    private void lateFrameUpdate(double deltaTime) {
        mActiveScene.updateComponentsList();

        for (Component component : mActiveScene.getEnabledComponents()) {
            if (component instanceof ILateFrameUpdate) {
                ((ILateFrameUpdate)component).lateFrameUpdate(deltaTime);
            }
        }
    }

    /**
     * Finish the loading of a new scene.
     * If the scene has never been active before, call the onAwake and onStart methods
     * if they are implemented
     */
    private void switchToNewScene() {
        // Add the currently active scene to inactive scenes and remove the new scene from
        // the set if it exists
        mInactiveScenes.add(mActiveScene);
        boolean sceneWasInactive = mInactiveScenes.remove(mNewScene);

        mActiveScene = mNewScene;
        mNewScene = null;

        // Scene has never been active before
        // Or do we still need to call awake and start even if it has been active before?
        if (!sceneWasInactive) {

            // Get the initial list of components in the scene
            mActiveScene.updateComponentsList();

            onStartAndAwake(mActiveScene.getComponents());
        }
    }

    /**
     * Cleans up all resources used by the engine on shutdown
     */
    private void cleanup() {
        // TODO: Release all resources that are still used at the time of shutdown here
    }

    /**
     * Getter for mActiveScene
     *
     * @return mActiveScene
     */
    public Scene getActiveScene() { return mActiveScene; }

    /**
     * Get the single instance of the engine
     *
     * @return The Engine instance
     */
    static Engine getInstance() {
        return ENGINE_INSTANCE;
    }
}
