package org.dragonskulle.game.lobby;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;

import java.net.InetAddress;
import java.util.ArrayList;

public class Lobby extends Component implements IFixedUpdate {

    private final ArrayList<InetAddress> mHosts = new ArrayList<>();
    private boolean mHostsUpdated;

    private void handleGetAllHosts(String response) {
        // TODO: Process response and separate each host into INetAddress
    }

    @Override
    protected void onDestroy() {

    }
    // TODO: If mHostsUpdated, re-create the UI to add those new hosts as buttons
    // TODO: Also need to handle the creation of new lobbies
    // TODO: Need to start the game after host starts the game

    /*
    Have method for



     */



    @Override
    public void fixedUpdate(float deltaTime) {



    }
}
