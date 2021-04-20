package org.dragonskulle.game.player;

import java.util.List;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.input.IButtonEvent;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class PauseMenu extends Component implements IOnAwake, IFixedUpdate, IFrameUpdate, IOnStart {

	@Getter @Setter private boolean mPaused = false;
	
	private Reference<NetworkManager> mNetworkManager;
	
	private GameObject mContainer;
	//private UIButton mButton;

	public PauseMenu(NetworkManager networkManager) {
		mNetworkManager = networkManager.getReference(NetworkManager.class);
	}

	@Override
	public void onStart() {
	}
	
	@Override
	public void onAwake() {
		
		mContainer = new GameObject("pause_container", new TransformUI());
		getGameObject().addChild(mContainer);
		
		UIButton button = new UIButton("test", (pbutton, pdeltatime) -> {
			System.out.println("pbutton: " + pbutton);
			System.out.println("pdeltatime: " + pdeltatime);
			
			if(Reference.isValid(mNetworkManager)) {
				NetworkManager networkManager = mNetworkManager.get(); 
				
				System.out.println("networkManager " + networkManager);
				if(networkManager.getClientManager() != null) {
					System.out.println("DISCONNECT");
					networkManager.getClientManager().disconnect();
				} else {
					System.out.println("no getClientManager");
				}
				
				if(networkManager.getServerManager() != null) {
					networkManager.getServerManager().destroy();
				}
			} else {
				System.out.println("cant mNetworkManager");
			}
			
			return;
		});
		
		mContainer.addComponent(button);
		mContainer.setEnabled(false);
		
		/*
		mContainer = new GameObject("pause_conatiner", new TransformUI());
		getGameObject().addChild(mContainer);
		
		NetworkManager t = getNetworkManager();
		//System.out.println("t: " + t);
		
		UIButton button = new UIButton("test", (pbutton, pdeltatime) -> {
			System.out.println("pbutton: " + pbutton);
			System.out.println("pdeltatime: " + pdeltatime);
			
			if(t != null) {
				System.out.println("getNetworkManager: " + getNetworkManager());
				if(t.getClientManager() != null) {
					System.out.println("DISCONNECT");
					t.getClientManager().disconnect();
				} else {
					System.out.println("cant getNetworkManager().getClientManager()");
				}
			} else {
				System.out.println("cant getNetworkManager");
			}
			
			return;
		});
		
		mContainer.addComponent(button);
		*/
		
		/*
		mContainer = new GameObject("pause_conatiner", new TransformUI());
		getGameObject().addChild(mContainer);
		
		final TransformUI transform = getGameObject().getTransform(TransformUI.class);
        transform.setMaintainAspect(false);
        transform.setParentAnchor(0.37f, 0.08f, 0.37f, 0.08f);
        transform.setMargin(-0.285f, -0.034f, 0.285f, 0.034f);
		
        mButton = new UIButton("test");
        mContainer.addChild(mButton.getGameObject());
        */
		
        //getGameObject().addComponent(mButton);
        
        //mButton.getGameObject().setEnabled(true);
        
		/*
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
                                                                "aiPlayer"));

                                log.warning("Created ai");
                            }));
        });
		 */
		
        
        
		/*
		final TransformUI transform = getGameObject().getTransform(TransformUI.class);
        transform.setMaintainAspect(false);
        transform.setParentAnchor(0.37f, 0.08f, 0.37f, 0.08f);
        transform.setMargin(-0.285f, -0.034f, 0.285f, 0.034f);

        UITextRect textRect = new UITextRect("Tokens: 0");

        getGameObject().addComponent(textRect);
        textRect.setRectTexture(GameUIAppearance.getInfoBoxTexture());

        mTextRect = textRect.getReference(UITextRect.class);
        */
	}
	
	/**
     * Build a menu, it is disabled by default.
     *
     * @param mButtonChildren the buttons to be built
     * @return reference to the built menu.
     *
    private Reference<GameObject> buildMenu(List<UITextButtonFrame> mButtonChildren) {
        /*
    	UIManager manager = UIManager.getInstance();
        UIButton[] buttons =
                mButtonChildren.stream()
                        .map(
                                child ->
                                        new UIButton(
                                                child.getText(),
                                                child.getOnClick(),
                                                child.isStartEnabled()))
                        .toArray(UIButton[]::new);

        final GameObject built_menu = new GameObject("build_menu", new TransformUI());
        manager.buildVerticalUi(built_menu, mOffsetToTop, 0f, 1f, buttons);
        getGameObject().addChild(built_menu);
        built_menu.setEnabled(false);
        return built_menu.getReference();
        *
    }
    */

	@Override
	protected void onDestroy() {}

	@Override
	public void fixedUpdate(float deltaTime) {		
		/*
		if(GameActions.TOGGLE_PAUSE.isJustActivated()) {
			mPaused = !mPaused;
			mContainer.setEnabled(mPaused);
			
			System.out.println("Is now: " + mPaused);
		}
		*/
	}

	@Override
	public void frameUpdate(float deltaTime) {
		if(GameActions.TOGGLE_PAUSE.isJustActivated()) {
			mPaused = !mPaused;
			mContainer.setEnabled(mPaused);
			
			System.out.println("Is now: " + mPaused);
		}
	}

}
