/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

    /** Contains a cached list of all components on the scene. */
    private final ArrayList<Component> mComponents = new ArrayList<>();
    /** Whether mComponents is dirty. */
    private boolean mComponentListDirty = true;

    /** Contains a cached list of all components that need starting on the scene. */
    private final ArrayList<Component> mToStartComponents = new ArrayList<>();
    /** Whether mToStartComponents is dirty. */
    private boolean mToStartComponentsDirty = false;

    /** Contains a cached list of all components that are not yet awake on the scene. */
    private final ArrayList<Component> mNotAwakeComponents = new ArrayList<>();
    /** Whether or not mNotAwakeComponents is dirty. */
    private boolean mNotAwakeComponentsDirty = false;

    /** Contains a cached list of all enabled and started components on the scene. */
    private final ArrayList<Component> mEnabledComponents = new ArrayList<>();
    /** Whether or not mEnabledComponents is dirty. */
    private boolean mEnabledComponentsDirty = false;

    /** List of enabled components by specific class types. */
    private final Map<Class<?>, CompList<?>> mInterfaceComponents = new HashMap<>();

    /** Name used to identify the scene. */
    @Getter private final String mName;

    /** Registered singletons on the scene. */
    @Getter private final SingletonStore mSingletons = new SingletonStore();

    /** Per-class component list. */
    private class CompList<T> {
        /** Is this list valid. */
        private boolean mIsValid = false;
        /** Type contained within the list. */
        private final Class<T> mType;
        /** The list itself. */
        private final ArrayList<T> mList = new ArrayList<>();

        /**
         * Create a component list.
         *
         * @param type type of the list.
         */
        public CompList(Class<T> type) {
            mType = type;
        }

        /**
         * Clear the component list.
         *
         * <p>This method will clear the underlying list, and mark it as invalid.
         */
        public void clear() {
            mIsValid = false;
            mList.clear();
        }

        /**
         * Get the component list.
         *
         * <p>This method will return cached component list, constructing it if needed.
         *
         * @return component list for this type of data.
         */
        public ArrayList<T> getList() {
            if (!mIsValid) {
                mList.clear();

                for (Component comp : getEnabledComponents()) {
                    if (mType.isInstance(comp)) {
                        mList.add(mType.cast(comp));
                    }
                }

                mIsValid = true;
            }

            return mList;
        }
    }

    /** Currently active scene. */
    @Accessors(prefix = "s")
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private static Scene sActiveScene = null;

    /** Allows to temporarily override active scene. */
    public static class SceneOverride implements AutoCloseable {
        private final Scene mPrevScene;

        /**
         * Create a scene override.
         *
         * <p>This override will be active until it is closed.
         *
         * @param newScene new scene to set as active scene.
         */
        public SceneOverride(Scene newScene) {
            mPrevScene = sActiveScene;
            sActiveScene = newScene;
        }

        @Override
        public void close() {
            sActiveScene = mPrevScene;
        }
    }

    /** Mark the component list on this scene as dirty. */
    public void dirtyComponentLists() {
        mComponentListDirty = true;
    }

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
        object.setScene(this);
        dirtyComponentLists();
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
        object.setScene(null);
        dirtyComponentLists();
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
        dirtyComponentLists();
    }

    /**
     * Moves a root object from one scene to the next.
     *
     * @param object object to move.
     * @param target target scene to move the object to.
     */
    public void moveRootObjectToScene(GameObject object, Scene target) {
        if (mGameObjects.remove(object)) {
            target.addRootObject(object);
            object.recreateReferences();
            object.setScene(target);
        }
        dirtyComponentLists();
        target.dirtyComponentLists();
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
     * @param <T> type of the singleton.
     * @param type type to retrieve the reference to
     * @return reference to the singleton. {@code null} if does not exist, or it has been destroyed
     */
    public <T extends Component> T getSingleton(Class<T> type) {
        return mSingletons.get(type);
    }

    /**
     * Retrieves a singleton reference for type if there is any.
     *
     * @param <T> type of the singleton.
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

        if (!mComponentListDirty) return;

        mComponentListDirty = false;

        mEnabledComponentsDirty = true;
        mToStartComponentsDirty = true;
        mNotAwakeComponentsDirty = true;

        mComponents.clear();

        mInterfaceComponents.values().stream().forEach(CompList::clear);

        for (GameObject root : mGameObjects) {
            mComponents.addAll(root.getComponents());

            ArrayList<GameObject> children = new ArrayList<>();
            root.getAllChildren(children);

            for (GameObject child : children) {

                mComponents.addAll(child.getComponents());
            }
        }
    }

    /** Dirty the enabled component list. */
    public void dirtyEnabledComponents() {
        mEnabledComponentsDirty = true;
        mInterfaceComponents.values().stream().forEach(CompList::clear);
    }

    /**
     * Get a list of all enabled components in the scene.
     *
     * @return A new ArrayList containing all of the enabled components
     */
    protected ArrayList<Component> getEnabledComponents() {
        updateComponentsList();

        if (mEnabledComponentsDirty) {
            mEnabledComponents.clear();
            mComponents.stream()
                    .filter(component -> component.getGameObject() != null)
                    .filter(component -> component.getGameObject().isEnabled())
                    .filter(component -> component.isAwake())
                    .filter(component -> component.isStarted())
                    .filter(Component::isEnabled)
                    .collect(Collectors.toCollection(() -> mEnabledComponents));
            mEnabledComponentsDirty = false;
        }

        return mEnabledComponents;
    }

    /**
     * Get a component list by their class.
     *
     * @param <T> type of the component.
     * @param type class of type T.
     * @return {@link ArrayList} with components of a given type.
     */
    @SuppressWarnings("unchecked")
    protected <T> ArrayList<T> getComponentsByIface(Class<T> type) {
        updateComponentsList();
        return (ArrayList<T>) mInterfaceComponents.computeIfAbsent(type, CompList::new).getList();
    }

    /**
     * Get a list of all components that aren't awake yet.
     *
     * @return An ArrayList containing all of the non awake components
     */
    protected ArrayList<Component> getNotAwakeComponents() {
        updateComponentsList();

        if (mNotAwakeComponentsDirty) {

            mNotAwakeComponents.clear();
            mComponents.stream()
                    .filter(component -> component.getGameObject() != null)
                    .filter(component -> !component.isAwake())
                    .collect(Collectors.toCollection(() -> mNotAwakeComponents));

            mNotAwakeComponentsDirty = false;
        }

        return mNotAwakeComponents;
    }

    /** Dirty the list of components that need to be started. */
    void dirtyToStartComponents() {
        mToStartComponentsDirty = true;
    }

    /**
     * Get a list of all components that are enabled but have not been started yet.
     *
     * @return An ArrayList containing all of the enabled but not started components
     */
    protected ArrayList<Component> getEnabledButNotStartedComponents() {
        updateComponentsList();

        if (mToStartComponentsDirty) {
            mToStartComponents.clear();
            mComponents.stream()
                    .filter(component -> component.getGameObject() != null)
                    .filter(component -> component.getGameObject().isEnabled())
                    .filter(component -> component.isAwake())
                    .filter(component -> !component.isStarted())
                    .collect(Collectors.toCollection(() -> mToStartComponents));
            mToStartComponentsDirty = false;
        }

        return mToStartComponents;
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
