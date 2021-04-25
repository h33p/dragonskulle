/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.lobby;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UITextRect;
import org.joml.Vector4f;

@Log
public class Lobby extends Component implements IFrameUpdate {

    private final ArrayList<InetSocketAddress> mHosts = new ArrayList<>();
    private final AtomicBoolean mHostsUpdated = new AtomicBoolean(false);
    @Getter private final GameObject mLobbyUi;
    @Getter private final GameObject mHostUi;
    @Getter private final GameObject mJoinUi;
    @Getter private final GameObject mServerBrowserUi;
    private Reference<GameObject> mServerList;
    @Getter private final GameObject mHostingUi;
    @Getter private final GameObject mJoiningUi;
    private final Reference<NetworkManager> mNetworkManager;

    public Lobby(Reference<GameObject> mainUi, Reference<NetworkManager> networkManager) {
        mNetworkManager = networkManager;

        mLobbyUi =
                new GameObject(
                        "lobbyUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        mHostUi =
                new GameObject(
                        "hostUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        mJoinUi =
                new GameObject(
                        "joinUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        mServerList = new GameObject("servers", false, new TransformUI(false)).getReference();

        mServerBrowserUi =
                new GameObject(
                        "serverBrowserUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                            root.addChild(mServerList.get());
                        });

        mHostingUi =
                new GameObject(
                        "hostingUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        mJoiningUi =
                new GameObject(
                        // TODO: Need a way to leave a lobby
                        "joiningUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                            root.addComponent(
                                    new UITextRect(
                                            "Waiting for host to start game!",
                                            new Vector4f(1f, 1f, 1f, 1f)));
                        });

        UIManager.getInstance()
                .buildVerticalUi(
                        mLobbyUi,
                        0.05f,
                        0,
                        0.2f,
                        new UIButton(
                                "Join Game",
                                (__, ___) -> {
                                    mJoinUi.setEnabled(true);
                                    mLobbyUi.setEnabled(false);
                                }),
                        new UIButton(
                                "Host Game",
                                (__, ___) -> {
                                    mHostUi.setEnabled(true);
                                    mLobbyUi.setEnabled(false);
                                }),
                        new UIButton(
                                "Cancel",
                                (__, ___) -> {
                                    mainUi.get().setEnabled(true);
                                    mLobbyUi.setEnabled(false);
                                }));

        UIManager.getInstance()
                .buildVerticalUi(
                        mHostingUi,
                        0.05f,
                        0,
                        0.2f,
                        new UIButton(
                                // TODO: When we start a game remove it from the server list
                                "Start Game",
                                (__, ___) -> {
                                    mNetworkManager.get().getServerManager().startGame();
                                }));

        buildJoinUi();
        buildHostUi();
        buildServerList();
        LobbyAPI.getAllHosts(this::handleGetAllHosts);
    }

    private void buildJoinUi() {
        UIManager.getInstance()
                .buildVerticalUi(
                        mJoinUi,
                        0.05f,
                        0,
                        0.2f,
                        new UIButton(
                                "Join remote lobby",
                                (__, ___) -> {
                                    mServerBrowserUi.setEnabled(true);
                                    mJoinUi.setEnabled(false);
                                }),
                        new UIButton(
                                "Join locally",
                                (button, __) -> {
                                    button.getLabelText().get().setText("Connecting...");
                                    mNetworkManager
                                            .get()
                                            .createClient(
                                                    "127.0.0.1",
                                                    7000,
                                                    (scene, manager, netID) -> {
                                                        onConnectToHost(scene, manager, netID);
                                                        button.getLabelText()
                                                                .get()
                                                                .setText("Join locally");

                                                        if (netID >= 0) {
                                                            mJoiningUi.setEnabled(true);
                                                            mJoinUi.setEnabled(false);
                                                        }
                                                    });
                                }),
                        new UIButton(
                                "Cancel",
                                (__, ___) -> {
                                    mJoinUi.setEnabled(false);
                                    mLobbyUi.setEnabled(true);
                                }));
    }

    private void buildServerList() {
        boolean enabled = mServerList.get().isEnabled();
        mServerList.get().destroy();
        mServerList = new GameObject("servers", enabled, new TransformUI(false)).getReference();

        final GameObject serverList = mServerList.get();

        UIButton[] buttons = new UIButton[mHosts.size() + 1];
        buttons[0] =
                new UIButton(
                        "Refresh list",
                        (button, ___) -> {
                            button.getLabelText().get().setText("Refreshing...");
                            LobbyAPI.getAllHosts(this::handleGetAllHosts);
                        });

        // TODO: Handle connection failures
        //      Either change the handler to

        for (int i = 0; i < mHosts.size(); i++) {
            final String text = mHosts.get(i).getHostString();
            buttons[i + 1] =
                    new UIButton(
                            text,
                            (button, ___) -> {
                                button.getLabelText().get().setText("Connecting...");
                                mNetworkManager
                                        .get()
                                        .createClient(
                                                "127.0.0.1",
                                                7000,
                                                (scene, manager, netID) -> {
                                                    onConnectToHost(scene, manager, netID);
                                                    button.getLabelText().get().setText(text);

                                                    if (netID >= 0) {
                                                        mJoiningUi.setEnabled(true);
                                                        mServerBrowserUi.setEnabled(false);
                                                    }
                                                });
                            });
        }

        UIManager.getInstance().buildVerticalUi(serverList, 0.05f, 0, 0.2f, buttons);

        mServerBrowserUi.addChild(mServerList.get());
    }

    private void buildHostUi() {
        UIManager.getInstance()
                .buildVerticalUi(
                        mHostUi,
                        0.05f,
                        0,
                        0.2f,
                        new UIButton(
                                // TODO: Online
                                // TODO: Get public ip
                                // TODO: Do auto port-forwarding with UPnP
                                // TODO: Use lobbyAPI to add lobby to list
                                ),
                        new UIButton(
                                "Host locally",
                                (__, ___) -> {
                                    mNetworkManager
                                            .get()
                                            .createServer(7000, this::onClientConnected);
                                    mHostingUi.setEnabled(true);
                                    mHostUi.setEnabled(false);
                                }),
                        new UIButton(
                                "Cancel",
                                (__, ___) -> {
                                    mHostUi.setEnabled(false);
                                    mLobbyUi.setEnabled(true);
                                }));
    }

    public void addUiToScene(Scene mainMenu) {
        mainMenu.addRootObject(mLobbyUi);
        mainMenu.addRootObject(mJoinUi);
        mainMenu.addRootObject(mServerBrowserUi);
        mainMenu.addRootObject(mJoiningUi);
        mainMenu.addRootObject(mHostUi);
        mainMenu.addRootObject(mHostingUi);
    }

    private void handleGetAllHosts(String response) {
        // TODO: Process response and separate each host into INetAddress
        log.info("Get all hosts returned:\n\t" + response);
        mHosts.add(new InetSocketAddress("127.0.0.1", 7000));
        mHostsUpdated.set(true);
    }

    private void onClientConnected(
            Scene gameScene, NetworkManager manager, ServerClient networkClient) {
        int id = networkClient.getNetworkID();
        log.info("New player connected to lobby with id " + id);
        // TODO: Track number of connected clients in the lobby
    }

    private void onConnectToHost(Scene gameScene, NetworkManager manager, int netID) {
        if (netID >= 0) {
            GameObject humanPlayer =
                    new GameObject(
                            "human player",
                            (handle) -> {
                                handle.addComponent(
                                        new HumanPlayer(
                                                manager.getReference(NetworkManager.class), netID));
                            });

            gameScene.addRootObject(humanPlayer);
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mHostsUpdated.compareAndSet(true, false)) {
            buildServerList();
        }
    }

    @Override
    protected void onDestroy() {}
}
