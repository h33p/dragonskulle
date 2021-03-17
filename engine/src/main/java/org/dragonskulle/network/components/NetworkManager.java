/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.io.IOException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;

/**
 * @author Aurimas Blažulionis
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class NetworkManager extends Component implements IFixedUpdate {

    /** Simple client connection result handler */
    public static interface IConnectionResultHandler {
        void handle(boolean success);
    }

    /** Simple server client connection handler interface. */
    public static interface IConnectedClientHandler {
        void handle(NetworkManager manager, int id);
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
    public void fixedUpdate(float deltaTime) {
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
    public void createClient(String ip, int port, IConnectionResultHandler resultHandler) {
        if (mClientManager == null && mServerManager == null)
            mClientManager = new ClientNetworkManager(this, ip, port, resultHandler);
    }

    /**
     * Create a network server
     *
     * @param port network port to bind
     * @param connectionHandler callback that gets called on every client connection
     */
    public void createServer(int port, IConnectedClientHandler connectionHandler) {
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
