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
import org.dragonskulle.game.camera.DragMovement;
import org.dragonskulle.game.camera.KeyboardMovement;
import org.dragonskulle.game.camera.ScrollTranslate;
import org.dragonskulle.game.camera.TargetMovement;
import org.dragonskulle.game.camera.ZoomTilt;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIDropDown;
import org.dragonskulle.ui.UIInputBox;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UISlider;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;
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

    public static final float MENU_BASEWIDTH = 0.2f;

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
                            light.getTransform(Transform3D.class).setRotationDeg(-60f, 0f, -30f);
                        }));

        GameObject cameraRig =
                new GameObject(
                        "mainCamera",
                        (rig) -> {
                            KeyboardMovement keyboardMovement = new KeyboardMovement();
                            rig.addComponent(keyboardMovement);
                            rig.addComponent(new TargetMovement());
                            rig.addComponent(new DragMovement());

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

        return mainScene;
    }

    private static Scene createMainScene(NetworkManager networkManager, boolean asServer) {

        Scene mainScene = createMainScene();
        // asServer = true;
        if (asServer) {
            log.info("I am the server");
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
                                                                                                "aStarAi"));

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

        for (GameObject obj : mNetworkTemplatesGltf.get().getDefaultScene().getGameObjects()) {
            log.info(obj.getName());
        }

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

        GameObject gameTitle =
                new GameObject(
                        "title",
                        new TransformUI(true),
                        (title) -> {
                            TransformUI t = title.getTransform(TransformUI.class);
                            t.setParentAnchor(0.4f, 0.05f, 0.8f, 0.05f);
                            t.setMargin(0f, 0f, 0f, 0.15f);
                            t.setPivotOffset(0.5f, 0.3f);

                            UIText txt = new UIText("Hex Wars");
                            txt.setDepthShift(-1f);

                            title.addComponent(txt);
                        });

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
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject hostUi =
                new GameObject(
                        "hostUi",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject settingsUI =
                new GameObject(
                        "settingsUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject audioSettingsUI =
                new GameObject(
                        "audioSettingsUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject graphicsSettingsUI =
                new GameObject(
                        "graphicsSettingsUI",
                        false,
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        final UIManager uiManager = UIManager.getInstance();

        uiManager.buildVerticalUi(
                mainUi,
                0.05f,
                0,
                MENU_BASEWIDTH,
                new UIButton(
                        "Join Game",
                        (__, ___) -> {
                            mainUi.setEnabled(false);
                            joinUi.setEnabled(true);
                            hostUi.setEnabled(false);
                        }),
                new UIButton(
                        "Host Game",
                        (__, ___) -> {
                            mainUi.setEnabled(false);
                            hostUi.setEnabled(true);
                        }),
                new UIButton(
                        "Settings",
                        (__, ___) -> {
                            mainUi.setEnabled(false);
                            settingsUI.setEnabled(true);
                        }),
                new UIButton("Quit", (__, ___) -> Engine.getInstance().stop()),
                new UIButton(
                        "Quick Reload",
                        (__, ___) -> {
                            sReload = true;
                            Engine.getInstance().stop();
                        }));

        final UITextRect connectingText = new UITextRect("");
        connectingText.setEnabled(false);
        connectingText.setOverrideAspectRatio(4f);
        connectingText.getColour().set(0f);

        UIInputBox ibox = new UIInputBox(sIP + ":" + sPort);

        uiManager.buildVerticalUi(
                joinUi,
                0.05f,
                0f,
                MENU_BASEWIDTH,
                ibox,
                new UIButton(
                        "Join (Temporary)",
                        (uiButton, __) -> {
                            int port = sPort;

                            connectingText.setEnabled(true);
                            connectingText.getGameObject().setEnabled(true);

                            try {
                                String text = ibox.getInput();
                                String[] elems = text.split(":");
                                String ip = elems[0];
                                String portText = elems.length > 1 ? elems[1] : null;

                                if (portText != null) {
                                    port = Integer.parseInt(portText);
                                }

                                connectingText.getLabelText().get().setText("Connecting...");

                                networkManager
                                        .get()
                                        .createClient(
                                                ip,
                                                port,
                                                (gameScene, manager, netID) -> {
                                                    if (netID >= 0) {
                                                        onConnectedClient(
                                                                gameScene, manager, netID);
                                                    } else {
                                                        connectingText.setEnabled(false);
                                                        connectingText
                                                                .getGameObject()
                                                                .setEnabled(false);
                                                    }
                                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                connectingText.getLabelText().get().setText("Invalid input!");
                            }
                        }),
                connectingText,
                new UIButton(
                        "Cancel",
                        (uiButton, __) -> {
                            joinUi.setEnabled(false);
                            mainUi.setEnabled(true);
                        }));

        uiManager.buildVerticalUi(
                settingsUI,
                0.05f,
                0f,
                MENU_BASEWIDTH,
                new UIButton(
                        "Sound",
                        (__, ___) -> {
                            settingsUI.setEnabled(false);
                            audioSettingsUI.setEnabled(true);
                        }),
                new UIButton(
                        "Graphics",
                        (__, ___) -> {
                            settingsUI.setEnabled(false);
                            graphicsSettingsUI.setEnabled(true);
                        }),
                new UIButton(
                        "Back",
                        (__, ___) -> {
                            settingsUI.setEnabled(false);
                            mainUi.setEnabled(true);
                        }));

        uiManager.buildVerticalUi(
                audioSettingsUI,
                0.05f,
                0f,
                MENU_BASEWIDTH,
                uiManager.buildWithChildrenRightOf(
                        new UITextRect("Master volume:"),
                        new UISlider(
                                AudioManager.getInstance().getMasterVolume(),
                                (__, val) -> AudioManager.getInstance().setMasterVolume(val))),
                new UIButton(
                        "Back",
                        (__, ___) -> {
                            audioSettingsUI.setEnabled(false);
                            settingsUI.setEnabled(true);
                        }));

        uiManager.buildVerticalUi(
                graphicsSettingsUI,
                0.05f,
                0f,
                MENU_BASEWIDTH,
                uiManager.buildWithChildrenRightOf(
                        new UITextRect("Fullscreen mode:"),
                        new UIDropDown(
                                0,
                                (drop) -> {
                                    Engine.getInstance()
                                            .getGLFWState()
                                            .setFullscreen(drop.getSelected() == 1);
                                },
                                "Windowed",
                                "Fullscreen")),
                new UIButton(
                        "Back",
                        (__, ___) -> {
                            graphicsSettingsUI.setEnabled(false);
                            settingsUI.setEnabled(true);
                        }));

        uiManager.buildVerticalUi(
                hostUi,
                0.05f,
                0f,
                MENU_BASEWIDTH,
                new UIButton(
                        "Host (Temporary)",
                        (__, ___) -> {
                            networkManager
                                    .get()
                                    .createServer(
                                            sPort, this::onClientConnected, this::onGameStarted);
                        }),
                new UIButton(
                        "Cancel",
                        (uiButton, __) -> {
                            hostUi.setEnabled(false);
                            mainUi.setEnabled(true);
                        }));

        mainMenu.addRootObject(networkManagerObject);

        mainMenu.addRootObject(audio);
        mainMenu.addRootObject(gameTitle);

        mainMenu.addRootObject(mainUi);
        mainMenu.addRootObject(hostUi);
        mainMenu.addRootObject(joinUi);
        mainMenu.addRootObject(settingsUI);
        mainMenu.addRootObject(audioSettingsUI);
        mainMenu.addRootObject(graphicsSettingsUI);

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
            AudioManager.getInstance().toggleMasterMute();
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

    private void onGameStarted(NetworkManager manager) {
        log.severe("Game Start");
        log.warning("Spawning 'Server' Owned objects");
        manager.getServerManager().spawnNetworkObject(-10000, manager.findTemplateByName("map"));
    }

    @Override
    public void free() {
        mMainMenuGltf.free();
        mNetworkTemplatesGltf.free();
    }
}
