/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.util.*;
import lombok.experimental.Accessors;

/**
 * Spawnable template manager
 *
 * @author Aurimas Blažulionis
 *     <p>This class acts as a collection of spawnable object templates.
 */
@Accessors(prefix = "m")
public class TemplateManager {
    private final List<GameObject> mTemplates = new ArrayList<>();
    private final Map<String, Integer> mNameToIndex = new HashMap<>();

    /**
     * Instantiate a template by ID
     *
     * @param id index of the object in the manager
     * @return a clone of the template
     */
    public GameObject instantiate(int id) {
        return GameObject.instantiate(mTemplates.get(id));
    }

    /**
     * Find template index by its name
     *
     * @param name name of the template
     * @return integer ID of the template. {@code null}, if not found.
     */
    public Integer find(String name) {
        return mNameToIndex.get(name);
    }

    /**
     * Add object to the template manager
     *
     * @param object object to add as template
     * @return integer ID of the template. {@code null}, if name is duplicate.
     */
    public Integer addObject(GameObject object) {
        if (mNameToIndex.containsKey(object.getName())) return null;
        int idx = mTemplates.size();
        mNameToIndex.put(object.getName(), idx);
        mTemplates.add(object);
        return idx;
    }

    public void addAllObjects(GameObject... objects) {
        for (GameObject go : objects) addObject(go);
    }
}
