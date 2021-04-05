/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

/**
 * Load resources from byte buffers
 *
 * @author Aurimas Blažulionis
 *     <p>This interface is the composite of {@link IResourceBufferLoader} and {@link
 *     IResourcePathResolver}. This is done so that both implementations can be composed from
 *     lambdas. See {@link ResourceManager#registerResource(Class, IResourcePathResolver,
 *     IResourceBufferLoader)}
 */
public interface IResourceLoader<T, F>
        extends IResourceBufferLoader<T, F>, IResourcePathResolver<T, F> {}
