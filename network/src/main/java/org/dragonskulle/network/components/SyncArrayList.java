package org.dragonskulle.network.components;


import java.util.ArrayList;

public class SyncArrayList<T> extends AbstractSync<ArrayList<T>> {

    public SyncArrayList(String id, ArrayList<T> data, NetworkObject netObject) {
        super(id, data, netObject);
    }

    public SyncArrayList(String id, ArrayList<T> data) {
        super(id, data);
    }

    public SyncArrayList(ArrayList<T> data, NetworkObject netObject) {
        super(data, netObject);
    }

    public SyncArrayList(ArrayList<T> data) {
        super(data);
    }
}
