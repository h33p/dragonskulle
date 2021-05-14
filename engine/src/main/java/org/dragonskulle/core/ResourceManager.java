/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Shared resource manager.
 *
 * @author Aurimas Blažulionis
 *     <p>This class allows loading and caching of various resources (files, placed under
 *     main/resources directory). This allows for better memory usage, and easier resource
 *     accessibility. Underlying objects are stored already parsed and loaded to their respective
 *     types.
 *     <p>example declaring a custom resource:
 *     <pre>{@code
 * static {
 *     ResourceManager.registerResource(GLTF.class, (a) -> "gltf/" + a + ".gltf", (b, __) -> new GLTF(b));
 * }
 *
 * public static Resource<GLTF> getResource(String name) {
 *     return ResourceManager.getResource(
 *         GLTF.class,
 *         name
 *      );
 * }
 * }</pre>
 */
public class ResourceManager {
    private static final ClassLoader CLASS_LOADER = ResourceManager.class.getClassLoader();
    private static HashMap<ResourceArguments<?, ?>, CountedResource<?>> sLoadedResources =
            new HashMap<>();
    private static HashMap<Class<?>, IResourceLoader<?, ?>> sLoaders = new HashMap<>();

    static {
        registerResource(byte[].class, (a) -> a.getName(), (b, __) -> b);
        registerResource(String.class, (a) -> a.getName(), (b, __) -> new String(b));
    }

    /** Reference counts the accesses. */
    @Accessors(prefix = "m")
    static class CountedResource<T> {
        @Getter private ResourceArguments<T, ?> mArgs;
        @Getter T mResource;
        private int mRefcount;
        private boolean mLinked;

        /**
         * Create a counted resource.
         *
         * @param args original arguments for the resource.
         * @param resource loaded resource itself.
         */
        CountedResource(ResourceArguments<T, ?> args, T resource) {
            this.mArgs = args;
            this.mResource = resource;
            mRefcount = 0;
            mLinked = true;
        }

