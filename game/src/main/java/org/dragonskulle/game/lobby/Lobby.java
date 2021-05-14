/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.lobby;

import com.google.common.net.InetAddresses;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.lambda.LambdaFrameUpdate;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.futures.ProducerFuture;
import org.dragonskulle.game.GameConfig;
import org.dragonskulle.game.GameState;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.game.player.ai.AimerAi;
import org.dragonskulle.game.player.ui.UIPauseMenu;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.UPnP;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkManager.IGameEndEvent;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.ServerNetworkManager;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIInputBox;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;
import org.joml.Vector4f;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Class that handles the creation, deletion, joining and leaving of game lobbies.
 *
 * @author Harry Stoltz
 */
@Log
@Accessors(prefix = "m")
public class Lobby extends Component implements IFrameUpdate {

    private static final int PORT = 17569;

    public static final int MAX_PLAYERS = 6;

    private final Map<String, String> mHosts = new HashMap<>();
    private final AtomicBoolean mHostsUpdated = new AtomicBoolean(false);
    private final AtomicBoolean mLobbyIDUpdated = new AtomicBoolean(false);
    @Getter private final GameObject mLobbyUi;
    private final GameObject mHostUi;
    private final GameObject mJoinUi;
    private final GameObject mServerListUi;
    private final GameObject mJoinIPUi;
    private Reference<GameObject> mServerList;
    private final GameObject mFailedToForwardUi;
    private final GameObject mHostingUi;
    private final GameObject mJoiningUi;
    private final Reference<NetworkManager> mClientNetworkManager;
    private final Reference<NetworkManager> mServerNetworkManager;
    private String mLobbyId = "";
    private final UITextRect mLobbyIDText;

    /**
     * Default constructor, creates all static UI elements and also GameObjects that will have the
     * dynamic UI elements added to them.
     *
     * @param mainUi Reference to the main UI object
     * @param clientNetworkManager The client NetworkManager for the scene
     * @param serverNetworkManager The server NetworkManager for the scene
     */
    public Lobby(
            Reference<GameObject> mainUi,
            Reference<NetworkManager> clientNetworkManager,
            Reference<NetworkManager> serverNetworkManager) {
        mClientNetworkManager = clientNetworkManager;
        mServerNetworkManager = serverNetworkManager;

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

        mJoinIPUi =
                new GameObject(
                        "joinIPUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
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
                        });

        mLobbyIDText = new UITextRect("Private Game");

        UIManager.getInstance()
                .buildVerticalUi(
                        mLobbyUi,
                        0.3f,
                        0,
                        1,
                        new UITextRect("Play Game:"),
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
                        0.3f,
                        0,
                        1,
                        (go) -> {
                            UITextRect playerCount = new UITextRect("Players: -/-");
                            go.addComponent(playerCount);
                            go.addComponent(
                                    new LambdaFrameUpdate(
                                            (__) -> {
                                                if (Reference.isValid(playerCount.getLabelText())
                                                        && Reference.isValid(
                                                                mServerNetworkManager)) {
                                                    int clientCount =
                                                            mServerNetworkManager
                                                                    .get()
                                                                    .getClientCount();
                                                    playerCount
                                                            .getLabelText()
                                                            .get()
                                                            .setText(
                                                                    String.format(
                                                                            "Players: %d/%d",
                                                                            clientCount,
                                                                            MAX_PLAYERS));
                                                }
                                            }));
                        },
                        mLobbyIDText,
                        new UIButton(
                                "Start Game",
                                (__, ___) -> {
                                    mServerNetworkManager.get().getServerManager().start(false);
                                    GameAPI.deleteHostAsync(mLobbyId, this::onDeleteHost);
                                    mLobbyId = "";
                                    mLobbyIDUpdated.set(true);
                                    LobbyDiscovery.closeLocalLobby();
                                }),
                        new UIButton(
                                "Close lobby",
                                (__, ___) -> {
                                    if (Reference.isValid(mServerNetworkManager)) {
                                        final NetworkManager serverManager =
                                                mServerNetworkManager.get();
                                        if (serverManager.getServerManager() != null) {
                                            serverManager.getServerManager().destroy();
                                        }
                                    }
                                    if (Reference.isValid(mClientNetworkManager)) {
                                        final NetworkManager clientManager =
                                                mClientNetworkManager.get();
                                        if (clientManager.getClientManager() != null) {
                                            clientManager.getClientManager().disconnect();
                                        }
                                    }
                                    if (!mLobbyId.equals("")) {
                                        GameAPI.deleteHostAsync(mLobbyId, this::onDeleteHost);
                                        mLobbyId = "";
                                        mLobbyIDUpdated.set(true);
                                    }
                                    LobbyDiscovery.closeLocalLobby();
                                    mHostUi.setEnabled(true);
                                    mHostingUi.setEnabled(false);
                                    if (UPnP.checkMappingExists(PORT, "TCP")) {
                                        UPnP.deletePortMapping(PORT, "TCP");
                                    }
                                }));

