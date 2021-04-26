/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.lobby;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.UPnP;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UITextRect;
import org.joml.Vector4f;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@Log
@Accessors(prefix = "m")
public class Lobby extends Component implements IFrameUpdate {

    private static final int PORT = 17569;

    private final ArrayList<InetSocketAddress> mHosts = new ArrayList<>();
    private final AtomicBoolean mHostsUpdated = new AtomicBoolean(false);
    @Getter private final GameObject mLobbyUi;
    private final GameObject mHostUi;
    private final GameObject mJoinUi;
    private final GameObject mServerBrowserUi;
    private Reference<GameObject> mServerList;
    private final GameObject mHostingUi;
    private final GameObject mJoiningUi;
    private final Reference<NetworkManager> mNetworkManager;

    private String lobbyID = "";

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
                                    mNetworkManager.get().getServerManager().start();
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
                                                    (manager, netID) -> {
                                                        button.getLabelText()
                                                                .get()
                                                                .setText("Join locally");

                                                        if (netID >= 0) {
                                                            mJoiningUi.setEnabled(true);
                                                            mJoinUi.setEnabled(false);
                                                        }
                                                    },
                                                    this::onHostStartGame);
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
                                                text,
                                                PORT,
                                                (manager, netID) -> {
                                                    button.getLabelText().get().setText(text);

                                                    if (netID >= 0) {
                                                        mJoiningUi.setEnabled(true);
                                                        mServerBrowserUi.setEnabled(false);
                                                    }
                                                },
                                                this::onHostStartGame);
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
                                "Host publicly",
                                (__, ___) -> {
                                    String ip = UPnP.getExternalIPAddress();
                                    LobbyAPI.addNewHost(ip, PORT, this::handleAddNewHost);
                                    mNetworkManager
                                            .get()
                                            .createServer(PORT, this::onClientLoaded, this::onGameStarted);
                                    mHostingUi.setEnabled(true);
                                    mHostUi.setEnabled(false);
                                }),
                        new UIButton(
                                "Host locally",
                                (__, ___) -> {
                                    mNetworkManager
                                            .get()
                                            .createServer(PORT, this::onClientLoaded, this::onGameStarted);
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
        mHosts.clear();
        JSONParser parser = new JSONParser();
        try {
            JSONArray array = (JSONArray)parser.parse(response);

            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = (JSONObject)array.get(i);

                String ip = (String)obj.get("address");
                int port = Math.toIntExact((Long)obj.get("port"));

                log.info("New host: " + ip + ":" + port);
                mHosts.add(new InetSocketAddress(ip, port));
            }

        } catch (ParseException e) {
            log.info("Failed to parse response from get all hosts");
            return;
        }
        mHostsUpdated.set(true);
    }

    private void handleAddNewHost(String response) {
        log.info("Add new host returned:\n\t" + response);
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject)parser.parse(response);
            lobbyID = (String)obj.get("_id");
            log.info(lobbyID);
        } catch (ParseException e) {
            e.printStackTrace();
            log.warning("Failed to parse response from add new host. Likely failed.");
        }
    }

    private void onHostStartGame(Scene gameScene, NetworkManager manager, int netId) {
        log.info("CONNECTED ID " + netId);

        GameObject humanPlayer =
                new GameObject(
                        "human player",
                        (handle) -> {
                            handle.addComponent(
                                    new HumanPlayer(
                                            manager.getReference(NetworkManager.class), netId));
                        });

        gameScene.addRootObject(humanPlayer);
    }

    private void onClientLoaded(
            Scene gameScene, NetworkManager manager, ServerClient networkClient) {
        log.info("Client ID: " + networkClient.getNetworkID() + " loaded.");
        int id = networkClient.getNetworkID();
        manager.getServerManager().spawnNetworkObject(id, manager.findTemplateByName("player"));
    }

    private void onGameStarted(NetworkManager manager) {
        log.severe("Game Start");
        log.warning("Spawning 'Server' Owned objects");
        manager.getServerManager().spawnNetworkObject(-10000, manager.findTemplateByName("map"));
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
