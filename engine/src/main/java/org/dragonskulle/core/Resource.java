/* (C) 2021 DragonSkulle */

package org.dragonskulle.core;

import org.lwjgl.system.NativeResource;

/**
 * Retrieve resources and keep them in resource manager while in use.
 *
 * @author Aurimas Blažulionis
 *     <p>As long as there is at least one {@code Resource<T>} reference alive, retrieving
 *     additional resources from ResourceManager should be quick and not involve any additional
 *     reloading. Upon all {@code Resource<T>} references have been freed (manually or garbage
 *     collected), the underlying {@code ResourceInstance} gets unlinked from {@code
 *     ResourceManager}, and thus get the underlying resource freed as well (close/free is also
 *     called, if the type implements {@link AutoCloseable} or {@link NativeResource}).
 *     <p>Note that GC is inconsistent and shouldn't be relied upon for lowest memory usage. Call
 *     {@code free} explicitly, if possible. Alternatively, use the {@code try-with-resources}
 *     syntax:
 *     <pre>{@code
 * try(Resource<ShaderBuf> resource = ShaderBuf.getResource("shaderc/frag.spv")) {
 *     ...
 * }
 * }</pre>
 */
public class Resource<T> implements NativeResource {

    private ResourceManager.CountedResource<T> instance;

    public Resource(ResourceManager.CountedResource<T> i) {
        instance = i;
    }

    /**
     * Get the underlying resource object.
     *
     * @return the underlying {@code T} value. Never {@code null}.
     */
    public T get() {
        return instance != null ? instance.getResource() : null;
    }

    /**
     * Try reloading the underlying resource object.
     *
     * @return {@code true} if reload was successful. On false, the underlying object is left
     *     unchanged.
     */
    public boolean reload() {
        return instance.reload();
    }

    @Override
    public final void free() {
        if (instance != null) {
            instance.decrRefCount();
            instance = null;
        }
    }

    @Override
    public final Resource<T> clone() {
        return instance != null ? instance.incRefCount() : null;
    }

    @SuppressWarnings("unchecked")
    public final <F> Resource<F> cast(Class<F> type) {
        if (type.isInstance(get())) {
            return (Resource<F>) this;
        }
        return null;
    }

    @Override
    public int hashCode() {
        return get().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Resource)) {
            return false;
        }
        Resource<?> res = (Resource<?>) o;
        return get().equals(res.get());
    }
}
