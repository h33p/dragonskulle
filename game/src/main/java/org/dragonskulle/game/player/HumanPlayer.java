/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.*;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.camera.TargetMovement;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.map.FogOfWar;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.map.MapEffects.StandardHighlightType;
import org.dragonskulle.game.player.network_data.AttackData;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.ui.*;
import org.joml.Vector3f;

/**
 * This class will allow a user to interact with game.
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class HumanPlayer extends Component implements IFrameUpdate, IFixedUpdate, IOnStart {

    // All screens to be used
    private Screen mScreenOn = Screen.MAP_SCREEN;
    private Reference<GameObject> mMapScreen;

    private Reference<UIMenuLeftDrawer> mMenuDrawer;

    // Data which is needed on different screens
    @Getter @Setter private HexagonTile mHexChosen;

    @Getter @Setter private Reference<Building> mBuildingChosen = new Reference<>(null);

    // The player
    private Reference<Player> mPlayer;
    private int mLocalTokens = 0;

    private final int mNetID;
    private final Reference<NetworkManager> mNetworkManager;

    // Visual effects
    private Reference<MapEffects> mMapEffects;
    private Reference<FogOfWar> mFogOfWar;
    private boolean mVisualsNeedUpdate;
    private Reference<GameObject> mZoomSlider;
    private Reference<UITokenCounter> mTokenCounter;
    private Reference<GameObject> mTokenCounterObject;
    private HexagonTile mLastHexChosen;
    private Reference<GameObject> attack_button;
    private Reference<GameObject> sell_button;
    private Reference<GameObject> upgrade_button;
    private Reference<GameObject> place_button;

    private boolean mMovedCameraToCapital = false;

    /**
     * Create a {@link HumanPlayer}.
     *
     * @param networkManager The network manager.
     * @param netID The human player's network ID.
     */
    public HumanPlayer(Reference<NetworkManager> networkManager, int netID) {
        mNetworkManager = networkManager;
        mNetID = netID;
        mNetworkManager.get().getClientManager().registerSpawnListener(this::onSpawnObject);
    }

    @Override
    public void onStart() {

        mMapEffects =
                Scene.getActiveScene()
                        .getSingleton(MapEffects.class)
                        .getReference(MapEffects.class);

        mFogOfWar =
                Scene.getActiveScene().getSingleton(FogOfWar.class).getReference(FogOfWar.class);

        // Get the screen for map
        mMapScreen =
                // Creates a blank screen
                getGameObject().buildChild("map screen", new TransformUI(), (go) -> {});

        mZoomSlider =
                // Creates a blank screen
                getGameObject()
                        .buildChild(
                                "zoom_slider",
                                new TransformUI(true),
                                (go) -> {
                                    go.addComponent(new UILinkedScrollBar());
                                });

        Reference<GameObject> tmpRef =
                getGameObject()
                        .buildChild(
                                "menu_drawer",
                                new TransformUI(true),
                                (go) -> {
                                    mTokenCounterObject =
                                            go.buildChild(
                                                    "token_counter",
                                                    new TransformUI(true),
                                                    (self) -> {
                                                        UITokenCounter tokenCounter =
                                                                new UITokenCounter();
                                                        self.addComponent(tokenCounter);
                                                    });
                                    go.addComponent(
                                            new UIMenuLeftDrawer(
                                                    this::getBuildingChosen,
                                                    this::setBuildingChosen,
                                                    this::getHexChosen,
                                                    this::setHexChosen,
                                                    this::setScreenOn,
                                                    this::getPlayer));
                                });

        mTokenCounter = mTokenCounterObject.get().getComponent(UITokenCounter.class);
        mMenuDrawer = tmpRef.get().getComponent(UIMenuLeftDrawer.class);

        mVisualsNeedUpdate = true;
    }

    private Reference<Player> getPlayer() {
        return this.mPlayer;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        // Try getting the player if haven't already
        if (mPlayer == null) {
            NetworkManager manager = mNetworkManager.get();

            if (manager != null && manager.getClientManager() != null)
                mPlayer =
                        manager.getClientManager()
                                .getNetworkObjects()
                                .filter(Reference::isValid)
                                .map(Reference::get)
                                .filter(NetworkObject::isMine)
                                .map(NetworkObject::getGameObject)
                                .map(go -> go.getComponent(Player.class))
                                .filter(ref -> ref != null)
                                .findFirst()
                                .orElse(null);
        }

        if (!Reference.isValid(mPlayer)) return;

        if (!mMovedCameraToCapital) {
            TargetMovement targetRig = Scene.getActiveScene().getSingleton(TargetMovement.class);

            Building capital = mPlayer.get().getCapital();

            if (targetRig != null && capital != null) {
                log.info("MOVE TO CAPITAL BRUDDY!");
                targetRig.setTarget(capital.getGameObject().getTransform());
                mMovedCameraToCapital = true;
            }
        }

        if (mPlayer.get().hasLost()) {
            log.warning("You've lost your capital");
            setEnabled(false);
            return;
        }
        // Update token
        if (Reference.isValid(mPlayer)) {
            updateVisibleTokens();
        }
    }

    private void updateVisibleTokens() {
        mLocalTokens = mPlayer.get().getTokens().get();
        if (Reference.isValid(mTokenCounter)) {
            mTokenCounter.get().setLabelReference(mLocalTokens);
        } else {
            mTokenCounter = getGameObject().getComponent(UITokenCounter.class);
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        // Choose which screen to show

        if (Reference.isValid(mMenuDrawer)) {
            mMenuDrawer.get().setMenu(mScreenOn);
        }

        mapScreen();

        if (mVisualsNeedUpdate) updateVisuals();
    }

    /** This will choose what to do when the user can see the full map */
    private void mapScreen() {

        // Checks that its clicking something
        if (GameActions.LEFT_CLICK.isActivated()) {
            if (UIManager.getInstance().getHoveredObject() == null) {
                // And then select the tile
                Player player = mPlayer.get();
                if (player != null) {
                    HexagonMap component = player.getMap();
                    if (component != null) {
                        mLastHexChosen = mHexChosen;
                        mHexChosen = component.cursorToTile();
                    }
                }

                log.info("Human:Got the Hexagon to enter");

                if (mHexChosen != null) {
                    if (mHexChosen.hasBuilding()) {
                        Building building = mHexChosen.getBuilding();

                        if (hasPlayerGotBuilding(building.getReference(Building.class))
                                && mScreenOn != Screen.ATTACKING_SCREEN) {
                            mBuildingChosen = building.getReference(Building.class);
                            setScreenOn(Screen.BUILDING_SELECTED_SCREEN);
                        } else if (mScreenOn == Screen.ATTACKING_SCREEN) {

                            // Get the defending building
                            Building defendingBuilding = building;

                            // Checks the building can be attacked
                            boolean canAttack =
                                    mBuildingChosen.get().isBuildingAttackable(defendingBuilding);
                            if (canAttack) {
                                player.getClientAttackRequest()
                                        .invoke(
                                                new AttackData(
                                                        mBuildingChosen.get(),
                                                        defendingBuilding)); // Send Data
                            }

                            setScreenOn(Screen.MAP_SCREEN);
                            mBuildingChosen = null;
                        }
                    } else {
                        // Checks if cannot build here
                        if (mHexChosen.isClaimed()) {
                            log.info("Human:Cannot build");
                            mHexChosen = null;
                            mBuildingChosen = null;
                            return;
                        } else {
                            // If you can build
                            log.info("Human:Change Screen");
                            setScreenOn(Screen.TILE_SCREEN);
                        }
                    }
                }
            } else if (GameActions.RIGHT_CLICK.isActivated()) {
                HexagonTile tile = mPlayer.get().getMap().cursorToTile();
                Vector3f pos = new Vector3f(tile.getQ(), tile.getR(), tile.getS());
                log.info("[DEBUG] RCL Position From Camera : " + pos.toString());
            }
        }
    }

    /** AURI!! This updates what the user can see */
    private void updateVisuals() {
        if (mMapEffects == null || mPlayer == null) return;

        mVisualsNeedUpdate = false;

        Player player = mPlayer.get();

        if (player == null) return;

        if (attack_button == null)
            attack_button = mMenuDrawer.get().getButtonReferences().get("attack_button");
        if (sell_button == null)
            sell_button = mMenuDrawer.get().getButtonReferences().get("sell_button");
        if (upgrade_button == null)
            upgrade_button = mMenuDrawer.get().getButtonReferences().get("upgrade_button");
        if (place_button == null)
            place_button = mMenuDrawer.get().getButtonReferences().get("place_button");

        MapEffects effects = mMapEffects.get();
        if (!mPlayer.get().hasLost()) {

            if (Reference.isValid(mFogOfWar)) {
                mFogOfWar.get().setActivePlayer(mPlayer);
            }

            effects.setActivePlayer(mPlayer);

            switch (mScreenOn) {
                case MAP_SCREEN:
                    effects.setDefaultHighlight(true);
                    effects.setHighlightOverlay(null);
                    break;
                case BUILDING_SELECTED_SCREEN:
                    if (Reference.isValid(mMenuDrawer)) {
                        if (Reference.isValid(attack_button)) {
                            attack_button.get().setEnabled(true);
                            attack_button.get().getComponent(UIButton.class).get().enable();
                        }
                        if (Reference.isValid(sell_button)) {
                            sell_button.get().setEnabled(true);
                            sell_button.get().getComponent(UIButton.class).get().enable();
                        }
                        if (Reference.isValid(place_button)) {
                            place_button.get().setEnabled(false);
                            place_button.get().getComponent(UIButton.class).get().disable();
                        }
                        if (Reference.isValid(upgrade_button)) {
                            upgrade_button.get().setEnabled(true);
                            upgrade_button.get().getComponent(UIButton.class).get().enable();
                        }
                    }
                    effects.setDefaultHighlight(true);
                    effects.setHighlightOverlay(
                            (fx) -> highlightSelectedTile(fx, StandardHighlightType.VALID));
                    break;
                case TILE_SCREEN:
                    if (Reference.isValid(mMenuDrawer)) {
                        if (Reference.isValid(attack_button)) {
                            attack_button.get().setEnabled(false);
                            attack_button.get().getComponent(UIButton.class).get().disable();
                        }
                        if (Reference.isValid(sell_button)) {
                            sell_button.get().setEnabled(false);
                            sell_button.get().getComponent(UIButton.class).get().disable();
                        }
                        if (Reference.isValid(place_button)) {
                            place_button.get().setEnabled(true);
                            place_button.get().getComponent(UIButton.class).get().enable();
                        }
                        if (Reference.isValid(upgrade_button)) {
                            upgrade_button.get().setEnabled(false);
                            upgrade_button.get().getComponent(UIButton.class).get().disable();
                        }
                    }
                    effects.setDefaultHighlight(true);
                    effects.setHighlightOverlay(
                            (fx) -> highlightSelectedTile(fx, StandardHighlightType.PLAIN));
                    break;
                case ATTACK_SCREEN:
                    if (Reference.isValid(mMenuDrawer)) {
                        if (Reference.isValid(attack_button)) {
                            attack_button.get().setEnabled(true);
                            attack_button.get().getComponent(UIButton.class).get().enable();
                        }
                        if (Reference.isValid(sell_button)) {
                            sell_button.get().setEnabled(false);
                            sell_button.get().getComponent(UIButton.class).get().disable();
                        }
                        if (Reference.isValid(place_button)) {
                            place_button.get().setEnabled(false);
                            place_button.get().getComponent(UIButton.class).get().disable();
                        }
                        if (Reference.isValid(upgrade_button)) {
                            upgrade_button.get().setEnabled(false);
                            upgrade_button.get().getComponent(UIButton.class).get().disable();
                        }
                    }
                    effects.setDefaultHighlight(true);
                    effects.setHighlightOverlay(
                            (fx) -> highlightSelectedTile(fx, StandardHighlightType.VALID));
                    for (Building attackableBuilding :
                            mHexChosen.getBuilding().getAttackableBuildings()) {
                        effects.highlightTile(
                                attackableBuilding.getTile(),
                                StandardHighlightType.ATTACK.asSelection());
                    }
                    break;
                case STAT_SCREEN:
                    effects.setDefaultHighlight(true);
                    effects.setHighlightOverlay(null);
                    break;
            }
        }
    }

    private void highlightSelectedTile(MapEffects fx, StandardHighlightType highlight) {
        if (mHexChosen != null) {
            fx.highlightTile(mHexChosen, highlight.asSelection());
        }
    }

    /** Marks visuals to update whenever a new object is spawned */
    private void onSpawnObject(NetworkObject obj) {
        if (obj.getGameObject().getComponent(Building.class) != null) mVisualsNeedUpdate = true;
    }

    /**
     * AURI!!!
     *
     * @param newScreen
     */
    private void setScreenOn(Screen newScreen) {
        if (!newScreen.equals(mScreenOn) || (mLastHexChosen != mHexChosen))
            mVisualsNeedUpdate = true;
        mScreenOn = newScreen;
    }

    /**
     * A Method to check if the player owns that buildingSelectedView or not
     *
     * @param buildingToCheck The buildingSelectedView to check
     * @return true if the player owns the buildingSelectedView, false if not
     */
    private boolean hasPlayerGotBuilding(Reference<Building> buildingToCheck) {
        if (!Reference.isValid(buildingToCheck)) return false;

        return mPlayer.get().getOwnedBuilding(buildingToCheck.get().getTile()) != null;
    }
}
