/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import lombok.Getter;
import lombok.experimental.Accessors;
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
@Accessors(prefix = "m")
public class UIPauseMenu extends Component implements IOnAwake, IFrameUpdate {

    public interface IHandleEvent {
        void handle();
    }

    /** The possible states the settings menu can be in. */
    private static enum State {
        MENU,
        SETTINGS,
        END_GAME,
        QUIT_CONFIRM
    }

    /** The current state the pause menu is in. */
    private State mCurrentState = State.MENU;

    private State mQuitBackState = State.MENU;

    /** The {@link NetworkManager} being used. */
    private Reference<NetworkManager> mNetworkManager;
    /** The camera being used. */
    private GameObject mCamera;

    /** Stores whether the menu is currently paused or not. */
    @Getter private boolean mPaused;

    /** Contains the pause menu. */
    private GameObject mMenuContainer;
    /** Contains the settings menu. */
    private GameObject mSettingsContainer;
    /** Contains the end game menu. */
    private GameObject mEndGameContainer;
    /** Contains the quit confirmation menu. */
    private GameObject mQuitConfirmContainer;
    /** Used to grey out the background. */
    private UIRenderable mBackground;
    /** Text displayed at the end screen. */
    private UITextRect mEndGameRect;

    /** Stores the {@link Screen} the {@link HumanPlayer} was previously on. */
    private Screen mPreviousScreen;

    /** Holds {@link Reference} to the {@link HumanPlayer} in game. */
    private Reference<HumanPlayer> mHumanPlayer;

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
        mSettingsContainer.setEnabled(mPaused && state == State.SETTINGS);
        mMenuContainer.setEnabled(mPaused && state == State.MENU);
        mEndGameContainer.setEnabled(mPaused && state == State.END_GAME);
        mQuitConfirmContainer.setEnabled(mPaused && state == State.QUIT_CONFIRM);
        mCurrentState = state;
    }

    /** Toggle the main pause menu visibility. */
    private void togglePause() {
        setPaused(!mPaused);
    }

    /**
     * Set whether the game is paused.
     *
     * @param pause Whether to enter the pause menu ({@code true}) or leave it ({@code false}).
     */
    private void setPaused(boolean pause) {

        if (pause == mPaused) {
            return;
        }

        mPaused = pause;

        // If the menu pause menu is enabled, disable the camera.
        mCamera.setEnabled(!pause);
        mBackground.setEnabled(pause);

        switchToState(mCurrentState);

        // Make the HumanPlayer go to the desired screen.
        Screen newScreen = Screen.DEFAULT_SCREEN;
        if (!pause && mPreviousScreen != null) {
            newScreen = mPreviousScreen;
        }

        if (!Reference.isValid(mHumanPlayer)) return;
        mPreviousScreen = mHumanPlayer.get().getScreenOn();
        mHumanPlayer.get().setScreenOn(newScreen);
        hideMenu(pause, mHumanPlayer.get());
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

        UIButton exit = new UIButton("Quit", (__, ___) -> onClickQuit());

        UIManager.getInstance()
                .buildVerticalUi(mMenuContainer, 0.3f, 0, 1f, title, resume, settings, exit);
    }

    @Override
    public void onAwake() {

        Scene.getActiveScene().registerSingleton(this);

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

        mEndGameRect = new UITextRect("Game has ended!");

        mEndGameContainer =
                new GameObject(
                        "end_screen",
                        false,
                        new TransformUI(),
                        (go) -> {
                            UIManager.getInstance()
                                    .buildVerticalUi(
                                            go,
                                            0.3f,
                                            0.1f,
                                            0.9f,
                                            mEndGameRect,
                                            new UIButton("View Map", (__, ___) -> setPaused(false)),
                                            new UIButton("Quit", (__, ___) -> onClickQuit()));
                        });

        getGameObject().addChild(mEndGameContainer);

        mQuitConfirmContainer =
                new GameObject(
                        "confirm_quit",
                        false,
                        new TransformUI(),
                        (go) -> {
                            UIManager.getInstance()
                                    .buildVerticalUi(
                                            go,
                                            0.3f,
                                            0.3f,
                                            0.7f,
                                            new UITextRect("Are you sure?"),
                                            new UIText("Unsaved progress will be lost."),
                                            new UIButton(
                                                    "Yes",
                                                    (__, ___) -> {
                                                        if (Reference.isValid(mNetworkManager)) {
                                                            mNetworkManager.get().closeInstance();
                                                            ;
                                                        } else {
                                                            log.severe(
                                                                    "Unable to get network manager.");
                                                        }
                                                    }),
                                            new UIButton(
                                                    "No",
                                                    (__, ___) -> {
                                                        switchToState(mQuitBackState);
                                                    }));
                        });

        getGameObject().addChild(mQuitConfirmContainer);

        // Used to grey out the background.
        mBackground = new UIRenderable(new Vector4f(1f, 1f, 1f, 0.25f));
        getGameObject().addComponent(mBackground);

        // Ensure all aspects of the menu are hidden.
        mMenuContainer.setEnabled(false);
        mSettingsContainer.setEnabled(false);
        mEndGameContainer.setEnabled(false);
        mBackground.setEnabled(false);

        // Generate the menu contents.
        generateMenu();
    }

    /**
     * End the game.
     *
     * <p>This method will end the game, and display the ending screen, instead of the default pause
     * screen.
     */
    public void endGame() {
        switchToState(State.END_GAME);
        setPaused(true);
    }

    /**
     * End the game.
     *
     * <p>This is a supermethod for {@link endGame()}, that accepts an optional label to be
     * displayed in the end screen.
     *
     * @param label label to display on the end screen.
     */
    public void endGame(String label) {
        if (Reference.isValid(mEndGameRect.getLabelText())) {
            mEndGameRect.getLabelText().get().setText(label);
        }
        endGame();
    }

    @Override
    public void frameUpdate(float deltaTime) {

        if (!Reference.isValid(mHumanPlayer)) {
            mHumanPlayer = Scene.getActiveScene().getSingletonRef(HumanPlayer.class);
        }

        // If the pause screen is in the main menu and the pause key is pressed.
        if (GameActions.TOGGLE_PAUSE.isJustActivated()
                && (mCurrentState == State.MENU || mCurrentState == State.END_GAME)) {
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

    private void onClickQuit() {
        mQuitBackState = mCurrentState;
        switchToState(State.QUIT_CONFIRM);
    }

    @Override
    protected void onDestroy() {}
}
