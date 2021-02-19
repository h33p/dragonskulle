package org.dragonskulle.network.components;

import java.util.ArrayList;

public class SyncArrayList<T> extends ISyncVar<ArrayList<T>> {
    public SyncArrayList(ArrayList<T> data) {
        super(data);
    }

    public SyncArrayList(){
        super(new ArrayList<T>());
    }
}
