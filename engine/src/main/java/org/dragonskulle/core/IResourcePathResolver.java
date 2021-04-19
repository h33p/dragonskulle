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
    String toPath(ResourceArguments<T, F> args);
}
