/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import org.dragonskulle.components.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GameObject class
 *
 * @author Harry Stoltz
 *      <p>
 *          Each GameObject represents a single entity in the game, the object itself performs no
 *          actions but each component that is added to the GameObject will be able to interact with
 *          itself in the world and other GameObjects.
 *      </p>
 */
public class GameObject {

    private final ArrayList<Component> mComponents = new ArrayList<>();
    private final ArrayList<GameObject> mChildren = new ArrayList<>();

    private Scene mScene;
    private GameObject mParent;
    // TODO: Make transform component and add one here
    private final String mName;
    // TODO: Make some sort of Tag class or enum and add here
    private final boolean mActive;

    /**
     * Constructor for GameObject, doesn't set mScene, mParent and defaults mActive to true
     *
     * @param name The name of the object
     */
    public GameObject(String name) {
        mScene = null;
        mParent = null;
        mName = name;
        mActive = true;
    }

    /**
     * Constructor for GameObject, doesn't set mScene, or mParent
     *
     * @param name The name of the object
     * @param active Whether the object is active or not
     */
    public GameObject(String name, boolean active) {
        mScene = null;
        mParent = null;
        mName = name;
        mActive = active;
    }

    /**
     * Constructor for GameObject, defaults mActive to true
     *
     * @param scene Scene for the object
     * @param parent The objects parent
     * @param name The name of the object
     */
    public GameObject(Scene scene, GameObject parent, String name) {
        mScene = scene;
        mParent = parent;
        mName = name;
        mActive = true;
    }

    public GameObject(Scene scene, GameObject parent, String name, boolean active) {
        mScene = scene;
        mParent = parent;
        mName = name;
        mActive = active;
    }

    /**
     * Copy constructor for GameObject
     *
     * @param object The GameObject to copy
     */
    public GameObject(GameObject object) {
        mScene = object.mScene;
        mParent = object.mParent;
        mName = object.mName;
        mActive = object.mActive;

        mComponents.addAll(object.mComponents);
        mChildren.addAll(object.mChildren);
    }


    /**
     *  Get all components of a given type T
     *
     * @param type Class object of T
     * @param <T> Type of component to be returned
     * @return A new ArrayList containing all components of type T, or null if none were found
     */
    public <T extends Component> ArrayList<T> getComponents(Class<T> type) {
        ArrayList<T> ret = mComponents.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toCollection(ArrayList::new));

        if (ret.isEmpty()) {
            return null;
        } else {
            return ret;
        }
    }

    /**
     * Get the first component of type T
     *
     * @param type Class object of T
     * @param <T> Type of component to be returned
     * @return The first component of type T found, or null if none were found
     */
    public <T extends Component> T getComponent(Class<T> type) {
        return mComponents.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Recursively get all children of a game object
     *
     * List is ordered in breadth-first order
     *
     * @return List containing all children, children's children etc..
     */
    ArrayList<GameObject> getAllChildren() {
        ArrayList<GameObject> ret = new ArrayList<>(mChildren);

        for (GameObject child : mChildren) {
            ret.addAll(child.getAllChildren());
        }

        return ret;
    }

    /**
     * Add a component to the GameObject, making sure that the component's owner is set to this
     * and if it isn't, the Component is not added.
     *
     * If a component is added, the scene's updated flag is set to true
     *
     * @param component Component to be added
     */
    public void addComponent(Component component) {
        if (component.getGameObject() == this) {
            mComponents.add(component);

            mScene.setSceneUpdated(true);
        }
    }

    /**
     * Add a child to the GameObject
     *
     * @param child GameObject to be added as a child
     */
    public void addChild(GameObject child) {
        child.mScene = mScene;
        child.mParent = this;
        mChildren.add(child);
    }

    /**
     * Add a list of children to the GameObject
     *
     * @param children List of GameObject to be added
     */
    public void addChildren(List<GameObject> children) {
        for (GameObject child : children) {
            child.mScene = mScene;
            child.mParent = this;
        }
        mChildren.addAll(children);
    }

    /**
     * Remove component from the GameObject.
     *
     * If a component is removed, the scene's updated flag is set to true
     *
     * @param component Component to be removed
     */
    public void removeComponent(Component component) {
        if (mComponents.remove(component)) {
            mScene.setSceneUpdated(true);
        }
    }

    /**
     * Remove a child from the GameObject.
     *
     * If a GameObject is removed, the scene's updated flag is set to true
     *
     * @param child GameObject to be removed
     */
    public void removeChild(GameObject child) {
        if (mChildren.remove(child)) {
            mScene.setSceneUpdated(true);
        }
    }

    /**
     * Destroy the GameObject, destroying all children and components and then removing ourselves
     * from our parent
      */
    public void destroy() {

        // Copy the list of children so that as they are destroyed and unlinked from the list
        // the iteration occurs without error
        ArrayList<GameObject> children = new ArrayList<>(mChildren);

        // First destroy the children, then the components and then remove ourselves from our parent
        for (GameObject child : children) {
            child.destroy();
        }

        for (Component component : mComponents) {
            component.destroy();
        }

        // Check that the parent isn't null before removing this
        if (mParent != null) {
            mParent.removeChild(this);
        }
    }

}
