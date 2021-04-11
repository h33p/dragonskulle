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
    private final Class<T> mType;
    private final String mName;
    private final F mAdditionalArgs;

    public ResourceArguments(Class<T> type, String name, F additionalArgs) {
        mType = type;
        mName = name;
        mAdditionalArgs = additionalArgs;
    }
}
