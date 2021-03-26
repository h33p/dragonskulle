/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.util.Scanner;
import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.audio.components.AudioListener;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.game.camera.KeyboardMovement;
import org.dragonskulle.game.camera.ScrollTranslate;
import org.dragonskulle.game.camera.ZoomTilt;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.components.*;
import org.dragonskulle.ui.*;
import org.joml.*;
import org.lwjgl.system.NativeResource;

@Log
public class App implements NativeResource {

    private static final int BGM_ID = AudioManager.getInstance().loadSound("game_background.wav");
    private static final int BGM2_ID =
            AudioManager.getInstance().loadSound("country_background_short.wav");
    private static final int BUTTON_SFX_ID = AudioManager.getInstance().loadSound("button-10.wav");

    private static String sIP = "127.0.0.1";
    private static int sPort = 7000;
    private static boolean sReload = false;

    private final Resource<GLTF> mMainMenuGLTF = GLTF.getResource("main_menu");
    private final Resource<GLTF> mNetworkTemplatesGLTF = GLTF.getResource("network_templates");

    private static Scene createMainScene() {
        // Create a scene
        Scene mainScene = new Scene("game");

        GameObject cameraRig =
                new GameObject(
                        "mainCamera",
                        (rig) -> {
                            KeyboardMovement keyboardMovement = new KeyboardMovement();
                            rig.addComponent(keyboardMovement);

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
                                                    cam.farPlane = 200;
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

        GameObject audioObject =
                new GameObject(
                        "audioObject",
                        new TransformUI(true),
                        (root) -> {
                            root.addComponent(new AudioSource());

                            TransformUI t = root.getTransform(TransformUI.class);
                            t.setParentAnchor(0.78f, 0.75f, 1f, 0.75f);
                            t.setMargin(0f, 0.1f, 0f, 0.2f);

                            //                            root.addComponent(
                            //                                    new UIButton(
                            //                                            new UIText(
                            //                                                    new Vector3f(0f,
                            // 0f, 0f),
                            //
                            // Font.getFontResource("Rise of Kingdom.ttf"),
                            //                                                    "Mute/Unmute"),
                            //                                            (uiButton, __) -> {
                            //
                            // AudioManager.getInstance()
                            //
                            // .toggleMute(SoundType.BACKGROUND);
                            //
                            // AudioManager.getInstance()
                            //
                            // .toggleMute(SoundType.SFX);
                            //                                            }));
                        });
        GameObject audioButtonEffect =
                new GameObject(
                        "audioObject",
                        (root) -> {
                            root.addComponent(new AudioSource());
                        });

        Reference<AudioSource> refAudio = audioObject.getComponent(AudioSource.class);
        Reference<AudioSource> refAudioButtonEffect =
                audioButtonEffect.getComponent(AudioSource.class);

        if (refAudio.isValid()) {
            AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 70);
            AudioManager.getInstance().setVolume(SoundType.SFX, 60);
            refAudio.get().loadAudio("game_background.wav", SoundType.BACKGROUND);
            refAudioButtonEffect.get().loadAudio("button-10.wav", SoundType.SFX);
            //            refAudio.get().play();
        }

        mainScene.addRootObject(audioObject);

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

    private static Scene createMainScene(
            Reference<NetworkManager> networkManagerReference, boolean asServer) {
        Scene mainScene = createMainScene();
        if (networkManagerReference != null && networkManagerReference.isValid() && asServer) {
            GameObject hostGameUI =
                    new GameObject(
                            "hostGameUI",
                            new TransformUI(false),
                            (root) -> {
                                root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
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
                                                    new UIRenderable(
                                                            new SampledTexture(
                                                                    "ui/wide_button.png")));
                                            box.addComponent(
                                                    new UIButton(
                                                            new UIText(
                                                                    new Vector3f(0f, 0f, 0f),
                                                                    Font.getFontResource(
                                                                            "Rise of Kingdom.ttf"),
                                                                    "Fill game with AI"),
                                                            (a, b) -> {
                                                                System.out.println(
                                                                        "should fill with ai");
                                                                networkManagerReference
                                                                        .get()
                                                                        .getServerManager()
                                                                        .spawnNetworkObject(
                                                                                -1,
                                                                                networkManagerReference
                                                                                        .get()
                                                                                        .findTemplateByName(
                                                                                                "aiPlayer"));
                                                            }));
                                        });
                            });
            mainScene.addRootObject(hostGameUI);
        }
        return mainScene;
    }

    private static Scene createMainMenu(Scene mainScene) {
        Scene mainMenu = new Scene("mainMenu");

        GameObject camera = new GameObject("mainCamera");
        Transform3D tr = (Transform3D) camera.getTransform();
        // Set where it's at
        tr.setPosition(0f, 0f, 1.5f);
        tr.rotateDeg(-30f, 0f, 70f);
        tr.translateLocal(0f, -8f, 0f);
        camera.addComponent(new Camera());
        mainMenu.addRootObject(camera);

        // Create a hexagon template
        GameObject hexagon = new GameObject("hexagon");

        // Add a renderable to it
        hexagon.addComponent(new Renderable());
        Reference<Renderable> hexRenderer = hexagon.getComponent(Renderable.class);
        hexRenderer.get().setMaterial(new VertexHighlightMaterial());
        VertexHighlightMaterial hexMaterial =
                hexRenderer.get().getMaterial(VertexHighlightMaterial.class);
        hexMaterial.setDistancePow(10f);
        hexMaterial.getTexColour().set(0.1f, 0.1f, 0.1f, 1.f);

        // Add wobble components
        hexagon.addComponent(new Wobbler());
        Reference<Wobbler> hexWobbler = hexagon.getComponent(Wobbler.class);

        GameObject hexRoot = new GameObject("hexRoot");
        hexRoot.addComponent(new Spinner(10, 10, 0.1f));

        // Create instances, change up some parameters
        for (int q = -INSTANCE_COUNT_ROOT / 2; q <= INSTANCE_COUNT_ROOT / 2; q++) {
            for (int r = -INSTANCE_COUNT_ROOT / 2; r <= INSTANCE_COUNT_ROOT / 2; r++) {
                int idx = q * r % COLOURS.length;
                if (idx < 0) idx += COLOURS.length;
                hexWobbler
                        .get()
                        .setPhaseShift((Math.abs(q) + Math.abs(r) + Math.abs(-q - r)) * 0.1f);
                hexMaterial.getColour().set(COLOURS[idx]);
                hexRoot.addChild(GameObject.instantiate(hexagon, new TransformHex(q, r)));
            }
        }
        return mainScene;
    }

    private Scene createMainMenu() {
        Scene mainMenu = mMainMenuGLTF.get().getDefaultScene();

        TemplateManager templates = new TemplateManager();

        templates.addAllObjects(
                mNetworkTemplatesGLTF.get().getDefaultScene().getGameObjects().stream()
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

        Reference<AudioSource> effectSource = new Reference<>(new AudioSource());

        GameObject audio =
                new GameObject(
                        "audio",
                        (audioRoot) -> {
                            audioRoot.buildChild(
                                    "muteUI",
                                    new TransformUI(true),
                                    (muteUI) -> {
                                        TransformUI t = muteUI.getTransform(TransformUI.class);
                                        t.setParentAnchor(0.78f, 0.75f, 1f, 0.75f);
                                        t.setMargin(0f, 0.1f, 0f, 0.2f);

                                        muteUI.addComponent(
                                                new UIButton(
                                                        new UIText(
                                                                new Vector3f(0f, 0f, 0f),
                                                                Font.getFontResource(
                                                                        "Rise of Kingdom.ttf"),
                                                                "Toggle Mute"),
                                                        (uiButton, __) -> {
                                                            effectSource
                                                                    .get()
                                                                    .playSound(BUTTON_SFX_ID);
                                                            AudioManager.getInstance()
                                                                    .toggleMasterMute();
                                                        }));
                                    });

                            audioRoot.addComponent(effectSource.get());
                            effectSource.get().setVolume(0.1f);

                            AudioSource bgm = new AudioSource();
                            bgm.setVolume(0.1f);
                            bgm.setLooping(true);
                            bgm.playSound(BGM_ID);

                            audioRoot.addComponent(bgm);
                            audioRoot.addComponent(new AudioListener());
                        });

        Reference<AudioSource> refAudio = audioObject.getComponent(AudioSource.class);
        Reference<AudioSource> refAudioButtonEffect =
                audioButtonEffect.getComponent(AudioSource.class);
        if (refAudio.isValid()) {
            AudioManager.getInstance().setVolume(SoundType.BACKGROUND, 70);
            AudioManager.getInstance().setVolume(SoundType.SFX, 60);
            refAudio.get().loadAudio("game_background.wav", SoundType.BACKGROUND);
            refAudioButtonEffect.get().loadAudio("button-10.wav", SoundType.SFX);
            //            refAudio.get().play();
        }

        GameObject gameTitle =
                new GameObject(
                        "title",
                        new TransformUI(true),
                        (title) -> {
                            TransformUI t = title.getTransform(TransformUI.class);
                            t.setParentAnchor(0.4f, 0.05f, 0.8f, 0.05f);
                            t.setMargin(0f, 0f, 0f, 0.2f);

                            title.addComponent(
                                    new UIText(
                                            new Vector3f(1f, 1f, 1f),
                                            Font.getFontResource("Rise of Kingdom.ttf"),
                                            "Hex Wars"));
                        });

        mainMenu.addRootObject(gameTitle);

        GameObject mainUI =
                new GameObject(
                        "mainUI",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject joinUI =
                new GameObject(
                        "joinUI",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject hostUI =
                new GameObject(
                        "hostUI",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        mainUI.buildChild(
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
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Join Game"),
                                                refAudioButtonEffect
                                                        .get()
                                                        .audibleClick(
                                                                (uiButton, __) -> {
                                                                    mainUI.setEnabled(false);
                                                                    joinUI.setEnabled(true);
                                                                }));
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
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Host Game"),
                                                refAudioButtonEffect
                                                        .get()
                                                        .audibleClick(
                                                                (uiButton, __) -> {
                                                                    mainUI.setEnabled(false);
                                                                    hostUI.setEnabled(true);
                                                                }));
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "settingsButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.25f, 0.5f, 0.25f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Settings"),
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                });

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
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Quit"),
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
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
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Quick Reload"),
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                    sReload = true;
                                                    Engine.getInstance().stop();
                                                });

                                button.addComponent(newButton);
                            });
                    bg.buildChild(
                            "slider",
                            new TransformUI(true),
                            (slider) -> {
                                slider.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.45f, 0.5f, 0.45f);
                                slider.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UISlider newSlider =
                                        new UISlider((uiSlider, val) -> System.out.println(val));

                                newSlider.setRoundStep(0.1f);
                                newSlider.setMaxValue(10f);

                                slider.addComponent(newSlider);
                            });
                });
        joinUI.buildChild(
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
                                                .setParentAnchor(0f, 0.12f, 0.5f, 0.12f);
                                        text.getTransform(TransformUI.class)
                                                .setMargin(0f, 0f, 0f, 0.07f);
                                        text.addComponent(
                                                new UIText(
                                                        new Vector3f(0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Connecting..."));
                                    });
                    final Reference<UIText> connectingTextRef =
                            connectingRef.get().getComponent(UIText.class);

                    connectingTextRef.get().setEnabled(false);

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Join (Temporary)"),
                                                refAudioButtonEffect
                                                        .get()
                                                        .audibleClick(
                                                                (uiButton, __) -> {
                                                                    networkManager
                                                                            .get()
                                                                            .recreateGameScene(
                                                                                    createMainScene(
                                                                                            networkManager,
                                                                                            false));
                                                                    networkManager
                                                                            .get()
                                                                            .createClient(
                                                                                    sIP,
                                                                                    sPort,
                                                                                    (manager,
                                                                                            netID) -> {
                                                                                        if (netID
                                                                                                >= 0) {
                                                                                            onConnectedClient(
                                                                                                    manager,
                                                                                                    netID);
                                                                                        } else if (connectingTextRef
                                                                                                .isValid()) {
                                                                                            connectingTextRef
                                                                                                    .get()
                                                                                                    .setEnabled(
                                                                                                            false);
                                                                                        }
                                                                                    });
                                                                    if (connectingTextRef.isValid())
                                                                        connectingTextRef
                                                                                .get()
                                                                                .setEnabled(true);
                                                                }));
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
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Cancel"),
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                    joinUI.setEnabled(false);
                                                    mainUI.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });
                });

        hostUI.buildChild(
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
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Host (Temporary)"),
                                                refAudioButtonEffect
                                                        .get()
                                                        .audibleClick(
                                                                (uiButton, __) -> {
                                                                    networkManager
                                                                            .get()
                                                                            .recreateGameScene(
                                                                                    createMainScene(
                                                                                            networkManager,
                                                                                            true));
                                                                    networkManager
                                                                            .get()
                                                                            .createServer(
                                                                                    sPort,
                                                                                    App
                                                                                            ::onClientConnected);
                                                                }));
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
                                                new UIText(
                                                        new Vector3f(0f, 0f, 0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Cancel"),
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                    hostUI.setEnabled(false);
                                                    mainUI.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });
                });

        joinUI.setEnabled(false);
        hostUI.setEnabled(false);

        mainMenu.addRootObject(GameObject.instantiate(hexRoot));

        mainMenu.addRootObject(networkManagerObject);

        mainMenu.addRootObject(hostUI);
        mainMenu.addRootObject(joinUI);
        mainMenu.addRootObject(mainUI);

        return mainMenu;
    }

    /**
     * Entrypoint of the program. Creates and runs one app instance
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        do {
            sReload = false;
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
                                    System.out.println("Address set successfully!");
                                } catch (Exception e) {
                                    System.out.println("Failed to set IP and port!");
                                }
                            }
                        })
                .start();

        // Run the game
        Engine.getInstance().start("Hex Wars", new GameBindings());
    }

    private static void onConnectedClient(NetworkManager manager, int netID) {
        System.out.println("CONNECTED ID " + netID);

        GameObject humanPlayer =
                new GameObject(
                        "human player",
                        (handle) -> {
                            handle.addComponent(
                                    new HumanPlayer(
                                            manager.getReference(NetworkManager.class), netID));
                        });

        manager.getGameScene().addRootObject(humanPlayer);
    }

    private void onClientConnected(
            Scene gameScene, NetworkManager manager, ServerClient networkClient) {
        int id = networkClient.getNetworkID();
        manager.getServerManager().spawnNetworkObject(id, manager.findTemplateByName("cube"));
        manager.getServerManager().spawnNetworkObject(id, manager.findTemplateByName("capital"));
        manager.getServerManager().spawnNetworkObject(id, manager.findTemplateByName("player"));
    }

    @Override
    public void free() {
        mMainMenuGLTF.free();
        mNetworkTemplatesGLTF.free();
    }
}
