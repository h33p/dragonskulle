/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.network.INetworkUpdate;
import org.dragonskulle.network.DisposingMethod;

/** @author Aurimas Blažulionis */
@Accessors(prefix = "m")
@Log
public class ServerNetworkManager extends NetworkManager {
    public INetworkUpdate mNetworkUpdate;
	private DisposingMethod mDisposingMethod;

    public ServerNetworkManager(INetworkUpdate networkUpdate, DisposingMethod disposingMethod) {
        mNetworkUpdate = networkUpdate;
		mDisposingMethod = disposingMethod;
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
