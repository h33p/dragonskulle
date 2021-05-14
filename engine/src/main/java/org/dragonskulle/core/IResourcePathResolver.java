/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

/**
 * Maps resource arguments to file paths.
 *
 * @author Aurimas Blažulionis
 *     <p>This interface is really trivial. A simple lambda that accepts {@code
 *     ResourceArguments<T>}, and outputs {@code String} implements it automatically!
 */
public interface IResourcePathResolver<T, F> {
    /**
     * Get the path from arguments.
     *
     * @param args arguments of the resource.
     * @return relative path to the file of the resource.
     */
    String toPath(ResourceArguments<T, F> args);
}
