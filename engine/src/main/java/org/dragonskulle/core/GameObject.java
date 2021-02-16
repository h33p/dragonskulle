/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.Transform;

/**
 * GameObject class
 *
 * @author Harry Stoltz
 *     <p>Each GameObject represents a single entity in the game, the object itself performs no
 *     actions but each component that is added to the GameObject will be able to interact with
 *     itself in the world and other GameObjects.
 */
public class GameObject {

    private final Reference<GameObject> mReference = new Reference<>(this);
    private final ArrayList<Component> mComponents = new ArrayList<>();
    private final ArrayList<GameObject> mChildren = new ArrayList<>();

    private GameObject mRoot;
    private GameObject mParent;
    private Transform mTransform = new Transform();
    private final String mName;
    // TODO: Make some sort of Tag class or enum and add here
    private boolean mActive;

    /**
     * Create a clone of a GameObject. The cloned GameObject's position is used
     *
     * @param object GameObject to be copied
     * @return The new instance of the GameObject
     */
    public static GameObject instantiate(GameObject object) {
        return new GameObject(object);
    }

    /**
     * Create a clone of a GameObject, providing a new transform for the object
     *
     * @param object GameObject to be copied
     * @param transform New transform for the object
     * @return The new instance of the GameObject
     */
    public static GameObject instantiate(GameObject object, Transform transform) {
        GameObject instance = new GameObject(object);
        instance.mTransform = transform;
        return instance;
    }

    /**
     * Find an instance of a GameObject with a given name in the currently active scene. This is
     * very slow and should not be used in any update loops. Instead, you should get all references
     * to necessary GameObject's in onAwake or onStart and save them for future use.
     *
     * @param name Name of the object to search for
     * @return A reference to the first GameObject found, or null if nothing is found
     */
    public static Reference<GameObject> FindObjectByName(String name) {
        Scene activeScene = Engine.getInstance().getActiveScene();

        for (GameObject root : activeScene.getRootObjects()) {

            for (GameObject obj : root.getAllChildren()) {

                if (obj.mName.equals(name)) {
                    return obj.getReference();
                }
            }
        }
        return null;
    }

    // TODO: GetComponentsIn(Parent/Children)

    /**
     * Constructor for GameObject, defaults mActive to true
     *
     * @param name The name of the object
     */
    public GameObject(String name) {
        mRoot = null;
        mParent = null;
        mName = name;
        mActive = true;
    }

    /**
     * Constructor for GameObject
     *
     * @param name The name of the object
     * @param active Whether the object is active or not
     */
    public GameObject(String name, boolean active) {
        mRoot = null;
        mParent = null;
        mName = name;
        mActive = active;
    }

    /**
     * Copy constructor for GameObject
     *
     * @param object The GameObject to copy
     */
    public GameObject(GameObject object) {

        // TODO: Rewrite this so that all children and components are new objects just with the same
        //       values etc

        mRoot = object.mRoot;
        mParent = object.mParent;
        mName = object.mName;
        mActive = object.mActive;
        mTransform = object.mTransform;

        mComponents.addAll(object.mComponents);
        mChildren.addAll(object.mChildren);
    }

    /**
     * Get all components of a given type T
     *
     * @param type Class object of T
     * @param <T> Type of component to be returned
     * @return A new ArrayList containing all components of type T, or null if none were found
     */
    public <T extends Component> ArrayList<Reference<T>> getComponents(Class<T> type) {
        ArrayList<Reference<T>> ret =
                mComponents.stream()
                        .filter(type::isInstance)
                        .map(type::cast)
                        .map(Reference::new)
                        .collect(Collectors.toCollection(ArrayList::new));

        return ret.isEmpty() ? null : ret;
    }

