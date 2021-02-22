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

    public boolean updateVar(ISyncVar var) {
        for (int i = 0; i < this.syncVarArrayList.size(); i++) {
            AccommodatingSyncVar accommodated = this.syncVarArrayList.get(i);
            if (accommodated.var.equals(var)) {
                AccommodatingSyncVar newAccomodate = accommodated.updateSyncVar(var);
                if (newAccomodate != null) {
                    this.syncVarArrayList.set(i, newAccomodate);
                    return true;
                }
                return false;
            }
        }
        return false;
    }
}


class AccommodatingSyncVar {
    final ClientInstance owner;
    ISyncVar var;

    AccommodatingSyncVar(ClientInstance owner, ISyncVar var) {
        this.owner = owner;
        this.var = var;
    }

    public AccommodatingSyncVar updateSyncVar(ISyncVar update) {
        try {
            this.var = update;
            return this;
        } catch (Exception e) {
            return null;
        }
    }
}