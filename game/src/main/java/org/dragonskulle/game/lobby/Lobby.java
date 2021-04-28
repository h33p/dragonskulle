/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.lobby;

import java.util.HashMap;
import java.util.Map;
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
import org.dragonskulle.ui.UIInputBox;
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

    private final Map<String, String> mHosts = new HashMap<>();
    private final AtomicBoolean mHostsUpdated = new AtomicBoolean(false);
    private final AtomicBoolean mLobbyIDUpdated = new AtomicBoolean(false);
    @Getter private final GameObject mLobbyUi;
    private final GameObject mHostUi;
    private final GameObject mJoinUi;
    private final GameObject mServerBrowserUi;
    private Reference<GameObject> mServerList;
    private final GameObject mHostingUi;
    private final GameObject mJoiningUi;
    private final Reference<NetworkManager> mNetworkManager;
    private String mLobbyId = "";
    private final UIInputBox mLobbyIDText;

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

        mLobbyIDText = new UIInputBox("ID: ");

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
                                "Back",
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
                                "Start Game",
                                (__, ___) -> {
                                    mNetworkManager.get().getServerManager().start();
                                    LobbyAPI.deleteHost(mLobbyId, this::onDeleteHost);
                                }),
                        mLobbyIDText,
                        new UIButton(
                                "Close lobby",
                                (__, ___) -> {
                                    if (Reference.isValid(networkManager)) {
                                        final NetworkManager manager = networkManager.get();
                                        if (manager.getServerManager() != null) {
                                            manager.getServerManager().destroy();
                                        }
                                    }
                                    if (!mLobbyId.equals("")) {
                                        LobbyAPI.deleteHost(mLobbyId, this::onDeleteHost);
                                    }
                                    mHostUi.setEnabled(true);
                                    mHostingUi.setEnabled(false);
                                }));

        UIManager.getInstance()
                .buildVerticalUi(
                        mJoiningUi,
                        0.05f,
                        0,
                        0.2f,
                        new UIButton(
                                "Leave lobby",
                                (__, ___) -> {
                                    if (Reference.isValid(networkManager)) {
                                        final NetworkManager manager = networkManager.get();
                                        if (manager.getClientManager() != null) {
                                            manager.getClientManager().disconnect();
                                        }
                                    }
                                    mJoinUi.setEnabled(true);
                                    mJoiningUi.setEnabled(false);
                                }));

        buildJoinUi();
        buildHostUi();
        buildServerList();
        LobbyAPI.getAllHosts(this::onGetAllHosts);
    }

    private void buildJoinUi() {
        UIManager.getInstance()
                .buildVerticalUi(
                        mJoinUi,
                        0.05f,
                        0,
                        0.2f,
                        new UIButton(
                                "Join public lobby",
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
                                                    PORT,
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
                                "Back",
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

        UIManager.IUIBuildHandler[] uiElements = new UIButton[mHosts.size() + 2];

        int i = 1;
        for (Map.Entry<String, String> entry : mHosts.entrySet()) {
            final String id = entry.getKey().substring(0, 10);
            final String ip = entry.getValue();
            uiElements[i] =
                    new UIButton(
                            id,
                            (button, ___) -> {
                                button.getLabelText().get().setText("Connecting...");
                                mNetworkManager
                                        .get()
                                        .createClient(
                                                ip,
                                                PORT,
                                                (manager, netID) -> {
                                                    button.getLabelText().get().setText(id);

                                                    if (netID >= 0) {
                                                        mJoiningUi.setEnabled(true);
                                                        mServerBrowserUi.setEnabled(false);
                                                    } else {
                                                        LobbyAPI.getAllHosts(this::onGetAllHosts);
                                                    }
                                                },
                                                this::onHostStartGame);
                            });
            i++;
        }

        uiElements[0] =
                new UIButton(
                        "Refresh",
                        (button, ___) -> {
                            button.getLabelText().get().setText("Refreshing...");
                            LobbyAPI.getAllHosts(this::onGetAllHosts);
                        });

        uiElements[uiElements.length - 1] =
                new UIButton(
                        "Back",
                        (__, ___) -> {
                            mJoinUi.setEnabled(true);
                            mServerBrowserUi.setEnabled(false);
                        });

        UIInputBox inputBox = new UIInputBox("Enter Lobby ID");

        UIManager.getInstance()
                .buildVerticalUi(
                        serverList,
                        0.05f,
                        0.15f,
                        0.35f,
                        inputBox,
                        new UIButton(
                                "Join with ID",
                                (button, ___) -> {
                                    for (Map.Entry<String, String> entry : mHosts.entrySet()) {
                                        if (entry.getKey().startsWith(inputBox.getInput())) {
                                            final String ip = entry.getKey();
                                            button.getLabelText().get().setText("Connecting...");
                                            mNetworkManager
                                                    .get()
                                                    .createClient(
                                                            ip,
                                                            PORT,
                                                            (manager, netID) -> {
                                                                button.getLabelText()
                                                                        .get()
                                                                        .setText("Join with ID");
                                                                if (netID >= 0) {
                                                                    mJoiningUi.setEnabled(true);
                                                                    mServerBrowserUi.setEnabled(
                                                                            false);
                                                                } else {
                                                                    LobbyAPI.getAllHosts(
                                                                            this::onGetAllHosts);
                                                                }
                                                            },
                                                            this::onHostStartGame);
                                        }
                                    }
                                }));

        UIManager.getInstance().buildVerticalUi(serverList, 0.05f, 0, 0.2f, uiElements);

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
                                "Host public lobby",
                                (__, ___) -> {
                                    String ip = UPnP.getExternalIPAddress();
                                    LobbyAPI.addNewHost(ip, PORT, this::onAddNewHost);
                                    mNetworkManager
                                            .get()
                                            .createServer(
                                                    PORT,
                                                    this::onClientLoaded,
                                                    this::onGameStarted);
                                    mHostingUi.setEnabled(true);
                                    mHostUi.setEnabled(false);
                                }),
                        new UIButton(
                                "Host locally",
                                (__, ___) -> {
                                    mNetworkManager
                                            .get()
                                            .createServer(
                                                    PORT,
                                                    this::onClientLoaded,
                                                    this::onGameStarted);
                                    mHostingUi.setEnabled(true);
                                    mHostUi.setEnabled(false);
                                }),
                        new UIButton(
                                "Back",
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

    private void onGetAllHosts(String response, boolean success) {
        if (!success) {
            log.warning("Failed to get server list");
            return;
        }

        mHosts.clear();
        JSONParser parser = new JSONParser();
        try {
            JSONArray array = (JSONArray) parser.parse(response);

            for (Object o : array) {
                JSONObject obj = (JSONObject) o;

                String id = (String) obj.get("_id");
                String ip = (String) obj.get("address");
                int port = Math.toIntExact((Long) obj.get("port"));

                // Shouldn't need to save port since all servers are created with a constant port

                log.fine("New host found: " + ip + ":" + port);
                mHosts.put(id, ip);
            }

        } catch (ParseException e) {
            log.info("Failed to parse response from get all hosts");
            return;
        }
        mHostsUpdated.set(true);
    }

    private void onAddNewHost(String response, boolean success) {
        if (!success) {
            log.warning("Failed to add new host to server list");
            return;
        }
        // TODO: Close server if we were unable to add to the server list? Or just leave it?
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(response);
            mLobbyId = (String) obj.get("_id");
            mLobbyIDUpdated.set(true);
        } catch (ParseException e) {
            e.printStackTrace();
            log.warning("Failed to parse response from add new host.");
        }
    }

    private void onDeleteHost(String response, boolean success) {
        if (!success) {
            log.warning("Failed to delete host from the server list");
        } else {
            mLobbyId = "";
        }
    }

    private void onHostStartGame(Scene gameScene, NetworkManager manager, int netId) {
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
        log.fine("Client ID: " + networkClient.getNetworkID() + " loaded.");
        int id = networkClient.getNetworkID();
        manager.getServerManager().spawnNetworkObject(id, manager.findTemplateByName("player"));
    }

    private void onGameStarted(NetworkManager manager) {
        log.fine("Game Start");
        log.fine("Spawning 'Server' Owned objects");
        manager.getServerManager().spawnNetworkObject(-10000, manager.findTemplateByName("map"));
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mHostsUpdated.compareAndSet(true, false)) {
            buildServerList();
        }
        if (mLobbyIDUpdated.compareAndSet(true, false)) {
            mLobbyIDText.setText("ID: " + mLobbyId.substring(10));
        }
    }

    @Override
    protected void onDestroy() {}
}
