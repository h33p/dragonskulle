/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Load resources from byte buffers.
 *
 * @author Aurimas Blažulionis
 *     <p>This class stores all arguments for a resource - its type, name, and any additional
 *     arguments that its loader may want to use. Essentially, if a resource is retrieved with the
 *     same arguments, it will point to the same resource.
 */
@Getter
@Accessors(prefix = "m")
@EqualsAndHashCode
public class ResourceArguments<T, F> {
    /** Type of the resource. */
    private final Class<T> mType;
    /** Name of the resource. */
    private final String mName;
    /** Additional arguments for the resource loader. */
    private final F mAdditionalArgs;

    /**
     * Construct {@link ResourceArguments}.
     *
     * @param type type of the data (target loader).
     * @param name name of the data.
     * @param additionalArgs additional arguments for the loader.
     */
    public ResourceArguments(Class<T> type, String name, F additionalArgs) {
        mType = type;
        mName = name;
        mAdditionalArgs = additionalArgs;
    }
}
