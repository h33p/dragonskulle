package org.dragonskulle.network.components;

import java.util.Objects;
import java.util.UUID;

public class ISyncVar<T>{
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        ISyncVar<?> iSyncVar = (ISyncVar<?>) o;
        if(getId().equals(iSyncVar.getId())) return true;
        if(getClass() != o.getClass()) return false;
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public NetworkObject getParent() {
        return this.parentNetObject;
    }

    @FunctionalInterface
    public interface Updater {
        void call(String netId, ISyncVar<? extends Object> newValue);
    }

    public Updater getOnUpdate() {
        return onUpdate;
    }

    private void setOnUpdate(Updater onUpdate) {
        this.onUpdate = onUpdate;
    }

    private Updater onUpdate;
    NetworkObject parentNetObject;
    final String id;


    public ISyncVar(String id, T data, NetworkObject netObject) {
        this.data = data;
        this.id = id;
        this.parentNetObject = netObject;
    }

    public ISyncVar(String id, T data) {
        this.data = data;
        this.id = id;
    }


    public ISyncVar(T data, NetworkObject netObject) {
        this.data = data;
        this.id = UUID.randomUUID().toString();
        this.parentNetObject = netObject;
    }

    public ISyncVar(T data) {
        this.data = data;
    }

    public void attachParent(NetworkObject parent){
        this.parentNetObject = parent;
    }

    public T looselyGet() {
        return data;
    }


    void set(T data) {
//        if (onUpdate != null) {
        System.out.println("Calling onupdate");
//        ISyncVar looseSync = looselySet(data);
//        System.out.println("loose sync :: " + looseSync.toString());
//        System.out.println("loose sync type :: " + looseSync.getClass());
        this.notifySyncedOfUpdateRequest((ISyncVar) data);
//            this.data = data;
//        }
    }


    public ISyncVar<T> firmlySet(T data){
        //when server notifies of update
        this.data = data;
        System.out.println("firmly set data");
//        return new ISyncVar<T>(this.id, data);
        return this;
    }

    private T data;

    byte[] serialize() {
        return null;
    }

    private void notifySyncedOfUpdateRequest(ISyncVar newVar) {
        System.out.println("notifySyncedOfUpdateRequest");
        this.parentNetObject.notifySyncedOfUpdate(newVar);
    }

    public void runUpdateCallback(String netId, ISyncVar<? extends Object> newData) {
        this.onUpdate.call(netId, newData);
    }

    public void attachUpdateListener(Updater callback) {
        System.out.println("Attaching update listener for sync var");
        this.setOnUpdate(callback);
    }

}
