/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

/** @author Oscar L */
public class NetworkObjectDoesNotHaveChildError extends Exception {
    public NetworkObjectDoesNotHaveChildError(String errorMessage) {
        super(errorMessage);
    }
}
