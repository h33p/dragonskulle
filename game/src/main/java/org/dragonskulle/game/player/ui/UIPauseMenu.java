/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.ui.*;
import org.joml.Vector4f;

/**
 * A component that displays a pause menu- allowing access to a {@link UISettingsMenu} and gives an
 * option to leave the current game.
 *
 * @author Craig Wilbourne
 */
@Log
public class UIPauseMenu extends Component implements IOnAwake, IFrameUpdate {

    public interface IHandleEvent {
        void handle();
    }

    /** The possible states the settings menu can be in. */
    private static enum State {
        MENU,
        SETTINGS
    }

    /** The current state the pause menu is in. */
    private State mCurrentState = State.MENU;

    /** The {@link NetworkManager} being used. */
    private Reference<NetworkManager> mNetworkManager;
    /** The camera being used. */
    private GameObject mCamera;

    /** Contains the pause menu. */
    private GameObject mMenuContainer;
    /** Contains the settings menu. */
    private GameObject mSettingsContainer;
    /** Used to grey out the background. */
    private UIRenderable mBackground;

    /** Stores the {@link Screen} the {@link HumanPlayer} was previously on. */
    private Screen mPreviousScreen;

    /**
     * Create a pause menu.
     *
     * @param networkManager The {@link NetworkManager} being used.
     * @param camera The camera {@link GameObject} being used.
     */
    public UIPauseMenu(NetworkManager networkManager, GameObject camera) {
        mNetworkManager = networkManager.getReference(NetworkManager.class);
        mCamera = camera;
    }

    /**
     * Change to the desired {@link State}. This will enable and disable the relevant containers,
     * and update {@link #mCurrentState}.
     *
     * @param state The desired state.
     */
    private void switchToState(State state) {
        if (mCurrentState == state) return;

        switch (state) {
            case SETTINGS:
                mSettingsContainer.setEnabled(true);
                mMenuContainer.setEnabled(false);
                break;
            case MENU:
            default:
                mSettingsContainer.setEnabled(false);
                mMenuContainer.setEnabled(true);
                break;
        }

        mCurrentState = state;
    }

    /** Toggle the main pause menu visibility. */
    private void togglePause() {
        setPaused(!mMenuContainer.isEnabled());
    }

    /**
     * Set whether the game is paused.
     *
     * @param pause Whether to enter the pause menu ({@code true}) or leave it ({@code false}).
     */
    private void setPaused(boolean pause) {
        // Either leave or enter the pause menu.
        mMenuContainer.setEnabled(pause);
        // Grey out the background if entering, or disable it if leaving.
        mBackground.setEnabled(pause);
        // If the menu pause menu is enabled, disable the camera.
        mCamera.setEnabled(!pause);

        // Make the HumanPlayer go to the desired screen.
        Screen newScreen = Screen.DEFAULT_SCREEN;
        if (!pause) {
            newScreen = mPreviousScreen;
        }

        HumanPlayer humanPlayer = Scene.getActiveScene().getSingleton(HumanPlayer.class);
        if (humanPlayer == null) return;
        mPreviousScreen = humanPlayer.getScreenOn();
        humanPlayer.setScreenOn(newScreen);
        hideMenu(pause, humanPlayer);
    }

    /** Generate the contents of {@link #mMenuContainer}. */
    private void generateMenu() {
        UITextRect title = new UITextRect("Paused");

        UIButton resume =
                new UIButton(
                        "Resume",
                        (__, ___) -> {
                            setPaused(false);
                        });

        UIButton settings =
                new UIButton(
                        "Settings",
                        (__, ___) -> {
                            switchToState(State.SETTINGS);
                        });

        UIButton exit =
                new UIButton(
                        "Quit",
                        (__, ___) -> {
                            if (Reference.isValid(mNetworkManager)) {
                                mNetworkManager.get().closeInstance();
                                ;
                            } else {
                                log.severe("Unable to get network manager.");
                            }
                        });

        UIManager.getInstance()
                .buildVerticalUi(mMenuContainer, 0.3f, 0, 1f, title, resume, settings, exit);
    }

    @Override
    public void onAwake() {
        // Create the GameObject that will hold all of the menu contents.
        mMenuContainer = new GameObject("pause_container", new TransformUI());
        getGameObject().addChild(mMenuContainer);

        // Create a settings menu.
        mSettingsContainer =
                new GameObject(
                        "settings_container",
                        new TransformUI(),
                        (settings) -> {
                            settings.addComponent(
                                    new UISettingsMenu(
                                            () -> {
                                                switchToState(State.MENU);
                                            }));
                        });
        getGameObject().addChild(mSettingsContainer);

        // Used to grey out the background.
        mBackground = new UIRenderable(new Vector4f(1f, 1f, 1f, 0.25f));
        getGameObject().addComponent(mBackground);

        // Ensure all aspects of the menu are hidden.
        mMenuContainer.setEnabled(false);
        mSettingsContainer.setEnabled(false);
        mBackground.setEnabled(false);

        // Generate the menu contents.
        generateMenu();
    }

    @Override
    public void frameUpdate(float deltaTime) {
        // If the pause screen is in the main menu and the pause key is pressed.
        if (GameActions.TOGGLE_PAUSE.isJustActivated() && mCurrentState == State.MENU) {
            togglePause();
        }
    }

    private static void hideMenu(boolean hide, HumanPlayer hp) {
        Reference<UIMenuLeftDrawer> menuDrawer = hp.getMenuDrawer();
        Reference<UILinkedScrollBar> scrollBar = hp.getScrollBar();
        if (Reference.isValid(menuDrawer)) {
            menuDrawer.get().setHidden(hide);
        }
        if (Reference.isValid(scrollBar)) {
            scrollBar.get().getGameObject().setEnabled(!hide);
        }
    }

    @Override
    protected void onDestroy() {}
}
