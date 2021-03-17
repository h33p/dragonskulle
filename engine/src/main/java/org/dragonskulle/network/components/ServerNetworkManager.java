/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.network.INetworkUpdate;

/** @author Aurimas Blažulionis */
@Accessors(prefix = "m")
@Log
public class ServerNetworkManager extends NetworkManager {
    public INetworkUpdate mNetworkUpdate;

    public ServerNetworkManager(INetworkUpdate networkUpdate) {
        mNetworkUpdate = networkUpdate;
    }

    @Override
    public void networkUpdate() {
        mNetworkUpdate.call();
    }

    @Override
    public boolean isServer() {
        return true;
    }

    protected void joinLobby() {}

    protected void joinGame() {}

    @Override
    protected void onDestroy() {}
}
