/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;

/**
 * Scene class.
 *
 * @author Harry Stoltz
 *     <p>Represents a single scene in a game, storing a list of all GameObjects in that scene.
 */
@Accessors(prefix = "m")
public class Scene {
    @Getter private final ArrayList<GameObject> mGameObjects = new ArrayList<>();

    @Getter private final ArrayList<Component> mComponents = new ArrayList<>();

    @Getter private final String mName;

    @Getter private final SingletonStore mSingletons = new SingletonStore();

    @Accessors(prefix = "s")
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private static Scene sActiveScene = null;

    /**
     * Constructor for a Scene.
     *
     * @param name Name of the scene
     */
    public Scene(String name) {
        mName = name;
    }

    /**
     * Add a new root object to the scene.
     *
     * @param object The GameObject to be added to the scene
     */
    public void addRootObject(GameObject object) {
        mGameObjects.add(object);
    }

    /**
     * Finds a root object by its name.
     *
     * @param name name of the object
     * @return the object, if found. {@code null} otherwise.
     */
    public GameObject findRootObject(String name) {
        return mGameObjects.stream()
                .filter(go -> go.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove a single game object from the scene.
     *
     * @param object The GameObject to be removed from the scene
     */
    public void removeRootObject(GameObject object) {
        mGameObjects.remove(object);
    }

    /**
     * Remove a single game object from the scene, and destroy it (immediately).
     *
     * <p>Try not to call this while in update loop :)
     *
     * @param object The GameObject to be removed from the scene
     */
    public void destroyRootObjectImmediate(GameObject object) {
        if (mGameObjects.remove(object)) {
            object.engineDestroy();
        }
    }

    /**
     * Moves a root object from one scene to the next.
     *
     * <p>Although
     */
    public void moveRootObjectToScene(GameObject object, Scene target) {
        if (mGameObjects.remove(object)) {
            target.addRootObject(object);
            object.recreateReferences();
        }
    }

    /**
     * Register a singleton
     *
     * <p>Allows to register a singleton for the scene. Singletons are meant for components that
     * should have only one, or only one main instance of them in the scene.
     *
     * @param comp component to register as a singleton
     * @return {@code true} if registration was successful, {@code false} if there already is a
     *     singleton.
     */
    public boolean registerSingleton(Component comp) {
        return mSingletons.register(comp);
    }

    /**
     * Retrieves a singleton for type if there is any.
     *
     * @param type type to retrieve the reference to
     * @return reference to the singleton. {@code null} if does not exist, or it has been destroyed
     */
    public <T extends Component> T getSingleton(Class<T> type) {
        return mSingletons.get(type);
    }

    /**
     * Retrieves a singleton reference for type if there is any.
     *
     * @param type type to retrieve the reference to
     * @return reference to the singleton. For invalid entries, it may be null, but may also be a
     *     non-null invalid reference.
     */
    public <T extends Component> Reference<T> getSingletonRef(Class<T> type) {
        return mSingletons.getRef(type);
    }

    /**
     * Unregisters a singleton.
     *
     * @param type type to unregister
     * @return component reference if there was a singleton
     */
    public Reference<Component> unregisterSingleton(Class<?> type) {
        return mSingletons.unregister(type);
    }

    /** Iterates through all GameObjects in the scene and collects their components. */
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
     * Get a list of all enabled components in the scene.
     *
     * @return A new ArrayList containing all of the enabled components
     */
    protected ArrayList<Component> getEnabledComponents() {
        return mComponents.stream()
                .filter(component -> component.getGameObject().isEnabled())
                .filter(Component::isEnabled)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get a list of all components that aren't awake yet.
     *
     * @return A new ArrayList containing all of the non awake components
     */
    protected ArrayList<Component> getNotAwakeComponents() {
        return mComponents.stream()
                .filter(component -> !component.isAwake())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get a list of all components that are enabled but have not been started yet.
     *
     * @return A new ArrayList containing all of the enabled but not started components
     */
    protected ArrayList<Component> getEnabledButNotStartedComponents() {
        return mComponents.stream()
                .filter(component -> component.getGameObject().isEnabled())
                .filter(Component::isEnabled)
                .filter(component -> !component.isStarted())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Scene)) {
            return false;
        } else {
            return ((Scene) o).getName().equals(mName);
        }
    }
}
