/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Load resources from byte buffers
 *
 * @author Aurimas Blažulionis
 *     <p>This interface is really trivial. A simple lambda that accepts {@code byte[]}, and outputs
 *     {@code T} implements it automatically!
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
