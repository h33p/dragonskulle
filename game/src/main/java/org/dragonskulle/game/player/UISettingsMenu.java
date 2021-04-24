package org.dragonskulle.game.player;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;

public class UISettingsMenu extends Component implements IOnAwake {

	public static interface Back {
		public void run();
	}
	
	private GameObject mMenuContainer;
	
	private Back mReturnAction;
	
	public UISettingsMenu(Back returnAction) {
		mReturnAction = returnAction;
	}
	
	private void generateMenu() {
    	UIButton resume =
                new UIButton(
                        "Audio",
                        (__, ___) -> {
                        	System.out.println("Audio.");
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
	
	@Override
	public void onAwake() {
		mMenuContainer = new GameObject("pause_container", false, new TransformUI());
        getGameObject().addChild(mMenuContainer);
        mMenuContainer.setEnabled(false);
		
		generateMenu();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		
	}

}