        UIManager.getInstance()
                .buildVerticalUi(
                        mJoiningUi,
                        0.3f,
                        0,
                        1,
                        (go) -> {
                            UITextRect playerCount = new UITextRect("Players: -/-");
                            go.addComponent(playerCount);
                            go.addComponent(
                                    new LambdaFrameUpdate(
                                            (__) -> {
                                                if (Reference.isValid(playerCount.getLabelText())
                                                        && Reference.isValid(
                                                                mClientNetworkManager)) {
                                                    int clientCount =
                                                            mClientNetworkManager
                                                                    .get()
                                                                    .getClientCount();
                                                    playerCount
                                                            .getLabelText()
                                                            .get()
                                                            .setText(
                                                                    String.format(
                                                                            "Players: %d/%d",
                                                                            clientCount,
                                                                            MAX_PLAYERS));
                                                }
                                            }));
                        },
                        new UITextRect("Waiting for host"),
                        new UIButton(
                                "Leave lobby",
                                (__, ___) -> {
                                    if (Reference.isValid(mClientNetworkManager)) {
                                        final NetworkManager manager = mClientNetworkManager.get();
                                        if (manager.getClientManager() != null) {
                                            manager.getClientManager().disconnect();
                                        }
                                    }
                                    if (mJoiningUi.isEnabled() && !mServerListUi.isEnabled()) {
                                        mJoinUi.setEnabled(true);
                                        mJoiningUi.setEnabled(false);
                                    }
                                }));

        UIInputBox ipInput = new UIInputBox("");

