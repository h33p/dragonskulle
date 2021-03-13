/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import org.dragonskulle.network.NetworkMessage;

public class AttackGameActionRequest extends GameActionRequest {
    // This is all temporary, please change it with your own required data and create your own types
    final int mFromBuilding;
    final int mToBuilding;
    final int mFromPlayer;

    public AttackGameActionRequest(int fromBuilding, int toBuilding, int fromPlayer) {
        this.mFromBuilding = fromBuilding;
        this.mToBuilding = toBuilding;
        this.mFromPlayer = fromPlayer;
    }

    @Override
    public byte[] toBytes() {
        return NetworkMessage.build(
                (byte) 100,
                new byte[0]); // should create the correct message to send to the server once
        // defined properly.
        // TODO implement custom serialization
    }
}
