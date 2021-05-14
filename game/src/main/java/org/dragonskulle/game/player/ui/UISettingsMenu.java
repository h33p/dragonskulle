/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import java.util.ArrayList;
import java.util.List;
import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.input.Cursor;
import org.dragonskulle.renderer.Renderer;
import org.dragonskulle.renderer.RendererException;
import org.dragonskulle.renderer.RendererSettings;
import org.dragonskulle.renderer.VBlankMode;
import org.dragonskulle.settings.Settings;
import org.dragonskulle.ui.BuildHandlerInfo;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIAppearance;
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
    public static interface IOnBack {
        /** Contains the code to execute on a back action. */
        public void run();
    }

    /** The action to execute when the user requests to leave the settings menu. */
    private IOnBack mReturnAction;

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

    private List<Integer> mSupportedMSAAModes = new ArrayList<>();

    private final UIDropDown mMSAADropDown =
            new UIDropDown(
                    0,
                    (drop) -> {
                        if (mSupportedMSAAModes == null || !drop.hasSelection()) {
                            return;
                        }
                        int val = drop.getSelected();

                        Renderer rend = Engine.getInstance().getGLFWState().getRenderer();

                        RendererSettings newSettings =
                                new RendererSettings(rend.getRendererSettings());

                        newSettings.setMSAACount(mSupportedMSAAModes.get(val));

                        try {
                            rend.setSettings(newSettings);
                            rend.getRendererSettings().writeSettings(Settings.getInstance());
                        } catch (RendererException e) {
                            e.printStackTrace();
                        }

                        updateGraphicsSettings();
                    });

    private VBlankMode[] mSupportedVSyncModes;

    private final UIDropDown mVSyncDropDown =
            new UIDropDown(
                    0,
                    (drop) -> {
                        if (mSupportedMSAAModes == null || !drop.hasSelection()) {
                            return;
                        }
                        int val = drop.getSelected();

                        Renderer rend = Engine.getInstance().getGLFWState().getRenderer();

                        RendererSettings newSettings =
                                new RendererSettings(rend.getRendererSettings());

                        newSettings.setVBlankMode(mSupportedVSyncModes[val]);

                        try {
                            rend.setSettings(newSettings);
                            rend.getRendererSettings().writeSettings(Settings.getInstance());
                        } catch (RendererException e) {
                            e.printStackTrace();
                        }

                        updateGraphicsSettings();
                    });

    /**
     * Create a new settings menu component.
     *
     * @param returnAction The action to be executed when the user requests to leave the settings
     *     menu.
     */
    public UISettingsMenu(IOnBack returnAction) {
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
                            updateGraphicsSettings();
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
                        AudioManager.getInstance().isMasterMuted() ? "Unmute" : "Mute",
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
        Settings settingsInstance = Settings.getInstance();

        // Volume:
        UITextRect sliderTitle = new UITextRect("Volume:");
        UISlider slider =
                new UISlider(
                        AudioManager.getInstance().getMasterVolume(),
                        (__, value) -> {
                            settingsInstance.saveValue(AudioManager.SETTINGS_VOLUME_STRING, value);
                            AudioManager.getInstance().setMasterVolume(value);
                        },
                        (__, ___) -> settingsInstance.save() // on button release
                        );
        IUIBuildHandler volume = uiManager.buildWithChildrenRightOf(sliderTitle, slider);

        // Back:
        UIButton back =
                new UIButton(
                        "Back",
                        (__, ___) -> {
                            switchToState(State.MENU);
                        });

        // Combined:
        BuildHandlerInfo titleInfo = new BuildHandlerInfo(title, 0);
        BuildHandlerInfo muteInfo = new BuildHandlerInfo(mute, -0.15f);
        BuildHandlerInfo volumeInfo = new BuildHandlerInfo(volume, -0.15f);
        BuildHandlerInfo backInfo = new BuildHandlerInfo(back, 0f);

        uiManager.buildVerticalUi(
                mAudioContainer, 0.3f, 0, 1f, titleInfo, muteInfo, volumeInfo, backInfo);
    }

    /** Update the graphics settings. */
    private void updateGraphicsSettings() {
        Renderer rend = Engine.getInstance().getGLFWState().getRenderer();

        int msaaModes = rend.getSupportedMSAASamples();

        ArrayList<String> mTmpList = new ArrayList<>();

        int selectedSamples = rend.getRendererSettings().getMSAACount();

        int selectedIdx = 0;
        int supportedIdx = 0;

        for (int i = 0; i < 16; i++) {
            if (((msaaModes >> i) & 1) == 1) {
                if ((1 << i) == selectedSamples) {
                    selectedIdx = supportedIdx;
                }
                mTmpList.add(String.format("%dx", 1 << i));
                mSupportedMSAAModes.add(1 << i);
                supportedIdx++;
            }
        }

        mMSAADropDown.setOptions(mTmpList.stream().toArray(String[]::new));
        mMSAADropDown.setSelected(selectedIdx);

        mSupportedVSyncModes = rend.getVBlankModes();

        VBlankMode selectedMode = rend.getRendererSettings().getVBlankMode();

        mTmpList.clear();

        selectedIdx = 0;

        for (int i = 0; i < mSupportedVSyncModes.length; i++) {
            if (mSupportedVSyncModes[i] == selectedMode) {
                selectedIdx = i;
            }

            mTmpList.add(mSupportedVSyncModes[i] == null ? "" : mSupportedVSyncModes[i].getName());
        }

        mVSyncDropDown.setOptions(mTmpList.stream().toArray(String[]::new));
        mVSyncDropDown.setSelected(selectedIdx);
    }

    /** Generate the contents of {@link #mGraphicsContainer}. */
    private void generateGraphics() {
        final UIManager uiManager = UIManager.getInstance();

        UITextRect title = new UITextRect("Graphics");

        UIDropDown uiWindowedDropDown =
                new UIDropDown(
                        0,
                        (drop) ->
                                Engine.getInstance()
                                        .getGLFWState()
                                        .setFullscreen(drop.getSelected() == 1),
                        "Windowed",
                        "Fullscreen");

        IUIBuildHandler windowed =
                uiManager.buildWithChildrenRightOf(
                        new UITextRect("Fullscreen mode:"), uiWindowedDropDown);

        IUIBuildHandler vsync =
                uiManager.buildWithChildrenRightOf(new UITextRect("VSync mode:"), mVSyncDropDown);

        IUIBuildHandler msaa =
                uiManager.buildWithChildrenRightOf(new UITextRect("Anti-aliasing:"), mMSAADropDown);

        Settings settingsInstance = Settings.getInstance();

        UISlider uiSlider =
                new UISlider(
                        settingsInstance.retrieveFloat(Cursor.SETTINGS_STRING, 0.4f),
                        0.1f,
                        1f,
                        0.01f,
                        (__, value) -> { // on slider change
                            settingsInstance.saveValue(Cursor.SETTINGS_STRING, value);
                            Cursor.setCustomCursor(UIAppearance.getHoverCursor());
                        },
                        (__, ___) -> settingsInstance.save() // on button release
                        );

        IUIBuildHandler cursorScale =
                uiManager.buildWithChildrenRightOf(new UITextRect("Cursor Size:"), uiSlider);

        UIButton back = new UIButton("Back", (__, ___) -> switchToState(State.MENU));

        final float midOff = -0.15f;

        BuildHandlerInfo titleInfo = new BuildHandlerInfo(title, 0);
        BuildHandlerInfo windowedInfo = new BuildHandlerInfo(windowed, midOff);
        BuildHandlerInfo msaaInfo = new BuildHandlerInfo(msaa, midOff);
        BuildHandlerInfo vsyncInfo = new BuildHandlerInfo(vsync, midOff);
        BuildHandlerInfo cursorInfo = new BuildHandlerInfo(cursorScale, midOff);
        BuildHandlerInfo backInfo = new BuildHandlerInfo(back, 0f);

        uiManager.buildVerticalUi(
                mGraphicsContainer,
                0.3f,
                0,
                1f,
                titleInfo,
                windowedInfo,
                msaaInfo,
                vsyncInfo,
                cursorInfo,
                backInfo);
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
