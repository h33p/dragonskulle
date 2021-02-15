/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import org.dragonskulle.components.Component;

import java.util.ArrayList;
import java.util.function.Predicate;
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

    private final ArrayList<Reference<Component>> mComponents = new ArrayList<>();

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
        mGameObjects.remove(object);
    }

    /**
     * Iterates through all GameObjects in the scene and collects their components
     */
    public void updateComponentsList() {
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
     * Get a list of references to all components
     *
     * @return mComponents
     */
    public ArrayList<Reference<Component>> getComponents() { return mComponents; }

    /**
     * Get a list of all enabled components in the scene
     *
     * @return A new ArrayList containing all of the enabled components
     */
    public ArrayList<Reference<Component>> getEnabledComponents() {
        Predicate<? super Reference<Component>> enabledComponents = (Reference<Component> ref) -> {
            Component component = ref.get();
            return component != null && component.getEnabled();
        };

        return mComponents.stream()
                .filter(enabledComponents)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
