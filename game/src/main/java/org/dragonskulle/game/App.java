/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.util.Scanner;
import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.audio.components.AudioListener;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.game.camera.KeyboardMovement;
import org.dragonskulle.game.camera.ScrollTranslate;
import org.dragonskulle.game.camera.TargetMovement;
import org.dragonskulle.game.camera.ZoomTilt;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.game.map.FogOfWar;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.joml.Vector4f;
import org.lwjgl.system.NativeResource;

@Log
public class App implements NativeResource {

    private static final int BGM_ID = AudioManager.getInstance().loadSound("game_background.wav");
    private static final int BGM2_ID =
            AudioManager.getInstance().loadSound("country_background_short.wav");

    private static String sIP = "127.0.0.1";
    private static int sPort = 7000;
    private static boolean sReload = false;

    private final Resource<GLTF> mMainMenuGltf = GLTF.getResource("main_menu");
    private final Resource<GLTF> mNetworkTemplatesGltf = GLTF.getResource("network_templates");

    private static void addDebugUi(Scene scene) {
        GameObject debugUi =
                new GameObject(
                        "debugUi",
                        new TransformUI(true),
                        (handle) -> {
                            handle.getTransform(TransformUI.class)
                                    .setParentAnchor(0.0f, 1f, 0.5f, 1f);
                            handle.getTransform(TransformUI.class).setMargin(0f, -0.3f, 0f, 0f);
                            handle.getTransform(TransformUI.class).setPivotOffset(0f, 1f);
                            handle.addComponent(new org.dragonskulle.devtools.RenderDebug());
                        });

        scene.addRootObject(debugUi);
    }

    private static Scene createMainScene() {
        // Create a scene
        Scene mainScene = new Scene("game");

        addDebugUi(mainScene);

        mainScene.addRootObject(
                new GameObject(
                        "light",
                        (light) -> {
                            light.addComponent(new Light());
                            light.getTransform(Transform3D.class).setRotationDeg(-60f, 0f, 0f);
                        }));

        GameObject cameraRig =
                new GameObject(
                        "mainCamera",
                        (rig) -> {
                            KeyboardMovement keyboardMovement = new KeyboardMovement();
                            rig.addComponent(keyboardMovement);
                            rig.addComponent(new TargetMovement());

                            rig.getTransform(Transform3D.class).setPosition(0, -4, 1.5f);

                            rig.buildChild(
                                    "rotationRig",
                                    (pitchRig) -> {
                                        ZoomTilt zoomTilt = new ZoomTilt();
                                        pitchRig.addComponent(zoomTilt);
                                        pitchRig.buildChild(
                                                "camera",
                                                (camera) -> {
                                                    ScrollTranslate scroll =
                                                            new ScrollTranslate(
                                                                    keyboardMovement, zoomTilt);
                                                    scroll.getStartPos().set(0f, -5f, 0f);
                                                    scroll.getEndPos().set(0f, -100f, 0f);
                                                    camera.addComponent(scroll);

                                                    // Make sure it's an actual camera
                                                    Camera cam = new Camera();
                                                    cam.mFarPlane = 200;
                                                    camera.addComponent(cam);

                                                    camera.addComponent(new MapEffects());
                                                    camera.addComponent(new FogOfWar());

                                                    AudioListener listener = new AudioListener();
                                                    camera.addComponent(listener);

                                                    AudioSource bgm = new AudioSource();
                                                    bgm.setVolume(0.1f);
                                                    bgm.setLooping(true);
                                                    bgm.playSound(BGM_ID);
                                                    camera.addComponent(bgm);
                                                });
                                    });
                        });

        mainScene.addRootObject(GameObject.instantiate(cameraRig));

        GameObject hexagonMap =
                new GameObject(
                        "hexagon map",
                        new Transform3D(),
                        (map) -> {
                            map.addComponent(new HexagonMap(51));
                        });

        mainScene.addRootObject(hexagonMap);

        return mainScene;
    }

    private static Scene createMainScene(NetworkManager networkManager, boolean asServer) {

        log.warning("We have got here " + asServer);
        Scene mainScene = createMainScene();

        // asServer = true;
        if (asServer) {
            log.warning("I am the server");
            GameObject hostGameUi =
                    new GameObject(
                            "hostGameUi",
                            new TransformUI(false),
                            (root) -> {
                                // root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f,
                                // 0.1f)));
                                root.getTransform(TransformUI.class).setParentAnchor(0f);
                                root.buildChild(
                                        "populate_with_ai",
                                        new TransformUI(true),
                                        (box) -> {
                                            box.getTransform(TransformUI.class)
                                                    .setParentAnchor(0.3f, 0.93f, 1f, 0.93f);
                                            box.getTransform(TransformUI.class)
                                                    .setMargin(0f, 0f, 0f, 0.07f);
                                            box.addComponent(
                                                    new UIButton(
                                                            "Fill game with AI",
                                                            (a, b) -> {
                                                                log.info("should fill with ai");
                                                                networkManager
                                                                        .getServerManager()
                                                                        .spawnNetworkObject(
                                                                                -1,
                                                                                networkManager
                                                                                        .findTemplateByName(
                                                                                                "aiPlayer"));

                                                                log.warning("Created ai");
                                                            }));
                                        });
                            });
            mainScene.addRootObject(hostGameUi);
        }
        return mainScene;
    }

