/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
public class GameObject implements Serializable {

    private final Reference<GameObject> mReference = new Reference<>(this);
    private final ArrayList<Component> mComponents = new ArrayList<>();
    private final ArrayList<GameObject> mChildren = new ArrayList<>();

    private GameObject mRoot;
    private GameObject mParent;
    private Transform mTransform = new Transform();
    private final String mName;
    // TODO: Make some sort of Tag class or enum and add here
    private boolean mActive;
    private boolean mDestroy = false;

    /**
     * Create a clone of a GameObject. The cloned GameObject's position is used
     *
     * @param object GameObject to be copied
     * @return The new instance of the GameObject
     */
    public static GameObject instantiate(GameObject object) {
        return object.createClone();
    }

    /**
     * Create a clone of a GameObject, providing a new transform for the object
     *
     * @param object GameObject to be copied
     * @param transform New transform for the object
     * @return The new instance of the GameObject
     */
    public static GameObject instantiate(GameObject object, Transform transform) {
        GameObject instance = object.createClone();
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

            ArrayList<GameObject> children = new ArrayList<>();
            root.getAllChildren(children);

            for (GameObject obj : children) {

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
     * Get all components of a given type T
     *
     * @param type Class object of T
     * @param <T> Type of component to be returned
     * @param ret List that will store the components found
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> void getComponents(Class<T> type, List<Reference<T>> ret) {
        mComponents.stream()
                .filter(type::isInstance)
                .map(component -> (Reference<T>) component.getReference())
                .collect(Collectors.toCollection(() -> ret));
    }

    /**
     * Get the first component of type T found
     *
     * @param type Class object of T
     * @param <T> Type of component to be returned
     * @return The first component of type T found, or null if none were found
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> Reference<T> getComponent(Class<T> type) {
        return mComponents.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .map(component -> (Reference<T>) component.getReference())
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a list of all components that implement the interface I
     *
     * @param iface Class object of the interface I
     * @param <I> Interface to search by
     * @param ret List that will contain any components found
     */
    public <I> void getComponentsByIface(Class<I> iface, List<Reference<Component>> ret) {
        mComponents.stream()
                .filter(iface::isInstance)
                .map(Component::getReference)
                .collect(Collectors.toCollection(() -> ret));
    }

    /**
     * Get every child with this GameObject acting as the root in a tree, adding to the list in a
     * depth-first order.
     *
     * <p>Doesn't return a list of references as this method should only be used by the engine which
     * is responsible for the destroying of objects and therefore won't keep any strong references
     * to destroyed objects.
     */
    protected void getAllChildren(List<GameObject> ret) {
        for (GameObject child : mChildren) {
            ret.add(child);
            child.getAllChildren(ret);
        }
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

        // Add the component
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

    /** Handle the destruction of the object. */
    protected void engineDestroy() {

        ArrayList<GameObject> children = new ArrayList<>(mChildren);

        // Destroy all children
        for (GameObject child : children) {
            child.engineDestroy();
        }

        // Set the destroy flag on all components
        for (Component component : mComponents) {
            component.engineDestroy();
        }

        // After we have finished destroying we need to clear our reference so nothing attempts to
        // access this
        mReference.clear();

        // Then remove this GameObject from the parent and remove the link to the parent
        if (mParent != null) {
            mParent.removeChild(this);
            mParent = null;
        }
    }

    /**
     * Set the destroy flag to true. The object won't actually be destroyed until the end of the
     * current render frame
     */
    public void destroy() {
        mDestroy = true;
    }

    /**
     * Create a deep copy of the GameObject
     *
     * @return New GameObject with identical values as this
     */
    public GameObject createClone() {
        byte[] objectData = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.flush();
            oos.close();
            baos.close();
            objectData = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (objectData != null) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(objectData);
                return (GameObject) new ObjectInputStream(bais).readObject();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
        return null;
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
    public boolean isActive() {
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
     * Getter for mDestroy
     *
     * @return mDestroy
     */
    public boolean isDestroyed() {
        return mDestroy;
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