        /** Decrease reference count. Potentially free and unlink the resource */
        public void decrRefCount() {
            if (--mRefcount == 0) {
                if (AutoCloseable.class.isInstance(mResource)) {
                    try {
                        ((AutoCloseable) mResource).close();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                mResource = null;
                if (mLinked) {
                    ResourceManager.unlinkResource(mArgs);
                }
            }

            if (mRefcount < 0) {
                throw new RuntimeException("Failed to do this!");
            }
        }

        /**
         * Increase reference count, and return a Resource instance.
         *
         * @param <F> target cast type of the resource.
         * @param type class of the type. Should really be {@code Class<T>}.
         * @return a resource with reference to underlying resource.
         */
        @SuppressWarnings("unchecked")
        private <F> Resource<F> incRefCount(Class<F> type) {
            // We are checking if T == F
            if (type.isInstance(mResource)) {
                mRefcount += 1;
                return new Resource<F>((CountedResource<F>) this);
            } else {
                return null;
            }
        }

        /**
         * Increases reference count, and returns a clone Resource instance.
         *
         * @return a resource with reference to underlying resource.
         */
        public Resource<T> incRefCount() {
            mRefcount += 1;
            return new Resource<T>(this);
        }

        /**
         * Try reloading the underlying resource object.
         *
         * @return {@code true} if reload was successful. On false, the underlying object is left
         *     unchanged.
         */
        public boolean reload() {
            T res = ResourceManager.loadResource(mArgs);
            if (res == null) {
                return false;
            }
            if (AutoCloseable.class.isInstance(mResource)) {
                try {
                    ((AutoCloseable) mResource).close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            mResource = res;
            return true;
        }
    }

    /** Simple composed {@link IResourceLoader}. */
    private static class CompositeResourceLoader<T, F> implements IResourceLoader<T, F> {
        private final IResourcePathResolver<T, F> mPathResolver;
        private final IResourceBufferLoader<T, F> mBufferLoader;

        /**
         * Create a {@link CompositeResourceLoader}.
         *
         * @param pathResolver path resolver part of the loader.
         * @param bufferLoader loading part of the loader.
         */
        CompositeResourceLoader(
                IResourcePathResolver<T, F> pathResolver,
                IResourceBufferLoader<T, F> bufferLoader) {
            mPathResolver = pathResolver;
            mBufferLoader = bufferLoader;
        }

        @Override
        public String toPath(ResourceArguments<T, F> args) {
            return mPathResolver.toPath(args);
        }

        @Override
        public T loadFromBuffer(byte[] buffer, ResourceArguments<T, F> args) throws Throwable {
            return mBufferLoader.loadFromBuffer(buffer, args);
        }
    }

    /**
     * Register a resource loader in a composite way.
     *
     * @param <T> type of the resource.
     * @param <F> type of the resource arguments.
     * @param type type of the resource
     * @param pathResolver implementation (lambda) of path resolving
     * @param bufferLoader implementation (lambda) of resource loading
     */
    public static <T, F> void registerResource(
            Class<T> type,
            IResourcePathResolver<T, F> pathResolver,
            IResourceBufferLoader<T, F> bufferLoader) {
        sLoaders.put(type, new CompositeResourceLoader<>(pathResolver, bufferLoader));
    }

    /**
     * Register a resource loader in a composite way
     *
     * <p>This method is purely for convenience to allow to easily specify the {@code F} type, but
     * does exactly the same as the above method.
     *
     * @param <T> type of the resource.
     * @param <F> type of the resource arguments.
     * @param type type of the resource
     * @param argType type of the argument
     * @param pathResolver implementation (lambda) of path resolving
     * @param bufferLoader implementation (lambda) of resource loading
     */
    public static <T, F> void registerResource(
            Class<T> type,
            Class<F> argType,
            IResourcePathResolver<T, F> pathResolver,
            IResourceBufferLoader<T, F> bufferLoader) {
        registerResource(type, pathResolver, bufferLoader);
    }

    /**
     * Register a resource loader.
     *
     * @param <T> type of the resource.
     * @param <F> type of the resource arguments.
     * @param type type of the resource
     * @param loader loader for the resource
     */
    public static <T, F> void registerResource(Class<T> type, IResourceLoader<T, F> loader) {
        sLoaders.put(type, loader);
    }

    /**
     * Get a resource object by name and class type
     *
     * <p>This method returns a resource, cached, or newly loaded from `loader`, if nothing was
     * cached.
     *
     * @param <T> type of the resource.
     * @param <F> type of the resource arguments.
     * @param arguments arguments used for loading
     * @return loaded resource object, if it succeeded to load, {@code null} otherwise. In addition,
     *     {@code null} is returned if the object type does not match the input name
     */
    public static <T, F> Resource<T> getResource(ResourceArguments<T, F> arguments) {
        CountedResource<?> inst = sLoadedResources.get(arguments);

        if (inst == null) {
            return loadAndCacheResource(arguments);
        } else {
            return inst.incRefCount(arguments.getType());
        }
    }

    /**
     * Get a resource object by name and class type
     *
     * <p>This method returns a resource, cached, or newly loaded from `loader`, if nothing was
     * cached.
     *
     * @param <T> type of the resource.
     * @param <F> type of the resource arguments.
     * @param type class of {@code T}. Usually {@code T.class}.
     * @param name name of the resource to load
     * @param additionalArgs additional arguments to load with
     * @return loaded resource object, if it succeeded to load, {@code null} otherwise. In addition,
     *     {@code null} is returned if the object type does not match the input {@code name}
     */
    public static <T, F> Resource<T> getResource(Class<T> type, String name, F additionalArgs) {
        return getResource(new ResourceArguments<>(type, name, additionalArgs));
    }

    /**
     * Get a resource object by name and class type
     *
     * <p>This method returns a resource, cached, or newly loaded from `loader`, if nothing was
     * cached.
     *
     * @param <T> type of the resource.
     * @param type class of {@code T}. Usually {@code T.class}.
     * @param name name of the resource to load.
     * @return loaded resource object, if it succeeded to load, {@code null} otherwise. In addition,
     *     {@code null} is returned if the object type does not match the input {@code name}
     */
    public static <T> Resource<T> getResource(Class<T> type, String name) {
        return getResource(type, name, null);
    }

    /**
     * Unlinks a resource from internal cache
     *
     * <p>Use this method if you want to preemptively remove a resource from cache. Useful when
     * reloading is needed, but active references should not be mutated.
     *
     * @param args full arguments that were used to load the resource with
     */
    public static void unlinkResource(ResourceArguments<?, ?> args) {
        CountedResource<?> res = sLoadedResources.remove(args);
        if (res != null) {
            res.mLinked = false;
        }
    }

    /**
     * Unlinks a resource from internal cache
     *
     * <p>Use this method if you want to preemptively remove a resource from cache. Useful when
     * reloading is needed, but active references should not be mutated.
     *
     * @param type type of the resource
     * @param name name of the resource to unlink.
     */
    public static void unlinkResource(Class<?> type, String name) {
        unlinkResource(new ResourceArguments<>(type, name, null));
    }

    /**
     * Loads a resource by name
     *
     * <p>This method loads a resource from file, and simply returns it. No caching occurs.
     *
     * @param <T> type of the resource.
     * @param <F> type of the resource arguments.
     * @param arguments arguments to load the resource with, including class, name, and custom args.
     * @return loaded object object, or {@code null}, if there was an error.
     */
    @SuppressWarnings("unchecked")
    public static <T, F> T loadResource(ResourceArguments<T, F> arguments) {

        IResourceLoader<?, ?> loader = sLoaders.get(arguments.getType());
        if (loader == null) {
            return null;
        }

        IResourceLoader<T, F> castLoader = (IResourceLoader<T, F>) loader;

        String path = castLoader.toPath(arguments);

        try (InputStream inputStream = CLASS_LOADER.getResourceAsStream(path)) {
            byte[] buffer = readAllBytes(inputStream);
            return castLoader.loadFromBuffer(buffer, arguments);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Loads and caches a resource.
     *
     * <p>This method simply loads a resource, and caches it in the internal map
     *
     * @param <T> type of the resource.
     * @param <F> type of the resource arguments.
     * @param arguments resource arguments.
     * @return loaded resource with increased reference count, or {@code null}, if loading fails.
     */
    private static <T, F> Resource<T> loadAndCacheResource(ResourceArguments<T, F> arguments) {
        T ret = loadResource(arguments);
        if (ret == null) {
            return null;
        }
        CountedResource<T> inst = new CountedResource<T>(arguments, ret);
        sLoadedResources.put(arguments, inst);
        return inst.incRefCount(arguments.getType());
    }

    /**
     * Essentially Java 9 readAllBytes.
     *
     * @param stream stream to read the bytes from.
     * @return all bytes of the stream.
     */
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
                if (n == CHUNK_SIZE) {
                    chunks.add(chunk);
                } else {
                    chunks.add(Arrays.copyOfRange(chunk, 0, n));
                }
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