    private Scene createMainMenu() {
        Scene mainMenu = mMainMenuGltf.get().getDefaultScene();

        addDebugUi(mainMenu);

        TemplateManager templates = new TemplateManager();

        templates.addAllObjects(
                mNetworkTemplatesGltf.get().getDefaultScene().getGameObjects().stream()
                        .toArray(GameObject[]::new));

        Reference<NetworkManager> networkManager =
                new NetworkManager(templates, App::createMainScene)
                        .getReference(NetworkManager.class);

        GameObject networkManagerObject =
                new GameObject(
                        "network manager",
                        (handle) -> {
                            handle.addComponent(networkManager.get());
                        });

        GameObject audio =
                new GameObject(
                        "audio",
                        (audioRoot) -> {
                            audioRoot.buildChild(
                                    "muteUI",
                                    new TransformUI(true),
                                    (muteUi) -> {
                                        TransformUI t = muteUi.getTransform(TransformUI.class);
                                        t.setParentAnchor(0.78f, 0.75f, 1f, 0.75f);
                                        t.setMargin(0f, 0.1f, 0f, 0.2f);

                                        muteUi.addComponent(
                                                new UIButton(
                                                        "Toggle Mute",
                                                        (uiButton, __) -> {
                                                            AudioManager.getInstance()
                                                                    .toggleMasterMute();
                                                        }));
                                    });

                            AudioSource bgm = new AudioSource();
                            bgm.setVolume(0.1f);
                            bgm.setLooping(true);
                            bgm.playSound(BGM_ID);

                            audioRoot.addComponent(bgm);
                            audioRoot.addComponent(new AudioListener());
                        });

        mainMenu.addRootObject(audio);

        GameObject gameTitle =
                new GameObject(
                        "title",
                        new TransformUI(true),
                        (title) -> {
                            TransformUI t = title.getTransform(TransformUI.class);
                            t.setParentAnchor(0.4f, 0.05f, 0.8f, 0.05f);
                            t.setMargin(0f, 0f, 0f, 0.2f);

                            UIText txt = new UIText("Hex Wars");
                            txt.setDepthShift(-1f);

                            title.addComponent(txt);
                        });

        mainMenu.addRootObject(gameTitle);

        GameObject mainUi =
                new GameObject(
                        "mainUi",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject joinUi =
                new GameObject(
                        "joinUi",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject hostUi =
                new GameObject(
                        "hostUi",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        mainUi.buildChild(
                "bg",
                new TransformUI(false),
                (bg) -> {
                    bg.addComponent(new UIRenderable(new Vector4f(0.1f, 0.1f, 0.1f, 0f)));

                    bg.getTransform(TransformUI.class).setParentAnchor(0f, 0f, 0.5f, 1.f);

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Join Game",
                                                (uiButton, __) -> {
                                                    mainUi.setEnabled(false);
                                                    joinUi.setEnabled(true);
                                                    hostUi.setEnabled(false);
                                                });
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "hostButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.15f, 0.5f, 0.15f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Host Game",
                                                (uiButton, __) -> {
                                                    mainUi.setEnabled(false);
                                                    hostUi.setEnabled(true);
                                                });
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "settingsButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.25f, 0.5f, 0.25f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton = new UIButton("Settings");
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "quitButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Quit",
                                                (uiButton, __) -> {
                                                    Engine.getInstance().stop();
                                                });

                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "reloadButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.45f, 0.5f, 0.45f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Quick Reload",
                                                (uiButton, __) -> {
                                                    sReload = true;
                                                    Engine.getInstance().stop();
                                                });

                                button.addComponent(newButton);
                            });
                });
        joinUi.buildChild(
                "bg",
                new TransformUI(false),
                (bg) -> {
                    bg.addComponent(new UIRenderable(new Vector4f(0.1f, 0.1f, 0.1f, 0f)));

                    bg.getTransform(TransformUI.class).setParentAnchor(0f, 0f, 0.5f, 1.f);

                    final Reference<GameObject> connectingRef =
                            bg.buildChild(
                                    "connecting",
                                    false,
                                    new TransformUI(true),
                                    (text) -> {
                                        text.getTransform(TransformUI.class)
                                                .setParentAnchor(0f, 0.25f, 0.5f, 0.25f);
                                        text.getTransform(TransformUI.class)
                                                .setMargin(0f, 0f, 0f, 0.07f);
                                        text.addComponent(new UIText("Connecting..."));
                                    });
                    final Reference<UIText> connectingTextRef =
                            connectingRef.get().getComponent(UIText.class);

                    connectingTextRef.get().setEnabled(false);

                    UIInputBox ibox = new UIInputBox(sIP + ":" + sPort);

                    bg.buildChild(
                            "ipInput",
                            new TransformUI(true),
                            (input) -> {
                                input.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                input.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);
                                input.addComponent(ibox);
                            });

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.15f, 0.5f, 0.15f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Join (Temporary)",
                                                (uiButton, __) -> {
                                                    int port = sPort;

                                                    try {
                                                        String text = ibox.getInput();
                                                        String[] elems = text.split(":");
                                                        String ip = elems[0];
                                                        String portText =
                                                                elems.length > 1 ? elems[1] : null;

                                                        if (portText != null) {
                                                            port = Integer.parseInt(portText);
                                                        }

                                                        networkManager
                                                                .get()
                                                                .createClient(
                                                                        ip,
                                                                        port,
                                                                        (gameScene,
                                                                                manager,
                                                                                netId) -> {
                                                                            if (netId >= 0) {
                                                                                onConnectedClient(
                                                                                        gameScene,
                                                                                        manager,
                                                                                        netId);
                                                                            } else if (Reference
                                                                                    .isValid(
                                                                                            connectingTextRef)) {
                                                                                connectingTextRef
                                                                                        .get()
                                                                                        .setEnabled(
                                                                                                false);
                                                                            }
                                                                        });
                                                    } catch (Exception e) {

                                                        e.printStackTrace();

                                                        if (Reference.isValid(connectingTextRef)) {
                                                            connectingTextRef
                                                                    .get()
                                                                    .setText("Invalid input!");
                                                        }
                                                    }

                                                    if (Reference.isValid(connectingTextRef)) {
                                                        connectingTextRef.get().setEnabled(true);
                                                    }
                                                });
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "cancelButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Cancel",
                                                (uiButton, __) -> {
                                                    joinUi.setEnabled(false);
                                                    mainUi.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });
                });

        hostUi.buildChild(
                "bg",
                new TransformUI(false),
                (bg) -> {
                    bg.addComponent(new UIRenderable(new Vector4f(0.1f, 0.1f, 0.1f, 0f)));

                    bg.getTransform(TransformUI.class).setParentAnchor(0f, 0f, 0.5f, 1.f);

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Host (Temporary)",
                                                (uiButton, __) -> {
                                                    networkManager
                                                            .get()
                                                            .createServer(
                                                                    sPort, this::onClientConnected);
                                                });
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "cancelButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Cancel",
                                                (uiButton, __) -> {
                                                    hostUi.setEnabled(false);
                                                    mainUi.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });
                });

        joinUi.setEnabled(false);
        hostUi.setEnabled(false);

        mainMenu.addRootObject(networkManagerObject);

        mainMenu.addRootObject(hostUi);
        mainMenu.addRootObject(joinUi);
        mainMenu.addRootObject(mainUi);

        return mainMenu;
    }

    /**
     * Entrypoint of the program. Creates and runs one app instance
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        GameUIAppearance.initialise();

        do {
            sReload = false;
            Settings.getInstance().loadSettings();
            try (App app = new App()) {
                app.run();
            }
        } while (sReload);

        AudioManager.getInstance().cleanup();
        System.exit(0);
    }

    private void run() {
        // Create the main menu
        Scene mainMenu = createMainMenu();

        // Load the mainMenu as the presentation scene
        Engine.getInstance().loadPresentationScene(mainMenu);

        // Load dev console
        // TODO: actually make a fully fledged console
        // TODO: join it at the end
        new Thread(
                        () -> {
                            Scanner in = new Scanner(System.in);

                            String line;

                            while ((line = in.nextLine()) != null) {
                                try {
                                    sPort = in.nextInt();
                                    sIP = line.trim();
                                    log.info("Address set successfully!");
                                } catch (Exception e) {
                                    log.info("Failed to set IP and port!");
                                }
                            }
                        })
                .start();

        // Run the game
        Engine.getInstance().start("Hex Wars", new GameBindings());
    }

    private void onConnectedClient(Scene gameScene, NetworkManager manager, int netId) {
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

    private void onClientConnected(
            Scene gameScene, NetworkManager manager, ServerClient networkClient) {
        int id = networkClient.getNetworkID();
        manager.getServerManager().spawnNetworkObject(id, manager.findTemplateByName("player"));
    }

    @Override
    public void free() {
        mMainMenuGltf.free();
        mNetworkTemplatesGltf.free();
    }
}
