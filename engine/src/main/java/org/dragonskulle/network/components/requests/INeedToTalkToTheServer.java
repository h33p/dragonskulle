/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import org.dragonskulle.network.components.sync.INetSerializable;

/** @author Oscar L */
public interface INeedToTalkToTheServer<T extends INetSerializable> {
    /** Server-side attack handler. Can also be a lambda */
    void handleEvent(T data);

    /** Invoke a server event */
    void clientInvokeEvent(T data);
}
