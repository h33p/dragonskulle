/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import org.dragonskulle.components.Component;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Scene class
 *
 * @author Harry Stoltz
 *      <p>
 *          Represents a single scene in a game, storing a list of all GameObjects in that scene.
 *      </p>
 */
public class Scene {
    private final ArrayList<GameObject> mGameObjects = new ArrayList<>();

    // For now I'm just going to have a list of all components in each scene
    // This will be updated each frame before any updates
    // So that only one iteration through all objects is required each frame
    // In the future it might be a good idea to further break this down
    // for each interface of components and only update the cache when the scene is updated
    // This won't affect the use of the engine so won't introduce any conflicts
    private final ArrayList<WeakReference<Component>> mComponents = new ArrayList<>();

    private final String mName;

    /**
     * Constructor for a Scene
     *
     * @param name Name of the scene
     */
    public Scene(String name) {
        mName = name;
    }

    /**
     * Add a new root object to the scene
     *
     * @param object The GameObject to be added to the scene
     */
    public void addRootObject(GameObject object) {
        mGameObjects.add(object);
    }

    /**
     * Remove a single game object from the scene
     *
     * @param object The GameObject to be removed from the scene
     */
    public void destroyRootObject(GameObject object) {
        mGameObjects.remove(object);
    }

    /**
     * Iterates through all GameObjects in the scene and collects their components
     */
    public void updateComponentsList() {
        mComponents.clear();

        for (GameObject root : mGameObjects) {

            mComponents.addAll(root.getComponents());

            for (WeakReference<GameObject> childRef : root.getAllChildren()) {
                GameObject child = childRef.get();
                if (child == null) {
                    continue;
                }
                mComponents.addAll(child.getComponents());
            }
        }

    }

    /**
     * Getter for mGameObjects
     *
     * @return mGameObjects
     */
    public ArrayList<GameObject> getRootObjects() { return mGameObjects; }

    /**
     * Getter for mName
     *
     * @return mName
     */
    public String getName() { return mName; }

    public ArrayList<WeakReference<Component>> getComponents() { return mComponents; }
}
