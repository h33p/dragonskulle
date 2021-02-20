package org.dragonskulle.network.components;

import java.util.ArrayList;

public class SyncBool extends ISyncVar<Boolean>{
    public SyncBool(Boolean data) {
        super(data);
    }
    public SyncBool(String id, Boolean data) {
        super(id,data);
    }

    public SyncBool() {
        super(null);
    }
};
