/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.io.InputStream;
import java.util.HashMap;

public abstract class ResourceManager {
    private static final ClassLoader CLASS_LOADER = ResourceManager.class.getClassLoader();
    private static HashMap<String, Object> loadedResources = new HashMap<String, Object>();

    public static <T> T getResource(Class<T> type, IResourceLoader loader, String name) {
        Object res = loadedResources.get(name);

        if (res == null) res = loadResource(loader, name);

        if (type.isInstance(res)) return type.cast(res);
        else return null;
    }

    public static void unlinkResource(String name) {
        loadedResources.remove(name);
    }

    private static Object loadResource(IResourceLoader loader, String name) {
        try (InputStream inputStream = CLASS_LOADER.getResourceAsStream(name)) {
            byte[] buffer = inputStream.readAllBytes();
            Object ret = loader.loadFromBuffer(buffer);
            loadedResources.put(name, ret);
            return ret;
        } catch (Exception e) {
            return null;
        }
    }
}
