/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.Accessors;

/**
 * Spawnable template manager.
 *
 * @author Aurimas Blažulionis
 *     <p>This class acts as a collection of spawnable object templates.
 */
@Accessors(prefix = "m")
public class TemplateManager {
    private final List<GameObject> mTemplates = new ArrayList<>();
    private final Map<String, Integer> mNameToIndex = new HashMap<>();

    /**
     * Instantiate a template by ID.
     *
     * @param id index of the object in the manager
     * @return a clone of the template. {@code null} if invalid ID was passed.
     */
    public GameObject instantiate(int id) {

        if (id < 0 || id >= mTemplates.size()) {
            return null;
        }

        return GameObject.instantiate(mTemplates.get(id));
    }

    /**
     * Find template index by its name.
     *
     * @param name name of the template
     * @return integer ID of the template. {@code null}, if not found.
     */
    public Integer find(String name) {
        return mNameToIndex.get(name);
    }

    /**
     * Add object to the template manager.
     *
     * @param object object to add as template
     * @return integer ID of the template. {@code null}, if name is duplicate.
     */
    public Integer addObject(GameObject object) {
        if (mNameToIndex.containsKey(object.getName())) {
            return null;
        }
        int idx = mTemplates.size();
        mNameToIndex.put(object.getName(), idx);
        mTemplates.add(object);
        return idx;
    }

    /**
     * Add all objects to the template manager.
     *
     * <p>This method will not return the IDs of the objects. It will be necessary to look them up
     * by name instead.
     *
     * @param objects objects to add.
     */
    public void addAllObjects(GameObject... objects) {
        for (GameObject go : objects) {
            addObject(go);
        }
    }
}
