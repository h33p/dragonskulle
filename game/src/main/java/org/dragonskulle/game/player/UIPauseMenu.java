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
public class UIPauseMenu extends Component
        implements IOnAwake, IFixedUpdate, IFrameUpdate, IOnStart {

    private boolean mUnpause = false;

    private Reference<NetworkManager> mNetworkManager;

    private GameObject mContainer;
    // private UIButton mButton;

    public UIPauseMenu(NetworkManager networkManager) {
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

        UIButton resume =
                new UIButton(
                        "Resume",
                        (__, ___) -> {
                            mUnpause = true;
                        });

        UIButton settings =
                new UIButton(
                        "Settings",
                        (__, ___) -> {
                            System.out.println("Settings.");
                        });

        UIButton exit =
                new UIButton(
                        "Exit",
                        (__, ___) -> {
                            if (Reference.isValid(mNetworkManager)) {
                                NetworkManager networkManager = mNetworkManager.get();

                                if (networkManager.getClientManager() != null) {
                                    networkManager.getClientManager().disconnect();
                                } else if (networkManager.getServerManager() != null) {
                                    networkManager.getServerManager().destroy();
                                }
                            } else {
                                log.severe("Unable to get network manager.");
                            }
                        });

        uiManager.buildVerticalUi(mContainer, 0.3f, 0, 1f, resume, settings, exit);
    }

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
