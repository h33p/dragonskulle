/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * Interface that allows to set request invokation data free of allocations.
 *
 * @author Aurimas Blažulionis
 */
public interface IInvokationSetter<T extends INetSerializable> {
    /**
     * Set values of data.
     *
     * @param data data to set the values of.
     */
    void setValues(T data);
}
