/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

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

    private double mPrevTime = 0;
    private double mCurTime = 0;

    // TODO: Maintain a cache of all active components with each interface type so that we don't
    //       have to iterate through all game objects and components 5 times each frame

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

        mPrevTime = Time.getTimeInSeconds();

        // Basic frame counter
        int frames = 0;
        double secondTimer = 0;
        double cumulativeTime = 0;

        while (mIsRunning) {

            if (mNewScene != null) {
                switchToNewScene();
            }

            // Calculate time for last frame
            mCurTime = Time.getTimeInSeconds();
            double deltaTime = mCurTime - mPrevTime;
            mPrevTime = mCurTime;

            cumulativeTime += deltaTime;
            secondTimer += deltaTime;

            // TODO: Process inputs here before any updates are performed


            // TODO: Frame updates should be called here

            // Perform all updates that we can fit in the time since last frame
            // Means that multiple fixed updates can happen before the next frame
            // if rendering to screen is taking a very long time
            while (cumulativeTime > UPDATE_TIME) {
                cumulativeTime -= UPDATE_TIME;

                // TODO: perform all fixed updates here

            }

            // TODO: Late frame updates should be called here


            // TODO: Perform actual rendering after all updates

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
     * Finish the loading of a new scene. If the scene has never been active before, call the
     * onAwake and onStart methods if they are implemented
     */
    private void switchToNewScene() {
        // Add the currently active scene to inactive scenes and remove the new scene from
        // the set if it exists
        mInactiveScenes.add(mActiveScene);
        boolean sceneWasInactive = mInactiveScenes.remove(mNewScene);

        mActiveScene = mNewScene;
        mNewScene = null;

        // Scene has never been active before
        if (!sceneWasInactive) {
            // TODO: Call onAwake and onStart on all components that implement them
        }
    }

    /**
     * Cleans up all resources used by the engine on shutdown
     */
    private void cleanup() {
        // TODO: Release all resources that are still used at the time of shutdown here
    }

    /**
     * Get the single instance of the engine
     *
     * @return The Engine instance
     */
    static Engine getInstance() {
        return ENGINE_INSTANCE;
    }
}
