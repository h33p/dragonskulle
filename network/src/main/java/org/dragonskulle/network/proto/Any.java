/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.proto;

public final class Any {
    private Any() {}

    public static final byte NONE = 0;
    public static final byte CreateCityRequest = 1;
    public static final byte NotifyCityCreated = 2;

    public static final String[] names = {
        "NONE", "CreateCityRequest", "NotifyCityCreated",
    };

    public static String name(int e) {
        return names[e];
    }
}
