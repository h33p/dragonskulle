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
import org.dragonskulle.game.GameState;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.game.player.ai.AimerAi;
import org.dragonskulle.game.player.ai.ProbabilisticAiPlayer;
import org.dragonskulle.game.player.ui.UIPauseMenu;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.UPnP;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkManager.IGameEndEvent;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.ui.*;
import org.joml.Vector4f;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@Log
@Accessors(prefix = "m")
/**
 * Class that handles the creation, deletion, joining and leaving of game lobbies
 *
 * @author Harry Stoltz
 */
public class Lobby extends Component implements IFrameUpdate {

    private static final int PORT = 17569;

    private final Map<String, String> mHosts = new HashMap<>();
    private final AtomicBoolean mHostsUpdated = new AtomicBoolean(false);
    private final AtomicBoolean mLobbyIDUpdated = new AtomicBoolean(false);
    @Getter private final GameObject mLobbyUi;
    private final GameObject mHostUi;
    private final GameObject mJoinUi;
    private final GameObject mServerListUi;
    private Reference<GameObject> mServerList;
    private final GameObject mFailedToForwardUi;
    private final GameObject mHostingUi;
    private final GameObject mJoiningUi;
    private final Reference<NetworkManager> mNetworkManager;
    private String mLobbyId = "";
    private final UIInputBox mLobbyIDText;

    /**
     * Default constructor, creates all static UI elements and also GameObjects that will have the
     * dynamic UI elements added to them.
     *
     * @param mainUi Reference to the main UI object
     * @param networkManager NetworkManager for the scene
     */
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