        UIManager.getInstance()
                .buildVerticalUi(
                        mJoinIPUi,
                        0.3f,
                        0,
                        1,
                        new UITextRect("Enter IP:"),
                        ipInput,
                        new UIButton(
                                "Connect",
                                (button, ___) -> {
                                    final String ip = ipInput.getInput();
                                    if (!InetAddresses.isInetAddress(ip)) {
                                        ipInput.setText("Invalid IP!");
                                        return;
                                    }
                                    button.getLabelText().get().setText("Connecting...");
                                    mClientNetworkManager
                                            .get()
                                            .createClient(
                                                    ip,
                                                    PORT,
                                                    (manager, netID) -> {
                                                        button.getLabelText()
                                                                .get()
                                                                .setText("Connect");

                                                        if (netID >= 0) {
                                                            mJoiningUi.setEnabled(true);
                                                            mJoinIPUi.setEnabled(false);
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
                                    mJoinIPUi.setEnabled(false);
                                    mJoinUi.setEnabled(true);
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
        GameAPI.getAllHostsAsync(this::onGetAllHosts);
    }

    /**
     * Create the game server.
     *
     * @param isLocal whether the game is local.
     * @param removePort whether to remove the UPnP port on stop.
     */
    private void createServer(boolean isLocal, boolean removePort) {
        GameConfig.refreshConfig().schedule();

        if (isLocal) {
            LobbyDiscovery.openLocalLobby();
        } else {
            String ip = UPnP.getExternalIPAddress();
            mLobbyId = "loading";
            mLobbyIDUpdated.set(true);
            GameAPI.addNewHostAsync(ip, PORT, this::onAddNewHost);
        }

        if (createServer(
                mServerNetworkManager.get(),
                (____) -> {
                    if (removePort) {
                        UPnP.deletePortMapping(PORT, "TCP");
                    }
                    mHostingUi.setEnabled(false);
                    mHostUi.setEnabled(true);
                })) {
            mClientNetworkManager
                    .get()
                    .createClient(
                            "127.0.0.1",
                            PORT,
                            null,
                            Lobby::onHostStartGame,
                            () -> {
                                if (Reference.isValid(mServerNetworkManager)) {
                                    final ServerNetworkManager serverManager =
                                            mServerNetworkManager.get().getServerManager();
                                    if (serverManager != null) {
                                        serverManager.destroy();
                                    }
                                }
                            });

            mHostUi.setEnabled(false);
            mHostingUi.setEnabled(true);
            mFailedToForwardUi.setEnabled(false);
        }
    }

    /**
     * Create the game server.
     *
     * @param manager network manager to craete the server on.
     * @param endEvent game end event.
     * @return {@code true} if server creation was successful.
     */
    public static boolean createServer(NetworkManager manager, IGameEndEvent endEvent) {
        return manager.createServer(
                PORT,
                Lobby::onClientConnectionAttempt,
                null,
                Lobby::onClientLoaded,
                Lobby::onGameStarted,
                endEvent);
    }

    /** Builds the "Join" section of the UI. */
    private void buildJoinUi() {

        boolean[] isDiscovering = {false};

        UIManager.getInstance()
                .buildVerticalUi(
                        mJoinUi,
                        0.3f,
                        0,
                        1,
                        new UITextRect("Join Game:"),
                        new UIButton(
                                "Join public lobby",
                                (__, ___) -> {
                                    mServerListUi.setEnabled(true);
                                    mJoinUi.setEnabled(false);
                                }),
                        new UIButton(
                                "Join by IP",
                                (__, ___) -> {
                                    mJoinIPUi.setEnabled(true);
                                    mJoinUi.setEnabled(false);
                                }),
                        new UIButton(
                                "Join locally",
                                (button, __) -> {
                                    button.getLabelText().get().setText("Connecting...");
                                    isDiscovering[0] = true;

                                    new ProducerFuture<>(
                                                    () -> LobbyDiscovery.discoverLobby(),
                                                    (___, address) -> {
                                                        if (isDiscovering[0] && address != null) {
                                                            mClientNetworkManager
                                                                    .get()
                                                                    .createClient(
                                                                            address
                                                                                    .getHostAddress(),
                                                                            PORT,
                                                                            (manager, netID) -> {
                                                                                button.getLabelText()
                                                                                        .get()
                                                                                        .setText(
                                                                                                "Join locally");

                                                                                mJoiningUi
                                                                                        .setEnabled(
                                                                                                true);
                                                                                mJoinUi.setEnabled(
                                                                                        false);
                                                                            },
                                                                            Lobby::onHostStartGame,
                                                                            () -> {
                                                                                mJoiningUi
                                                                                        .setEnabled(
                                                                                                false);
                                                                                mJoinUi.setEnabled(
                                                                                        true);
                                                                            });
                                                        } else {
                                                            button.getLabelText()
                                                                    .get()
                                                                    .setText("Join locally");
                                                        }
                                                        isDiscovering[0] = false;
                                                    })
                                            .schedule();
                                }),
                        new UIButton(
                                "Back",
                                (__, ___) -> {
                                    isDiscovering[0] = false;
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

        UIManager.IUIBuildHandler[] uiElements = new UIManager.IUIBuildHandler[mHosts.size() + 2];

        uiElements[0] = new UITextRect("Server List:");

        uiElements[1] =
                new UIButton(
                        "Refresh",
                        (button, ___) -> {
                            button.getLabelText().get().setText("Refreshing...");
                            GameAPI.getAllHostsAsync(this::onGetAllHosts);
                        });

        int i = 2;
        for (Map.Entry<String, String> entry : mHosts.entrySet()) {
            final String id = entry.getKey();
            final String ip = entry.getValue();
            uiElements[i] =
                    new UIButton(
                            id,
                            (button, ___) -> {
                                button.getLabelText().get().setText("Connecting...");
                                mClientNetworkManager
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
                                                        GameAPI.getAllHostsAsync(
                                                                this::onGetAllHosts);
                                                    }
                                                },
                                                Lobby::onHostStartGame,
                                                () -> {
                                                    GameAPI.getAllHostsAsync(this::onGetAllHosts);
                                                    mJoiningUi.setEnabled(false);
                                                    mServerListUi.setEnabled(true);
                                                });
                            });
            i++;
        }

        UIInputBox inputBox = new UIInputBox("Enter Lobby ID");

        UIManager.getInstance()
                .buildVerticalUi(
                        serverList,
                        0.3f,
                        0.4f,
                        0.6f,
                        0.5f,
                        new UITextRect("Join Public Lobby:"),
                        inputBox,
                        new UIButton(
                                "Join with ID",
                                (button, ___) -> {
                                    button.getLabelText().get().setText("Connecting...");
                                    GameAPI.getHostById(
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
                                                        mClientNetworkManager
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
                                                                                GameAPI
                                                                                        .getAllHostsAsync(
                                                                                                this
                                                                                                        ::onGetAllHosts);
                                                                            }
                                                                        },
                                                                        Lobby::onHostStartGame,
                                                                        () -> {
                                                                            GameAPI
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
                                                        log.warning(
                                                                "Failed to parse response from get host by id");
                                                    }
                                                }
                                            });
                                    button.getLabelText().get().setText("Join with ID");
                                }),
                        new UIButton(
                                "Back",
                                (__, ___) -> {
                                    mJoinUi.setEnabled(true);
                                    mServerListUi.setEnabled(false);
                                }));

        UIManager.getInstance().buildVerticalUi(serverList, 0.3f, 0, 0.4f, 1f, uiElements);

        mServerListUi.addChild(mServerList.get());
    }

    /** Builds the "Host" section of the UI. */
    private void buildHostUi() {
        UIManager.getInstance()
                .buildVerticalUi(
                        mHostUi,
                        0.3f,
                        0,
                        1,
                        new UITextRect("Host Game:"),
                        new UIButton(
                                "Host public lobby",
                                (__, ___) -> {
                                    if (!UPnP.isPortAvailable(PORT, "TCP")
                                            || !UPnP.addPortMapping(PORT, "TCP")) {
                                        mFailedToForwardUi.setEnabled(true);
                                        mHostUi.setEnabled(false);
                                    } else {
                                        createServer(false, true);
                                    }
                                }),
                        new UIButton(
                                "Host locally",
                                (__, ___) -> {
                                    createServer(true, false);
                                }),
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
        mainMenu.addRootObject(mJoinIPUi);
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
            log.fine("Failed to parse response from get all hosts");
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
     * Called on server side when a client attempts to connect to it.
     *
     * @param manager network manager which the event is called from.
     * @param client server client instance.
     * @return {@code null} if the client should be connected. {@code -1} otherwise.
     */
    public static Integer onClientConnectionAttempt(NetworkManager manager, ServerClient client) {
        if (manager.getServerManager().getClients().size() >= MAX_PLAYERS) {
            return -1;
        }
        return null;
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
        HumanPlayer humanPlayer = new HumanPlayer(manager.getReference(NetworkManager.class));

        GameObject humanPlayerObject = new GameObject("human player");
        humanPlayerObject.addComponent(humanPlayer);

        gameScene.addRootObject(humanPlayerObject);
        gameScene.registerSingleton(humanPlayer);
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
    }

    /**
     * Called on server side when the server is starting the game.
     *
     * @param manager The network manager.
     */
    public static void onGameStarted(NetworkManager manager) {
        log.fine("Game Start");
        log.fine("Spawning 'Server' Owned objects");

        ServerNetworkManager serverManager = manager.getServerManager();

        serverManager.spawnNetworkObject(-10000, manager.findTemplateByName("game_state"));

        serverManager.spawnNetworkObject(-10000, manager.findTemplateByName("map"));

        GameState gameState = Scene.getActiveScene().getSingleton(GameState.class);

        gameState.getNumPlayers().set(MAX_PLAYERS);

        gameState.registerGameEndListener(
                new Reference<>(
                        (__) -> {
                            UIPauseMenu pauseMenu =
                                    manager.getGameScene().getSingleton(UIPauseMenu.class);
                            if (pauseMenu != null) {
                                pauseMenu.endGame();
                            }
                        }));

        // Get the number of clients and thus the number of AI needed
        int clientNumber = serverManager.getClients().size();
        int numOfAi = MAX_PLAYERS - clientNumber;

        // Add the AI
        for (int i = -1; i >= -1 * numOfAi; i--) {
            Reference<NetworkObject> player =
                    serverManager.spawnNetworkObject(i, manager.findTemplateByName("player"));
            GameObject playerObj = player.get().getGameObject();

            // Choose which AI to add
            playerObj.addComponent(new AimerAi());
        }

        // Add regular players
        for (ServerClient c : serverManager.getClients()) {
            int id = c.getNetworkID();
            manager.getServerManager().spawnNetworkObject(id, manager.findTemplateByName("player"));
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mHostsUpdated.compareAndSet(true, false)) {
            buildServerList();
        }
        if (mLobbyIDUpdated.compareAndSet(true, false)) {
            if (Reference.isValid(mLobbyIDText.getLabelText())) {
                if (!mLobbyId.equals("")) {
                    mLobbyIDText.getLabelText().get().setText("ID: " + mLobbyId);
                } else {
                    mLobbyIDText.getLabelText().get().setText("Private Game" + mLobbyId);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (!mLobbyId.equals("")) {
            GameAPI.deleteHostAsync(mLobbyId, null);
        }
    }
}
