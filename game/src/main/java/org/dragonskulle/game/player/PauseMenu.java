/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;

@Log
public class PauseMenu extends Component implements IOnAwake, IFixedUpdate, IFrameUpdate, IOnStart {

    private boolean mUnpause = false;

    private Reference<NetworkManager> mNetworkManager;

    private GameObject mContainer;
    // private UIButton mButton;

    public PauseMenu(NetworkManager networkManager) {
        mNetworkManager = networkManager.getReference(NetworkManager.class);
    }

    @Override
    public void onStart() {}

    /*
    * final UIManager uiManager = UIManager.getInstance();

          uiManager.buildVerticalUi(
                  mainUi,
                  0.05f,
                  0,
                  MENU_BASEWIDTH,
                  new UIButton(
                          "Join Game",
                          (__, ___) -> {
                              mainUi.setEnabled(false);
                              joinUi.setEnabled(true);
                              hostUi.setEnabled(false);
                          }),
                  new UIButton(
                          "Host Game",
                          (__, ___) -> {
                              mainUi.setEnabled(false);
                              hostUi.setEnabled(true);
                          }),
                  new UIButton(
                          "Settings",
                          (__, ___) -> {
                              mainUi.setEnabled(false);
                              settingsUI.setEnabled(true);
                          }),
                  new UIButton("Quit", (__, ___) -> Engine.getInstance().stop()),
                  new UIButton(
                          "Quick Reload",
                          (__, ___) -> {
                              sReload = true;
                              Engine.getInstance().stop();
                          }));
    */

    @Override
    public void onAwake() {

        final UIManager uiManager = UIManager.getInstance();

        mContainer = new GameObject("pause_container", new TransformUI());
        getGameObject().addChild(mContainer);

        // Make button activate TOGGLE_PAUSE

        UIButton exit = new UIButton("Exit", (__, ___) -> {
        	if(Reference.isValid(mNetworkManager)) {
        		NetworkManager networkManager = mNetworkManager.get();

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
        
        
        uiManager.buildVerticalUi(
                mContainer,
                0.05f,
                0,
                0.2f,
                new UIButton(
                        "Resume",
                        (__, ___) -> {
                            mUnpause = true;
                        }),
                new UIButton(
                        "Settings",
                        (__, ___) -> {
                            // mainUi.setEnabled(false);
                            // settingsUI.setEnabled(true);
                            System.out.println("Settings.");
                        }),
                exit
                
        		);

        /*

        UIButton button = new UIButton("Exit", (__, ___) -> {
        	if(Reference.isValid(mNetworkManager)) {
        		NetworkManager networkManager = mNetworkManager.get();

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

        */

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

        // getGameObject().addComponent(mButton);

        // mButton.getGameObject().setEnabled(true);

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
     *     <p>private Reference<GameObject> buildMenu(List<UITextButtonFrame> mButtonChildren) { /*
     *     UIManager manager = UIManager.getInstance(); UIButton[] buttons =
     *     mButtonChildren.stream() .map( child -> new UIButton( child.getText(),
     *     child.getOnClick(), child.isStartEnabled())) .toArray(UIButton[]::new);
     *     <p>final GameObject built_menu = new GameObject("build_menu", new TransformUI());
     *     manager.buildVerticalUi(built_menu, mOffsetToTop, 0f, 1f, buttons);
     *     getGameObject().addChild(built_menu); built_menu.setEnabled(false); return
     *     built_menu.getReference();
     *     <p>}
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
        if (mUnpause) {
            mContainer.setEnabled(false);
            mUnpause = false;
        } else if (GameActions.TOGGLE_PAUSE.isJustActivated()) {
            mContainer.setEnabled(!mContainer.isEnabled());
        }
    }
}