        mServerListUi =
                new GameObject(
                        "serverBrowserUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                            root.addChild(mServerList.get());
                        });

        mFailedToForwardUi =
                new GameObject(
                        "failedToForwardUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
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
                                    LobbyAPI.deleteHostAsync(mLobbyId, this::onDeleteHost);
                                    mLobbyId = "";
                                    mLobbyIDUpdated.set(true);
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
                                        LobbyAPI.deleteHostAsync(mLobbyId, this::onDeleteHost);
                                        mLobbyId = "";
                                        mLobbyIDUpdated.set(true);
                                    }
                                    mHostUi.setEnabled(true);
                                    mHostingUi.setEnabled(false);
                                    UPnP.deletePortMapping(PORT, "TCP");
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
                                    if (mJoiningUi.isEnabled() && !mServerListUi.isEnabled()) {
                                        mJoinUi.setEnabled(true);
                                        mJoiningUi.setEnabled(false);
                                    }
                                }));

        UIManager.getInstance()
                .buildVerticalUi(
                        mFailedToForwardUi,
                        0.3f,
                        0.1f,
                        0.9f,
                        new UIText(
                                new Vector4f(1f, 1f, 1f, 1f), "Automatic port forwarding failed!"),
                        new UIText(
                                new Vector4f(1f, 1f, 1f, 1f),
                                "To play online, please ensure port 17569 is opened and points to your local IP"),
                        new UIButton("Continue anyway", (__, ___) -> createServer(false, true)),
                        new UIButton(
                                "Try override",
                                (__, ___) -> {
                                    UPnP.addPortMapping(PORT, "TCP");
                                    createServer(false, true);
                                }),
                        new UIButton(
                                "Cancel",
                                (__, ___) -> {
                                    mHostUi.setEnabled(true);
                                    mFailedToForwardUi.setEnabled(false);
                                }));

        buildJoinUi();
        buildHostUi();
        buildServerList();
        LobbyAPI.getAllHostsAsync(this::onGetAllHosts);
    }

    private void createServer(boolean isLocal, boolean removePort) {
        if (!isLocal) {
            String ip = UPnP.getExternalIPAddress();
            LobbyAPI.addNewHostAsync(ip, PORT, this::onAddNewHost);
        }

        createServer(
                mNetworkManager.get(),
                (____) -> {
                    if (removePort) {
                        UPnP.deletePortMapping(PORT, "TCP");
                    }
                    mHostingUi.setEnabled(false);
                    mHostUi.setEnabled(true);
                });

        mHostUi.setEnabled(false);
        mHostingUi.setEnabled(true);
        mFailedToForwardUi.setEnabled(false);
    }

    public static void createServer(NetworkManager manager, IGameEndEvent endEvent) {
        manager.createServer(PORT, null, Lobby::onClientLoaded, Lobby::onGameStarted, endEvent);
    }

    /** Builds the "Join" section of the UI. */
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
                                    mServerListUi.setEnabled(true);
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
                                                    Lobby::onHostStartGame,
                                                    () -> {
                                                        if (mJoiningUi.isEnabled()) {
                                                            mJoiningUi.setEnabled(false);
                                                            mJoinUi.setEnabled(true);
                                                        }
                                                    });
                                }),
                        new UIButton(
                                "Back",
                                (__, ___) -> {
                                    mJoinUi.setEnabled(false);
                                    mLobbyUi.setEnabled(true);
                                }));
    }

    /**
     * Takes the map of hosts that we received from the API and converts them into a list of buttons
     * that can be used to join the various hosts. This is called every time the mHosts map is
     * updated.
     */
    private void buildServerList() {
        boolean enabled = false;
        if (Reference.isValid(mServerList)) {
            enabled = mServerList.get().isEnabled();
            mServerList.get().destroy();
        }
        mServerList = new GameObject("servers", enabled, new TransformUI(false)).getReference();

        final GameObject serverList = mServerList.get();

        UIManager.IUIBuildHandler[] uiElements = new UIManager.IUIBuildHandler[mHosts.size() + 3];

        int i = 2;
        for (Map.Entry<String, String> entry : mHosts.entrySet()) {
            final String id = entry.getKey();
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
                                                        mServerListUi.setEnabled(false);
                                                    } else {
                                                        LobbyAPI.getAllHostsAsync(
                                                                this::onGetAllHosts);
                                                    }
                                                },
                                                Lobby::onHostStartGame,
                                                () -> {
                                                    LobbyAPI.getAllHostsAsync(this::onGetAllHosts);
                                                    mJoiningUi.setEnabled(false);
                                                    mServerListUi.setEnabled(true);
                                                });
                            });
            i++;
        }

        UIInputBox inputBox = new UIInputBox("Enter Lobby ID");

        uiElements[0] = inputBox;

        uiElements[1] =
                new UIButton(
                        "Refresh",
                        (button, ___) -> {
                            button.getLabelText().get().setText("Refreshing...");
                            LobbyAPI.getAllHostsAsync(this::onGetAllHosts);
                        });

        uiElements[uiElements.length - 1] =
                new UIButton(
                        "Back",
                        (__, ___) -> {
                            mJoinUi.setEnabled(true);
                            mServerListUi.setEnabled(false);
                        });

        UIManager.getInstance()
                .buildVerticalUi(
                        serverList,
                        0.05f,
                        0.15f,
                        0.35f,
                        new UIButton(
                                "Join with ID",
                                (button, ___) -> {
                                    button.getLabelText().get().setText("Connecting...");
                                    LobbyAPI.getHostById(
                                            inputBox.getInput(),
                                            (response, success) -> {
                                                if (success) {
                                                    JSONParser parser = new JSONParser();
                                                    try {
                                                        JSONObject obj =
                                                                (JSONObject) parser.parse(response);
                                                        String ip = (String) obj.get("address");
                                                        int port =
                                                                Math.toIntExact(
                                                                        (Long) obj.get("port"));
                                                        mNetworkManager
                                                                .get()
                                                                .createClient(
                                                                        ip,
                                                                        port,
                                                                        (manager, netID) -> {
                                                                            if (netID >= 0) {
                                                                                mJoiningUi
                                                                                        .setEnabled(
                                                                                                true);
                                                                                mServerListUi
                                                                                        .setEnabled(
                                                                                                false);
                                                                            } else {
                                                                                LobbyAPI
                                                                                        .getAllHostsAsync(
                                                                                                this
                                                                                                        ::onGetAllHosts);
                                                                            }
                                                                        },
                                                                        Lobby::onHostStartGame,
                                                                        () -> {
                                                                            LobbyAPI
                                                                                    .getAllHostsAsync(
                                                                                            this
                                                                                                    ::onGetAllHosts);
                                                                            mJoiningUi.setEnabled(
                                                                                    false);
                                                                            mServerListUi
                                                                                    .setEnabled(
                                                                                            true);
                                                                        });

                                                    } catch (ParseException e) {
                                                        e.printStackTrace();
                                                        log.warning(
                                                                "Failed to parse response from get host by id");
                                                    }
                                                }
                                            });
                                    button.getLabelText().get().setText("Join with ID");
                                }));

        UIManager.getInstance().buildVerticalUi(serverList, 0.05f, 0, 0.2f, uiElements);

        mServerListUi.addChild(mServerList.get());
    }

    /** Builds the "Host" section of the UI. */
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
                                    if (!UPnP.isPortAvailable(PORT, "TCP")
                                            || !UPnP.addPortMapping(PORT, "TCP")) {
                                        mFailedToForwardUi.setEnabled(true);
                                    } else {
                                        createServer(false, true);
                                    }
                                }),
                        new UIButton("Host locally", (__, ___) -> createServer(true, false)),
                        new UIButton(
                                "Back",
                                (__, ___) -> {
                                    mHostUi.setEnabled(false);
                                    mLobbyUi.setEnabled(true);
                                }));
    }

    /**
     * Adds all UI objects to the main menu scene.
     *
     * @param mainMenu Scene to add the GameObjects to
     */
    public void addUiToScene(Scene mainMenu) {
        mainMenu.addRootObject(mLobbyUi);
        mainMenu.addRootObject(mJoinUi);
        mainMenu.addRootObject(mServerListUi);
        mainMenu.addRootObject(mFailedToForwardUi);
        mainMenu.addRootObject(mJoiningUi);
        mainMenu.addRootObject(mHostUi);
        mainMenu.addRootObject(mHostingUi);
    }

    /**
     * Handles LobbyAPI.getAllHosts. If success is true, the response string is parsed and all of
     * the hosts in the JSON array are added to mHosts.
     *
     * @param response String containing the response from the getAllHosts request
     * @param success true if the request was successful, false otherwise
     */
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

                String id = (String) obj.get("_code");
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

    /**
     * Handles LobbyAPI.addNewHost. If success is true, the response string is parsed and the new
     * lobby ID for our lobby is stored.
     *
     * @param response String containing the response from the addNewHost request
     * @param success true if the request was successful, false otherwise
     */
    private void onAddNewHost(String response, boolean success) {
        if (!success) {
            log.warning("Failed to add new host to server list");
            return;
        }
        // TODO: Close server if we were unable to add to the server list? Or just leave it?
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(response);
            mLobbyId = (String) obj.get("_code");
            mLobbyIDUpdated.set(true);
        } catch (ParseException e) {
            e.printStackTrace();
            log.warning("Failed to parse response from add new host.");
        }
    }

    /**
     * Handles LobbyAPI.deleteHost. If success is true, mLobbyId is set to a blank string.
     *
     * @param response String containing the response from the deleteHost request
     * @param success true if the request was successful, false otherwise
     */
    private void onDeleteHost(String response, boolean success) {
        if (!success) {
            log.warning("Failed to delete host from the server list");
        } else {
            mLobbyId = "";
        }
    }

    /**
     * Called on client side when the server the client is connected to sends the start game
     * message.
     *
     * @param gameScene The current game scene
     * @param manager The network manager
     * @param netId The network ID of the client
     */
    private static void onHostStartGame(Scene gameScene, NetworkManager manager, int netId) {
        GameObject humanPlayer =
                new GameObject(
                        "human player",
                        (handle) -> {
                            handle.addComponent(
                                    new HumanPlayer(manager.getReference(NetworkManager.class)));
                        });

        gameScene.addRootObject(humanPlayer);
    }

    /**
     * Called on server side when a client has fully loaded and sent the client loaded message to
     * the server.
     *
     * @param gameScene The current game scene
     * @param manager The network manager
     * @param networkClient The client that sent the loaded message
     */
    public static void onClientLoaded(
            Scene gameScene, NetworkManager manager, ServerClient networkClient) {
        log.fine("Client ID: " + networkClient.getNetworkID() + " loaded.");
        int id = networkClient.getNetworkID();
        manager.getServerManager().spawnNetworkObject(id, manager.findTemplateByName("player"));
    }

    /**
     * Called on server side when the server is starting the game.
     *
     * @param manager The network manager.
     */
    public static void onGameStarted(NetworkManager manager) {
        log.fine("Game Start");
        log.fine("Spawning 'Server' Owned objects");
        Reference<NetworkObject> obj =
                manager.getServerManager()
                        .spawnNetworkObject(-10000, manager.findTemplateByName("map"));

        Reference<GameState> gameState = obj.get().getGameObject().getComponent(GameState.class);

        // 6 players for now
        int maxPlayers = 6;

        gameState.get().getNumPlayers().set(maxPlayers);

        gameState
                .get()
                .registerGameEndListener(
                        new Reference<>(
                                (__) -> {
                                    UIPauseMenu pauseMenu =
                                            manager.getGameScene().getSingleton(UIPauseMenu.class);
                                    if (pauseMenu != null) {
                                        pauseMenu.endGame();
                                    }
                                }));

        // Get the number of clients and thus the number of AI needed
        int clientNumber = manager.getServerManager().getClients().size();
        int numOfAi = maxPlayers - clientNumber;

        // Add the AI
        for (int i = -1; i >= -1 * numOfAi; i--) {
            Reference<NetworkObject> player =
                    manager.getServerManager()
                            .spawnNetworkObject(i, manager.findTemplateByName("player"));
            GameObject playerObj = player.get().getGameObject();
            playerObj.addComponent(new ProbabilisticAiPlayer());
            playerObj.addComponent(new AimerAi());
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mHostsUpdated.compareAndSet(true, false)) {
            buildServerList();
        }
        if (mLobbyIDUpdated.compareAndSet(true, false)) {
            mLobbyIDText.setText("ID: " + mLobbyId);
        }
    }

    @Override
    protected void onDestroy() {
        if (!mLobbyId.equals("")) {
            LobbyAPI.deleteHostAsync(mLobbyId, null);
        }
    }
}
