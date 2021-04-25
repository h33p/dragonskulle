/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIDropDown;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;
import org.dragonskulle.ui.UISlider;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;

/**
 * A component that displays a settings menu.
 *
 * @author Craig Wilbourne
 */
public class UISettingsMenu extends Component implements IOnAwake, IFrameUpdate {

    /** Contains the action to execute when the user requests to leave the settings menu. */
    public static interface Back {
        /** Contains the code to execute on a back action. */
        public void run();
    }

    /** The action to execute when the user requests to leave the settings menu. */
    private Back mReturnAction;

    /** The possible states the settings menu can be in. */
    private static enum State {
        MENU,
        AUDIO,
        GRAPHICS
    }

    /** The current state the settings menu is in. */
    private State mCurrentState = State.MENU;

    /** GameObject that contains the main settings menu. */
    private GameObject mMenuContainer;
    /** GameObject that contains the audio settings menu. */
    private GameObject mAudioContainer;
    /** GameObject that contains the graphics settings menu. */
    private GameObject mGraphicsContainer;

    /**
     * Create a new settings menu component.
     *
     * @param returnAction The action to be executed when the user requests to leave the settings
     *     menu.
     */
    public UISettingsMenu(Back returnAction) {
        mReturnAction = returnAction;
    }

    /**
     * Change to the desired {@link State}. This will remove and add the relevant containers, and
     * update {@link #mCurrentState}.
     *
     * @param state The desired state.
     */
    private void switchToState(State state) {
        if (mCurrentState == state) return;

        switch (state) {
            case AUDIO:
                getGameObject().addChild(mAudioContainer);
                getGameObject().removeChild(mGraphicsContainer);
                mMenuContainer.setEnabled(false);
                break;
            case GRAPHICS:
                getGameObject().removeChild(mAudioContainer);
                getGameObject().addChild(mGraphicsContainer);
                mMenuContainer.setEnabled(false);
                break;
            case MENU:
            default:
                getGameObject().removeChild(mAudioContainer);
                getGameObject().removeChild(mGraphicsContainer);
                mMenuContainer.setEnabled(true);
                break;
        }

        mCurrentState = state;
    }

    /** Generate the contents of {@link #mMenuContainer}. */
    private void generateMenu() {
        UITextRect title = new UITextRect("Settings");

        UIButton resume =
                new UIButton(
                        "Audio",
                        (__, ___) -> {
                            switchToState(State.AUDIO);
                        });

        UIButton settings =
                new UIButton(
                        "Graphics",
                        (__, ___) -> {
                            switchToState(State.GRAPHICS);
                        });

        UIButton exit =
                new UIButton(
                        "Back",
                        (__, ___) -> {
                            switchToState(State.MENU);
                            mReturnAction.run();
                        });

        final UIManager uiManager = UIManager.getInstance();
        uiManager.buildVerticalUi(mMenuContainer, 0.3f, 0, 1f, title, resume, settings, exit);
    }

    /** Generate the contents of {@link #mAudioContainer}. */
    private void generateAudio() {
        final UIManager uiManager = UIManager.getInstance();

        // Title:
        UITextRect title = new UITextRect("Audio");

        // Mute:
        UITextRect muteTitle = new UITextRect("Toggle mute:");
        UIButton muteButton =
                new UIButton(
                        "Mute",
                        (button, __) -> {
                            AudioManager audioManager = AudioManager.getInstance();
                            audioManager.toggleMasterMute();

                            Reference<UIText> text = button.getLabelText();
                            if (Reference.isValid(text)) {
                                if (audioManager.isMasterMuted()) {
                                    text.get().setText("Unmute");
                                } else {
                                    text.get().setText("Mute");
                                }
                            }
                        });
        IUIBuildHandler mute = uiManager.buildWithChildrenRightOf(muteTitle, muteButton);

        // Volume:
        UITextRect sliderTitle = new UITextRect("Volume:");
        UISlider slider =
                new UISlider(
                        AudioManager.getInstance().getMasterVolume(),
                        (__, value) -> AudioManager.getInstance().setMasterVolume(value));
        IUIBuildHandler volume = uiManager.buildWithChildrenRightOf(sliderTitle, slider);

        // Back:
        UIButton back =
                new UIButton(
                        "Back",
                        (__, ___) -> {
                            switchToState(State.MENU);
                        });

        // Combined:
        uiManager.buildVerticalUi(mAudioContainer, 0.3f, 0, 1f, title, mute, volume, back);
    }

    /** Generate the contents of {@link #mGraphicsContainer}. */
    private void generateGraphics() {
        final UIManager uiManager = UIManager.getInstance();

        UITextRect title = new UITextRect("Graphics");

        IUIBuildHandler windowed =
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
                                "Fullscreen"));

        UIButton back =
                new UIButton(
                        "Back",
                        (__, ___) -> {
                            switchToState(State.MENU);
                        });

        uiManager.buildVerticalUi(mGraphicsContainer, 0.3f, 0, 1f, title, windowed, back);
    }

    @Override
    public void onAwake() {
        mMenuContainer = new GameObject("pause_container", new TransformUI());
        getGameObject().addChild(mMenuContainer);

        mAudioContainer = new GameObject("audio_container", new TransformUI());
        mGraphicsContainer = new GameObject("graphics_container", new TransformUI());

        generateMenu();
        generateAudio();
        generateGraphics();
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (GameActions.TOGGLE_PAUSE.isJustActivated()) {
            if (mCurrentState == State.AUDIO || mCurrentState == State.GRAPHICS) {
                switchToState(State.MENU);
            } else {
                mReturnAction.run();
            }
        }
    }

    @Override
    protected void onDestroy() {}
}
