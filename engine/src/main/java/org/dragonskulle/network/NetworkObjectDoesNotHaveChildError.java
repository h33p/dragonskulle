/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

/** @author Oscar L */
public class NetworkObjectDoesNotHaveChildError extends Exception {
    public NetworkObjectDoesNotHaveChildError(String errorMessage, int componentId) {
        super(errorMessage);
        this.invalidComponentId = componentId;
    }

    public final int invalidComponentId;
}
