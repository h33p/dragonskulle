/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.io.InputStream;
import java.util.*;
import java.util.HashMap;
import lombok.Getter;

/**
 * Shared resource manager
 *
 * @author Aurimas Blažulionis
 *     <p>This class allows loading and caching of various resources (files, placed under
 *     main/resources directory). This allows for better memory usage, and easier resource
 *     accessibility. Underlying objects are stored already parsed and loaded to their respective
 *     types.
 *     <p>example declaring a custom resource:
 *     <pre>{@code
 * public static Resource<ShaderBuf> getResource(String name) {
 *     return ResourceManager.getResource(
 *         ShaderBuf.class,
 *         (buf) -> {
 *             ShaderResource ret = new ShaderResource();
 *             ret.buffer = buf;
 *             return ret;
 *         }
 *     )
 * }
 * }</pre>
 */
public class ResourceManager {
    private static final ClassLoader CLASS_LOADER = ResourceManager.class.getClassLoader();
    private static HashMap<String, CountedResource<?>> loadedResources =
            new HashMap<String, CountedResource<?>>();

    /** Reference counts the accesses */
    static class CountedResource<T> {
        @Getter private String name;
        @Getter T resource;
        private int refcount;
        private boolean linked;

        CountedResource(String name, T resource) {
            this.name = name;
            this.resource = resource;
            refcount = 0;
            linked = true;
        }

        /** Decrease reference count. Potentially free and unlink the resource */
        public void decrRefCount() {
            if (--refcount == 0) {
                if (AutoCloseable.class.isInstance(resource)) {
                    try {
                        ((AutoCloseable) resource).close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                resource = null;
                if (linked) ResourceManager.unlinkResource(name);
            }

            if (refcount < 0) {
                throw new RuntimeException("Failed to do this!");
            }
        }

        /**
         * Increase reference count, and return a Resource instance
         *
         * @param type class of the type. Should really be {@code Class<T>}.
         * @return a resource with reference to underlying resource.
         */
        @SuppressWarnings("unchecked")
        private <F> Resource<F> incRefCount(Class<F> type) {
            // We are checking if T == F
            if (type.isInstance(resource)) {
                refcount += 1;
                return new Resource<F>((CountedResource<F>) this);
            } else return null;
        }

        /**
         * Increases reference count, and returns a clone Resource instance
         *
         * @retrun a resource with reference to underlying resource.
         */
        public Resource<T> incRefCount() {
            refcount += 1;
            return new Resource<T>(this);
        }

        /**
         * Try reloading the underlying resource object
         *
         * @param loader loader to reload the resource with.
         * @return {@code true} if reload was successful. On false, the underlying object is left
         *     unchanged.
         */
        public boolean reload(IResourceLoader<T> loader) {
            T res = ResourceManager.loadResource(loader, name);
            if (res == null) return false;
            resource = res;
            return true;
        }
    }

    /**
     * Get a resource object by name and class type
     *
     * <p>This method returns a resource, cached, or newly loaded from `loader`, if nothing was
     * cached.
     *
     * @param type class of {@code T}. Usually {@code T.class}.
     * @param loader object that maps {@code byte[]} to {@code T}. Can be a simple lambda.
     * @param name resource path name.
     * @return loaded resource object, if it succeeded to load, {@code null} otherwise. In addition,
     *     {@code name} is returned if the object type does not match the input {@code name}
     */
    public static <T> Resource<T> getResource(
            Class<T> type, IResourceLoader<T> loader, String name) {
        CountedResource<?> inst = loadedResources.get(name);

        if (inst == null) return loadAndCacheResource(type, loader, name);
        else return inst.incRefCount(type);
    }

    /**
     * Unlinks a resource from internal cache
     *
     * <p>Use this method if you want to preemptively remove a resource from cache. Useful when
     * reloading is needed, but active references should not be mutated.
     *
     * @param name name of the resource to unlink.
     */
    public static void unlinkResource(String name) {
        CountedResource<?> res = loadedResources.remove(name);
        if (res != null) res.linked = false;
    }

    /**
     * Loads a resource by name
     *
     * <p>This method loads a resource from file, and simply returns it. No caching occurs.
     *
     * @param loader mapper from {@code byte[]} to {@code T}. Can be a lambda.
     * @return loaded object object, or {@code null}, if there was an error.
     */
    public static <T> T loadResource(IResourceLoader<T> loader, String name) {
        try (InputStream inputStream = CLASS_LOADER.getResourceAsStream(name)) {
            byte[] buffer = readAllBytes(inputStream);
            return loader.loadFromBuffer(buffer);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Loads and caches a resource
     *
     * <p>This method simply loads a resource, and caches it in the internal map
     */
    private static <T> Resource<T> loadAndCacheResource(
            Class<T> type, IResourceLoader<T> loader, String name) {
        T ret = loadResource(loader, name);
        if (ret == null) return null;
        CountedResource<T> inst = new CountedResource<T>(name, ret);
        loadedResources.put(name, inst);
        return inst.incRefCount(type);
    }

    /** Essentially Java 9 readAllBytes */
    private static byte[] readAllBytes(InputStream stream) throws Exception {
        List<byte[]> chunks = new ArrayList<byte[]>();
        int n = 0;
        int total = 0;
        final int CHUNK_SIZE = 4096;
        while (n >= 0) {
            byte[] chunk = new byte[CHUNK_SIZE];
            n = stream.read(chunk);
            if (n > 0) {
                total += n;
                if (n == CHUNK_SIZE) chunks.add(chunk);
                else chunks.add(Arrays.copyOfRange(chunk, 0, n));
            }
        }

        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, result, offset, c.length);
            offset += c.length;
        }
        return result;
    }
}
