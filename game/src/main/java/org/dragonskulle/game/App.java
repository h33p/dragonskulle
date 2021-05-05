/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

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
import org.dragonskulle.game.camera.HeightByMap;
import org.dragonskulle.game.camera.KeyboardMovement;
import org.dragonskulle.game.camera.ScrollTranslate;
import org.dragonskulle.game.camera.TargetMovement;
import org.dragonskulle.game.camera.ZoomTilt;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.game.lobby.Lobby;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.player.ui.UIPauseMenu;
import org.dragonskulle.game.player.ui.UISettingsMenu;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.settings.Settings;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.joml.Vector4f;
import org.lwjgl.system.NativeResource;

@Log
public class App implements NativeResource {
    private static final Settings mSettings = Settings.getInstance().loadSettings();

    private static boolean sReload = false;

    private final Resource<GLTF> mMainMenuGltf = GLTF.getResource("main_menu");
    private final Resource<GLTF> mNetworkTemplatesGltf = GLTF.getResource("network_templates");
    private static final String BGM_SOUND = "game_background.wav";

    public static final Resource<GLTF> TEMPLATES = GLTF.getResource("templates");

    public static final float MENU_BASEWIDTH = 0.2f;

    /**
     * Adds the debug overlay, this is enabled by pressing F3.
     *
     * @param scene the scene
     */
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

    /**
     * Creates the main scene.
     *
     * @param networkManager the network manager
     * @return the scene created
     */
    static Scene createMainScene(NetworkManager networkManager) {
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
                            HeightByMap heightByMap = new HeightByMap();
                            rig.addComponent(heightByMap);

                            rig.getTransform(Transform3D.class).setPosition(0, -4, 0.5f);

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
                                                                    keyboardMovement,
                                                                    zoomTilt,
                                                                    heightByMap);
                                                    scroll.getStartPos().set(0f, -2.5f, 0f);
                                                    scroll.getEndPos().set(0f, -100f, 0f);
                                                    camera.addComponent(scroll);

                                                    // Make sure it's an actual camera
                                                    Camera cam = new Camera();
                                                    cam.mFarPlane = 200;
                                                    camera.addComponent(cam);

                                                    camera.addComponent(new MapEffects());
                                                });
                                    });
                        });

        GameObject camera = GameObject.instantiate(cameraRig);
        mainScene.addRootObject(camera);

        GameObject audioObject =
                new GameObject(
                        "game_audio",
                        (audio) -> {
                            AudioListener listener = new AudioListener();
                            audio.addComponent(listener);

                            AudioSource bgm = new AudioSource();
                            bgm.setVolume(0.1f);
                            bgm.setLooping(true);
                            bgm.playSound(BGM_SOUND);
                            audio.addComponent(bgm);
                        });
        mainScene.addRootObject(audioObject);

        // Pause menu
        GameObject pauseMenu =
                new GameObject(
                        "pause menu",
                        new TransformUI(),
                        (menu) -> {
                            menu.addComponent(new UIPauseMenu(networkManager, camera));
                        });

        mainScene.addRootObject(pauseMenu);

        return mainScene;
    }

    /**
     * Creates the main scene.
     *
     * @param networkManager the network manager
     * @param asServer true, if to create as server
     * @return the scene created
     */
    static Scene createMainScene(NetworkManager networkManager, boolean asServer) {
        Scene mainScene = createMainScene(networkManager);
        return mainScene;
    }

    /**
     * Creates a collection of templates used by the game.
     *
     * @return a new template manager containing all networked templates from network templates glTF
     *     file.
     */
    TemplateManager createTemplateManager() {
        TemplateManager templates = new TemplateManager();

        for (GameObject obj : mNetworkTemplatesGltf.get().getDefaultScene().getGameObjects()) {
            log.info(obj.getName());
        }

        templates.addAllObjects(
                mNetworkTemplatesGltf.get().getDefaultScene().getGameObjects().stream()
                        .toArray(GameObject[]::new));

        return templates;
    }

    /**
     * Creates the main menu scene.
     *
     * @return the scene created
     */
    private Scene createMainMenu() {
        Scene mainMenu = mMainMenuGltf.get().getDefaultScene();
        addDebugUi(mainMenu);

        Reference<NetworkManager> networkManager =
                new NetworkManager(createTemplateManager(), App::createMainScene)
                        .getReference(NetworkManager.class);

        GameObject networkManagerObject =
                new GameObject(
                        "network manager",
                        (handle) -> {
                            handle.addComponent(networkManager.get());
                        });

        GameObject audioObject =
                new GameObject(
                        "menu audio",
                        (audio) -> {
                            AudioSource bgm = new AudioSource();
                            bgm.setVolume(0.1f);
                            bgm.setLooping(true);
                            bgm.playSound(BGM_SOUND);

                            audio.addComponent(bgm);
                            audio.addComponent(new AudioListener());
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

        Reference<Lobby> lobby =
                new Lobby(mainUi.getReference(), networkManager).getReference(Lobby.class);

        GameObject lobbyObject =
                new GameObject(
                        "lobby",
                        true,
                        (root) -> {
                            root.addComponent(lobby.get());
                        });

        GameObject settingsUI =
                new GameObject(
                        "settingsUI",
                        false,
                        new TransformUI(),
                        (settings) -> {
                            settings.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            // settings.getTransform(TransformUI.class).setParentAnchor(0f);
                            settings.addComponent(
                                    new UISettingsMenu(
                                            () -> {
                                                mainUi.setEnabled(true);
                                                settings.setEnabled(false);
                                            }));
                        });

        final UIManager uiManager = UIManager.getInstance();

        uiManager.buildVerticalUi(
                mainUi,
                0.05f,
                0,
                MENU_BASEWIDTH,
                new UIButton(
                        "Play Game",
                        (__, ___) -> {
                            mainUi.setEnabled(false);
                            lobby.get().getLobbyUi().setEnabled(true);
                        }),
                new UIButton(
                        "Settings",
                        (__, ___) -> {
                            mainUi.setEnabled(false);
                            settingsUI.setEnabled(true);
                        }),
                new UIButton("Quit", (__, ___) -> Engine.getInstance().stop()));

        mainMenu.addRootObject(networkManagerObject);

        mainMenu.addRootObject(audioObject);
        mainMenu.addRootObject(gameTitle);
        mainMenu.addRootObject(lobbyObject);

        mainMenu.addRootObject(mainUi);
        lobby.get().addUiToScene(mainMenu);
        mainMenu.addRootObject(settingsUI);

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

        // Run the game
        Engine.getInstance().start("Hex Wars", new GameBindings());
    }

    @Override
    public void free() {
        mMainMenuGltf.free();
        mNetworkTemplatesGltf.free();
    }
}
