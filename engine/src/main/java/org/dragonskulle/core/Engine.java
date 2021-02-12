/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

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

    private final Scene mActiveScene = null;
    private Scene mNewScene = null;

    private double mCurTime;


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



        while (mIsRunning) {

            // TODO: Load new scene if necessary

            // TODO: Calculate time since last frame


        }

        cleanup();
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