    /**
     * Get the first component of type T found
     *
     * @param type Class object of T
     * @param <T> Type of component to be returned
     * @return The first component of type T found, or null if none were found
     */
    public <T extends Component> Reference<T> getComponent(Class<T> type) {
        return mComponents.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .map(Reference::new)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a list of all components that implement the interface I
     *
     * @param iface Class object of the interface I
     * @param <I> Interface to search by
     * @return A new list containing all components that implement the interface I, or null
     */
    public <I> ArrayList<Reference<Component>> getComponentsByIface(Class<I> iface) {
        ArrayList<Reference<Component>> ret =
                mComponents.stream()
                        .filter(iface::isInstance)
                        .map(Reference::new)
                        .collect(Collectors.toCollection(ArrayList::new));

        return ret.isEmpty() ? null : ret;
    }

    /**
     * Recursively get all children of a game object List is ordered in breadth-first order
     *
     * @return List containing all children, children's children etc..
     */
    protected ArrayList<GameObject> getAllChildren() {
        ArrayList<GameObject> ret = new ArrayList<>(mChildren);

        for (GameObject child : mChildren) {
            ret.addAll(child.getAllChildren());
        }

        return ret;
    }

    /**
     * Add a component to the GameObject. If the component's GameObject is null, it is set to this
     * If the component's GameObject is another GameObject, the component is removed from that
     * GameObject and is set to this.
     *
     * @param component Component to be added
     */
    public void addComponent(Component component) {
        GameObject obj = component.getGameObject();

        if (obj == null) {
            // GameObject not set
            component.setGameObject(this);
        } else if (obj != this) {
            // GameObject is some other object
            obj.removeComponent(component);
            component.setGameObject(this);
        }

        // Add the component and set scene updated to true
        mComponents.add(component);
    }

    /**
     * Add a child to the GameObject. Setting the parent to this and the root to this.mRoot
     *
     * @param child GameObject to be added as a child
     */
    public void addChild(GameObject child) {
        // If this doesn't have a parent, it must be a root object so set the child's root to this
        if (mRoot == null) {
            child.mRoot = this;
        } else {
            child.mRoot = mRoot;
        }
        child.mParent = this;
        mChildren.add(child);
    }

    /**
     * Add a list of children to the GameObject, setting the parent to this and the root to mRoot,
     * or this if mRoot is null
     *
     * @param children List of GameObject to be added
     */
    public void addChildren(List<GameObject> children) {

        GameObject root = (mRoot == null) ? this : mRoot;

        for (GameObject child : children) {
            child.mRoot = root;
            child.mParent = this;
        }
        mChildren.addAll(children);
    }

    /**
     * Remove component from the GameObject. If a component is removed, the scene's updated flag is
     * set to true. Set's the component's GameObject to null
     *
     * @param component Component to be removed
     */
    public void removeComponent(Component component) {
        if (mComponents.remove(component)) {
            component.setGameObject(null);
        }
    }

    /**
     * Remove a child from the GameObject.
     *
     * @param child GameObject to be removed
     */
    public void removeChild(GameObject child) {
        mChildren.remove(child);
    }

    /**
     * Destroy the GameObject, destroying all children and components and then removing ourselves
     * from our parent
     */
    public void destroy() {

        // Copy the list of children so that as they are destroyed and unlinked from the list
        // the iteration occurs without error
        ArrayList<GameObject> children = new ArrayList<>(mChildren);

        // First destroy the children and the components
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

        // After we have finished destroying we need to clear our reference so nothing attempts to
        // access this
        mReference.clear();
    }

    /**
     * Getter for mChildren, should only be used by the engine
     *
     * @return mChildren
     */
    protected ArrayList<GameObject> getChildren() {
        return mChildren;
    }

    /**
     * Getter for mComponents, should only be used by the engine
     *
     * @return mComponents
     */
    protected ArrayList<Component> getComponents() {
        return mComponents;
    }

    /**
     * Getter for mActive
     *
     * @return mActive
     */
    public boolean getActive() {
        return mActive;
    }

    /**
     * Setter for mActive
     *
     * @param val New value for mActive
     */
    public void setActive(boolean val) {
        mActive = val;
    }

    /**
     * Getter for mTransform
     *
     * @return mTransform
     */
    public Transform getTransform() {
        return mTransform;
    }

    /**
     * Getter for mReference
     *
     * @return mReference
     */
    public Reference<GameObject> getReference() {
        return mReference;
    }

    /**
     * Getter for mRoot, used for testing
     *
     * @return mRoot
     */
    protected GameObject getRoot() {
        return mRoot;
    }

    /**
     * Getter for mParent, used for testing
     *
     * @return mParent
     */
    protected GameObject getParent() {
        return mParent;
    }
}
