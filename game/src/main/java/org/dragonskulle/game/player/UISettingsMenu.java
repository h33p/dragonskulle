package org.dragonskulle.game.player;

import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;
import org.dragonskulle.ui.UISlider;
import org.dragonskulle.ui.UITextRect;

public class UISettingsMenu extends Component implements IOnAwake, IOnStart {

	public static interface Back {
		public void run();
	}
	
	private GameObject mMenuContainer;
	
	private GameObject mAudioContainer;
	
	private Back mReturnAction;
	
	public UISettingsMenu(Back returnAction) {
		mReturnAction = returnAction;
	}
	
	private void generateMenu() {
    	UIButton resume =
                new UIButton(
                        "Audio",
                        (__, ___) -> {
                        	mMenuContainer.setEnabled(false);
                        	mAudioContainer.setEnabled(true);
                        	getGameObject().addChild(mAudioContainer);
                        });

        UIButton settings =
                new UIButton(
                        "Graphics",
                        (__, ___) -> {
                        	System.out.println("Graphics.");
                        });

        UIButton exit =
                new UIButton(
                        "Back",
                        (__, ___) -> {
                        	mReturnAction.run();
                        });

        final UIManager uiManager = UIManager.getInstance();
        uiManager.buildVerticalUi(mMenuContainer, 0.3f, 0, 1f, resume, settings, exit);
    }
	
	private void generateAudio() {
		final UIManager uiManager = UIManager.getInstance();
		
		UIButton back =
                new UIButton(
                        "Back",
                        (__, ___) -> {
                        	mMenuContainer.setEnabled(true);
                        	mAudioContainer.setEnabled(false);
                        	getGameObject().removeChild(mAudioContainer);
                        });
		
		IUIBuildHandler t = uiManager.buildWithChildrenRightOf(
                new UITextRect("Master volume:"),
                new UISlider(
                        AudioManager.getInstance().getMasterVolume(),
                        (__, val) -> AudioManager.getInstance().setMasterVolume(val)));
		
		uiManager.buildVerticalUi(mAudioContainer, 0.3f, 0, 1f, back, t);
	}
	
	@Override
	public void onAwake() {
		mMenuContainer = new GameObject("pause_container", false, new TransformUI());
        getGameObject().addChild(mMenuContainer);
        mMenuContainer.setEnabled(false);
        
        mAudioContainer = new GameObject("audio_container", false, new TransformUI());
        //getGameObject().addChild(mAudioContainer);
        mAudioContainer.setEnabled(false);
        
        //getGameObject().removeChild(mAudioContainer);
        
        generateMenu();
        generateAudio();
	}

	@Override
	protected void onDestroy() {
	}

	@Override
	public void onStart() {
		System.out.println("start");
	}
	
	/*
	 * 
	 * uiManager.buildVerticalUi(
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
	 */
	
}
