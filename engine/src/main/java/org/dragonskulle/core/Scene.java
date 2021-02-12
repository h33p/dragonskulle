/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.ArrayList;

/**
 * Scene class
 *
 * @author Harry Stoltz
 *      <p>
 *          Represents a single scene in a game, storing a list of all GameObjects in that scene.
 *          Allows for adding and removing of GameObjects.
 *      </p>
 */
public class Scene {
    private final ArrayList<GameObject> mGameObjects = new ArrayList<>();
    private final String mName;

    private boolean mSceneUpdated = false;

    /**
     * Constructor for a Scene
     *
     * @param name Name of the scene
     */
    public Scene(String name) {
        mName = name;
    }

    /**
     * Add a single game object to the scene
     *
     * @param object The GameObject to be added to the scene
     */
    public void addGameObject(GameObject object) {
        mGameObjects.add(object);
        mSceneUpdated = true;
    }

    /**
     * Remove a single game object from the scene
     *
     * @param object The GameObject to be removed from the scene
     */
    public void destroyGameObject(GameObject object) {
        mGameObjects.remove(object);
        mSceneUpdated = true;
    }

    /**
     * Getter for mGameObjects
     *
     * @return mGameObjects
     */
    public ArrayList<GameObject> getGameObjects() { return mGameObjects; }

    /**
     * Getter for mName
     *
     * @return mName
     */
    public String getName() { return mName; }

    /**
     * Getter for mSceneUpdated
     *
     * @return mSceneUpdated
     */
    public boolean getSceneUpdated() { return mSceneUpdated; }

    /**
     * Setter for mSceneUpdated
     *
      * @param val New value of mSceneUpdated
     */
    public void setSceneUpdated(boolean val) { mSceneUpdated = val; }
}
