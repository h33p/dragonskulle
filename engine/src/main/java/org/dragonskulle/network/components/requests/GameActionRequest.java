/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import lombok.experimental.Accessors;

/** @author Oscar L */
@Accessors(prefix = "m")
abstract class GameActionRequest {
    public abstract byte[] toBytes();
}
