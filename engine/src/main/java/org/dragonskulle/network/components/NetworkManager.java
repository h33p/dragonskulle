/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.IOException;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.INetworkUpdate;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.network.ServerClient;

/**
 * @author Aurimas Blažulionis
 * @author Oscar L
 */
@Accessors(prefix = "m")
@Log
public class NetworkManager extends Component implements INetworkUpdate {

    /** Simple client connection result handler */
    public static interface IConnectionResultEvent {
        void handle(NetworkManager manager, int netID);
    }

    /** Simple server client connection handler interface. */
    public static interface IConnectedClientEvent {
        void handle(NetworkManager manager, ServerClient client);
    }

    /** A registerable listener for when objects are spawned */
    public static interface IObjectSpawnEvent {
        void handleSpawn(NetworkObject object);
    }

    /** A registerable listener for when objects change owner */
    public static interface IObjectOwnerModifiedEvent {
        void handleModifyOwner(Reference<NetworkObject> object);
    }

    /** Registered spawnable templates */
    @Getter(AccessLevel.PACKAGE)
    protected final TemplateManager mSpawnableTemplates;
    /** Target game scene */
    @Getter(AccessLevel.PACKAGE)
    private final Scene mGameScene;
    /** Client manager. Exists when there is a client connection */
    @Getter private transient ClientNetworkManager mClientManager;
    /** Server manager. Exists when there is a server instance */
    @Getter private transient ServerNetworkManager mServerManager;

    public NetworkManager(TemplateManager templates, Scene gameScene) {
        mSpawnableTemplates = templates;
        mGameScene = gameScene;
    }

    @Override
    public void networkUpdate() {
        Scene.getActiveScene().registerSingleton(this);
        mGameScene.registerSingleton(this);

        if (mServerManager != null) mServerManager.networkUpdate();
        else if (mClientManager != null) mClientManager.networkUpdate();
    }

    /**
     * Create a network client
     *
     * @param ip IP address to connect to
     * @param port network port to connect to
     * @param resultHandler connection result callback
     */
    public void createClient(String ip, int port, IConnectionResultEvent resultHandler) {
        if (mClientManager == null && mServerManager == null) {
            mClientManager = new ClientNetworkManager(this, ip, port, resultHandler);
        }
    }

    /**
     * Create a network server
     *
     * @param port network port to bind
     * @param connectionHandler callback that gets called on every client connection
     */
    public void createServer(int port, IConnectedClientEvent connectionHandler) {
        if (mClientManager == null && mServerManager == null) {
            try {
                mServerManager = new ServerNetworkManager(this, port, connectionHandler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Find template index by name
     *
     * @param name target name of the template
     * @return {@code null} if not found, integer ID otherwise
     */
    public Integer findTemplateByName(String name) {
        return mSpawnableTemplates.find(name);
    }

    /**
     * Check whether we are running as server
     *
     * @return {@code true} if we are running as server, {@code false} otherwise
     */
    public boolean isServer() {
        return mServerManager != null;
    }

    /**
     * Check whether we are running as client
     *
     * @return {@code true} if we are running as client, {@code false} otherwise
     */
    public boolean isClient() {
        return mClientManager != null;
    }

    public Stream<NetworkObject> getObjectsOwnedBy(int netId) {
        Stream<Reference<NetworkObject>> obj = null;

        if (mServerManager != null)
            obj =
                    mServerManager.getNetworkObjects().values().stream()
                            .map(e -> e.getNetworkObject());
        else if (mClientManager != null) obj = mClientManager.getNetworkObjects();

        return obj.filter(Reference::isValid)
                .map(Reference::get)
                .filter(o -> o.getOwnerId() == netId);
    }

    /** Called whenever client disconnects */
    void onClientDisconnect() {
        mClientManager = null;
    }

    /** Called whenever server is destroyed */
    void onServerDestroy() {
        mServerManager = null;
    }

    @Override
    protected void onDestroy() {
        if (mServerManager != null) mServerManager.destroy();
        if (mClientManager != null) mClientManager.disconnect();
    }
}
