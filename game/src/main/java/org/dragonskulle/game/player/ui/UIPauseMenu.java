/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.joml.Vector4f;

/**
 * A component that displays a pause menu- allowing access to a {@link UISettingsMenu} and gives an
 * option to leave the current game.
 *
 * @author Craig Wilbourne
 */
@Log
public class UIPauseMenu extends Component implements IOnAwake, IFrameUpdate {

    /** The possible states the settings menu can be in. */
    private static enum State {
        MENU,
        SETTINGS
    }

    /** The current state the pause menu is in. */
    private State mCurrentState = State.MENU;

    /** The {@link NetworkManager} being used. */
    private Reference<NetworkManager> mNetworkManager;

    /** Contains the pause menu. */
    private GameObject mMenuContainer;
    /** Contains the settings menu. */
    private GameObject mSettingsContainer;
    /** Used to grey out the background. */
    private UIRenderable mBackground;

    private GameObject mCamera;
    
    /**
     * Create a pause menu.
     *
     * @param networkManager The {@link NetworkManager} being used.
     * @param cameraRig 
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

    private void togglePause() {
    	// Either leave or enter the pause menu.
        mMenuContainer.setEnabled(!mMenuContainer.isEnabled());
        // Grey out the background if entering, or disable it if leaving.
        mBackground.setEnabled(mMenuContainer.isEnabled());
        // If the menu pause menu is enabled, disable the camera.
        mCamera.setEnabled(!mMenuContainer.isEnabled());
        
        for (Scene scene : Engine.getInstance().getActiveScenes()) {
    		HumanPlayer humanPlayer = scene.getSingleton(HumanPlayer.class);
        	if(humanPlayer == null) continue;
        	humanPlayer.setScreenOn(Screen.DEFAULT_SCREEN);
        	
        	//Reference<UIMenuLeftDrawer> menuDraw = humanPlayer.getMenuDrawer();
        	//if(!Reference.isValid(menuDraw)) continue;
        	//menuDraw.get().setEnabled(!mMenuContainer.isEnabled());
        	
        	//humanPlayer.setEnabled(!mMenuContainer.isEnabled());
        	System.out.println("Success.");
		}  
    }
    
    /** Generate the contents of {@link #mMenuContainer}. */
    private void generateMenu() {
        UIButton resume =
                new UIButton(
                        "Resume",
                        (__, ___) -> {
                            togglePause();
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

        UIManager.getInstance()
                .buildVerticalUi(mMenuContainer, 0.3f, 0, 1f, resume, settings, exit);
    }

    @Override
    public void onAwake() {
        // Create the GameObject that will hold all of the menu contents.
        mMenuContainer = new GameObject("pause_container", false, new TransformUI());
        getGameObject().addChild(mMenuContainer);

        // Create a settings menu.
        mSettingsContainer =
                new GameObject(
                        "settings_container",
                        false,
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

    @Override
    protected void onDestroy() {}
}
