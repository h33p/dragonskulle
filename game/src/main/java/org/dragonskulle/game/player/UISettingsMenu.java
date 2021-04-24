package org.dragonskulle.game.player;

import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIDropDown;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;
import org.dragonskulle.ui.UISlider;
import org.dragonskulle.ui.UITextRect;

public class UISettingsMenu extends Component implements IOnAwake, IFrameUpdate {

	/** Contains the action to execute when the user requests to leave the settings menu. */
	public static interface Back {
		public void run();
	}
	
	/** The action to execute when the user requests to leave the settings menu. */
	private Back mReturnAction;

	private static enum State {
		MENU,
		AUDIO,
		GRAPHICS
	}
	
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
	 * @param returnAction The action to be executed when the user requests to leave the settings menu.
	 */
	public UISettingsMenu(Back returnAction) {
		mReturnAction = returnAction;
	}
	
	private void switchToState(State state) {
		if(mCurrentState == state) return;
		
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
	
	private void generateMenu() {
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
        uiManager.buildVerticalUi(mMenuContainer, 0.3f, 0, 1f, resume, settings, exit);
    }
	
	private void generateAudio() {
		final UIManager uiManager = UIManager.getInstance();
		
		IUIBuildHandler volume = uiManager.buildWithChildrenRightOf(
                new UITextRect("Master volume:"),
                new UISlider(
                        AudioManager.getInstance().getMasterVolume(),
                        (__, value) -> AudioManager.getInstance().setMasterVolume(value)));
		
		UIButton back =
                new UIButton(
                        "Back",
                        (__, ___) -> {
                        	switchToState(State.MENU);
                        });
		
		uiManager.buildVerticalUi(mAudioContainer, 0.3f, 0, 1f, volume, back);
	}
	
	private void generateGraphics() {
		final UIManager uiManager = UIManager.getInstance();
		
		IUIBuildHandler windowed = uiManager.buildWithChildrenRightOf(
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
		
		uiManager.buildVerticalUi(mGraphicsContainer, 0.3f, 0, 1f, windowed, back);
	}
	
	@Override
	public void onAwake() {
		mMenuContainer = new GameObject("pause_container", new TransformUI());
        getGameObject().addChild(mMenuContainer);
        
        mAudioContainer = new GameObject("audio_container", new TransformUI());
        mGraphicsContainer = new GameObject("audio_container", new TransformUI());
        
        generateMenu();
        generateAudio();
        generateGraphics();
	}

	@Override
    public void frameUpdate(float deltaTime) {
        if (GameActions.TOGGLE_PAUSE.isJustActivated()) {
        	if(mCurrentState == State.AUDIO || mCurrentState == State.GRAPHICS) {
        		switchToState(State.MENU);
        	} else {
        		mReturnAction.run();        		
        	}        	
        }
    }
	
	@Override
	protected void onDestroy() {
	}
	
}
