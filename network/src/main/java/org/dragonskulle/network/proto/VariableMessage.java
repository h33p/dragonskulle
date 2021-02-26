/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

public final class VariableMessage {
    private VariableMessage() {}

    public static final byte NONE = 0;
    public static final byte RegisterSyncVarsRequest = 1;
    public static final byte RegisteredSyncVarsResponse = 2;
    public static final byte UpdateSyncVarsRequest = 3;
    public static final byte UpdateSyncVarsResponse = 4;

    public static final String[] names = {
        "NONE",
        "RegisterSyncVarsRequest",
        "RegisteredSyncVarsResponse",
        "UpdateSyncVarsRequest",
        "UpdateSyncVarsResponse",
    };

    public static String name(int e) {
        return names[e];
    }
}
