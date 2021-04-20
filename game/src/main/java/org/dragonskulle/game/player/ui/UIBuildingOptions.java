/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.PredefinedBuildings;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.*;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;

/**
 * The UI Component to display the pre-defined placeable buildings.
 *
 * @author Oscar L
 */
@Log
@Accessors(prefix = "m")
public class UIBuildingOptions extends Component implements IOnStart {
    private List<BuildingDescriptor> mBuildingsCanPlace;
    @Getter
    private Building mSelectedBuilding;
    @Getter
    private final UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    @Setter
    private Reference<GameObject> mPastOptionsRef;
    private Reference<GameObject> mPossibleBuildingComponent;
    @Setter
    private Reference<UIButton> mPreviousLock = new Reference<>(null);
    @Setter
    private BuildingDescriptor mSelectedBuildingDescriptor;
    private IUIBuildHandler[] mBuildingsCanPlaceButtons;
    private List<Reference<GameObject>> mBuildingsCanPlaceButtonsReferences;

    /**
     * Constructor.
     *
     * @param mGetPlayer the callback to get the player from HumanPlayer.
     */
    public UIBuildingOptions(UIMenuLeftDrawer.IGetPlayer mGetPlayer) {
        this.mGetPlayer = mGetPlayer;
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy.
     */
    @Override
    protected void onDestroy() {}

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        mPossibleBuildingComponent =
                getGameObject()
                        .buildChild(
                                "possible_buildings",
                                new TransformUI(true),
                                (self) -> {
                                    UIManager manager = UIManager.getInstance();
                                    mBuildingsCanPlace = PredefinedBuildings.getAll();
                                    mBuildingsCanPlaceButtons = mBuildingsCanPlace.stream().map(this::buildPredefinedBuildingBox).toArray(IUIBuildHandler[]::new);
                                    mBuildingsCanPlaceButtonsReferences = manager.buildGridUI(4, 2,
                                            0.05f,
                                            0.25f,
                                            0.45f,
                                            0.1f,
                                            mBuildingsCanPlaceButtons); //will filter the references by disabled on frame update
                                });
    }

    private IUIBuildHandler buildPredefinedBuildingBox(BuildingDescriptor descriptor) {
        return (go) -> {
            go.getTransform(TransformUI.class).setPivotOffset(0.5f, 0f);
            UIButton but = new UIButton((me, ___) -> {
                if (Reference.isValid((mPreviousLock))) {
                    mPreviousLock.get().setLockPressed(false);
                }
                setPreviousLock(me.getReference(UIButton.class));
                setSelectedBuildingDescriptor(descriptor);
                me.setLockPressed(true);
            });
            but.setRectTexture(GameUIAppearance.getSquareButtonTexture());
            go.addComponent(but);
            go.buildChild(
                    "sym",
                    new TransformUI(true),
                    (handle) -> {
                        handle.getTransform(TransformUI.class).setParentAnchor(0.25f);
                        if (descriptor.getIconPath() != null) {
                            handle.addComponent(new UIFlatImage(new SampledTexture("cat_material.jpg"), false));
                        } else {
                            handle.addComponent(new UIFlatImage(new SampledTexture(descriptor.getIconPath()), false));
                        }
                    });
        };
    }

    private void buildOptions(List<String> mOptionsChildren) {
        Reference<GameObject> ref =
                mPossibleBuildingComponent
                        .get()
                        .buildChild(
                                "built_upgradable_options",
                                new TransformUI(),
                                (root) -> {
                                    new UIDropDown(
                                            0,
                                            (drop) ->
                                                    log.warning(
                                                            "will place building, "
                                                                    + drop.getSelectedOption()),
                                            String.valueOf(mOptionsChildren.stream()));
                                });
        ref.get().setEnabled(false);
        replaceOptions(ref);
    }

    private void replaceOptions(Reference<GameObject> newOptions) {
        //        log.warning("replacing building options");
        if (mPastOptionsRef != null && !mPastOptionsRef.equals(newOptions)) {
            mPastOptionsRef.get().destroy();
        }
        if (getGameObject().isEnabled()) {
            newOptions.get().setEnabled(true);
        }
        mPastOptionsRef = newOptions;
    }

}
