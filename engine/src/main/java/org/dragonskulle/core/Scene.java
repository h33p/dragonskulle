/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.ArrayList;
import java.util.stream.Collectors;
import org.dragonskulle.components.Component;

/**
 * Scene class
 *
 * @author Harry Stoltz
 *     <p>Represents a single scene in a game, storing a list of all GameObjects in that scene.
 */
public class Scene {
    private final ArrayList<GameObject> mGameObjects = new ArrayList<>();

    private final ArrayList<Component> mComponents = new ArrayList<>();

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
        if (mGameObjects.remove(object)) {
            object.destroy();
        }
    }

    /** Iterates through all GameObjects in the scene and collects their components */
    public void updateComponentsList() {
        mComponents.clear();

        for (GameObject root : mGameObjects) {

            mComponents.addAll(root.getComponents());

            ArrayList<GameObject> children = new ArrayList<>();
            root.getAllChildren(children);

            for (GameObject child : children) {

                mComponents.addAll(child.getComponents());
            }
        }
    }

    /**
     * Getter for mGameObjects
     *
     * @return mGameObjects
     */
    public ArrayList<GameObject> getRootObjects() {
        return mGameObjects;
    }

    /**
     * Getter for mName
     *
     * @return mName
     */
    public String getName() {
        return mName;
    }

    /**
     * Get a list of all components
     *
     * @return mComponents
     */
    protected ArrayList<Component> getComponents() {
        return mComponents;
    }

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

    /**
     * Get a list of all components that aren't awake yet
     *
     * @return A new ArrayList containing all of the non awake components
     */
    protected ArrayList<Component> getNotAwakeComponents() {
        return mComponents.stream()
                .filter(component -> !component.isAwake())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get a list of all components that are enabled but have not been started yet
     *
     * @return A new ArrayList containing all of the enabled but not started components
     */
    protected ArrayList<Component> getEnabledButNotStartedComponents() {
        return mComponents.stream()
                .filter(Component::isEnabled)
                .filter(component -> !component.isStarted())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get a list of all components that are to be destroyed at the end of the current frame
     *
     * @return A new ArrayList containing all components with the destroy flag set
     */
    protected ArrayList<Component> getDestroyedComponents() {
        return mComponents.stream()
                .filter(Component::isDestroyed)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
