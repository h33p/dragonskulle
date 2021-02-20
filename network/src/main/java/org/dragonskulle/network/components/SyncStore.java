package org.dragonskulle.network.components;

import org.dragonskulle.network.ClientInstance;

import java.util.ArrayList;

public class SyncStore {
    ArrayList<AccommodatingSyncVar> syncVarArrayList = new ArrayList<>();

    public boolean addSyncVar(ClientInstance owner, ISyncVar syncable) {
        AccommodatingSyncVar syncVar = new AccommodatingSyncVar(owner, syncable);
        if (this.syncVarArrayList.contains(syncVar)) {
            return false;
        }
        this.syncVarArrayList.add(syncVar);
        return true;
    }
}


class AccommodatingSyncVar {
    final ClientInstance owner;
    ISyncVar var;

    AccommodatingSyncVar(ClientInstance owner, ISyncVar var) {
        this.owner = owner;
        this.var = var;
    }

    public boolean updateSyncVar(ISyncVar update) {
        try {
            this.var = update;
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}