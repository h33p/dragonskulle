/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import org.dragonskulle.components.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;

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

    private final ArrayList<Component> mComponents = new ArrayList<>();

    private final String mName;

    private boolean mSceneChanged;

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
        if (mGameObjects.remove(object)) {
            object.destroy();
        }
    }

    /**
     * Get a list of all of the new components in the scene since the last frame
     *
     * @return A list containing all of the new components found
     */
    public ArrayList<Component> getNewComponents() {
        ArrayList<Component> ret = new ArrayList<>();

        // TODO: This is currently really inefficient so I'm sure there is a better way to do it

        for (GameObject root : mGameObjects) {

            for (Component component : root.getComponents()) {
                if (!mComponents.contains(component)) {
                    ret.add(component);
                }
            }

            for (GameObject child : root.getAllChildren()) {

                for (Component component : child.getComponents()) {
                    if (!mComponents.contains(component)) {
                        ret.add(component);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Iterates through all GameObjects in the scene and collects their components
     */
    public void updateComponentsList() {

        // TODO: Add some check whether any components have been added/changed/removed so that
        //       the list is only updated when necessary

        mComponents.clear();

        for (GameObject root : mGameObjects) {

            mComponents.addAll(root.getComponents());

            for (GameObject child : root.getAllChildren()) {

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

    /**
     * Get a list of all components
     *
     * @return mComponents
     */
    protected ArrayList<Component> getComponents() { return mComponents; }

    /**
     * Get a list of all enabled components in the scene
     *
     * @return A new ArrayList containing all of the enabled components
     */
    protected ArrayList<Component> getEnabledComponents() {
        return mComponents.stream()
                .filter(Component::isEnabled)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
