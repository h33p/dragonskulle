/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.lambda.LambdaFixedUpdate;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.PredefinedBuildings;
import org.dragonskulle.game.player.network_data.BuildData;
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
public class UIBuildingOptions extends Component implements IOnStart, IFixedUpdate {
    private List<BuildingDescriptor> mBuildingsCanPlace;
    @Getter
    private Building mSelectedBuilding;
    @Setter
    private BuildingDescriptor mSelectedBuildingDescriptor;
    @Setter
    private Reference<GameObject> mPastOptionsRef;
    private Reference<GameObject> mPossibleBuildingComponent;
    @Setter
    private Reference<UIButton> mPreviousLock = new Reference<>(null);
    private List<IUIBuildHandler> mBuildingsCanPlaceButtons;
    @Setter
    private Reference<GameObject> mVisibleDescriptorHint = new Reference<>(null);
    private List<Reference<UIButton>> mBuildingsCanPlaceButtonsReferences;
    @Getter
    private final UIShopSection mParent;
    @Setter
    private boolean mStickyHint = false;
    private Reference<Player> mPlayerReference;
    @Getter
    @Setter
    private int mTokens = 0;

    /**
     * Constructor.
     *
     * @param mParent its parent, this avoids passing tonnes of callbacks
     */
    public UIBuildingOptions(UIShopSection mParent) {
        this.mParent = mParent;
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
                                    mBuildingsCanPlaceButtons =
                                            mBuildingsCanPlace.stream()
                                                    .map(this::buildPredefinedBuildingBox)
                                                    .collect(Collectors.toList());


                                    manager.buildGridUI(
                                            self,
                                            4,
                                            0f,
                                            0.3f,
                                            0.55f,
                                            0.15f,
                                            mBuildingsCanPlaceButtons);
                                    // will filter the references by disabled on frame update

                                    self.buildChild(
                                            "build_selected_tile",
                                            new TransformUI(true),
                                            (me) -> {
                                                UIButton button =
                                                        new UIButton(
                                                                "Build Selected",
                                                                (__, ___) -> {
                                                                    if (getParent()
                                                                            .getParent()
                                                                            .mGetHexChosen
                                                                            .getHex()
                                                                            != null) {
                                                                        Reference<Player> player =
                                                                                getParent()
                                                                                        .getParent()
                                                                                        .mGetPlayer
                                                                                        .getPlayer();
                                                                        if (Reference.isValid(
                                                                                player)) {
                                                                            player.get()
                                                                                    .getClientBuildRequest()
                                                                                    .invoke(
                                                                                            new BuildData(
                                                                                                    getParent()
                                                                                                            .getParent()
                                                                                                            .mGetHexChosen
                                                                                                            .getHex(),
                                                                                                    PredefinedBuildings
                                                                                                            .getIndex(
                                                                                                                    mSelectedBuildingDescriptor)));
                                                                        }

                                                                        getParent()
                                                                                .getParent()
                                                                                .mSetHexChosen
                                                                                .setHex(null);
                                                                        getParent()
                                                                                .getParent()
                                                                                .mSetBuildingChosen
                                                                                .setBuilding(null);
                                                                        getParent()
                                                                                .getParent()
                                                                                .mNotifyScreenChange
                                                                                .call(
                                                                                        Screen
                                                                                                .MAP_SCREEN);
                                                                    }
                                                                });
                                                me.addComponent(button);
                                                TransformUI transformUI =
                                                        me.getTransform(TransformUI.class);
                                                transformUI.setParentAnchor(
                                                        0.16f, 0.6f, 0.86f, 1.1f);
                                            });
                                });
    }

    private IUIBuildHandler buildPredefinedBuildingBox(BuildingDescriptor descriptor) {
        return (go) -> {
            go.getTransform(TransformUI.class).setPivotOffset(0.5f, 0f);
            UIButton but =
                    new UIButton(
                            "",
                            (me, ___) -> {
                                if (Reference.isValid((mPreviousLock))) {
                                    mPreviousLock.get().setLockPressed(false);
                                }
                                setStickyHint(true);
                                setPreviousLock(me.getReference(UIButton.class));
                                setSelectedBuildingDescriptor(descriptor);
                                me.setLockPressed(true);
                            },
                            null,
                            null,
                            (me, ___) ->
                                    showDescriptorHint(descriptor), // will add parent information
                            (__, ___) -> {
                                if (!mStickyHint) hideDescriptorHint();
                            });

            go.addComponent(
                    new LambdaFixedUpdate(
                            (dt) -> {
                                if (descriptor.getCost() > mTokens) {
                                    but.disable();
                                } else {
                                    but.enable();
                                }
                            }));

            but.setRectTexture(GameUIAppearance.getSquareButtonTexture());
            go.addComponent(but);
            go.buildChild(
                    "sym",
                    new TransformUI(),
                    (handle) -> {
                        handle.getTransform(TransformUI.class).setParentAnchor(0.25f);
                        if (descriptor.getIconPath() == null) {
                            handle.addComponent(
                                    new UIFlatImage(new SampledTexture("cat_material.jpg"), false));
                        } else {
                            handle.addComponent(
                                    new UIFlatImage(
                                            new SampledTexture(descriptor.getIconPath()), false));
                        }
                    });
        };
    }

    private void hideDescriptorHint() {
        if (Reference.isValid(mVisibleDescriptorHint)) {
            mVisibleDescriptorHint.get().destroy();
            setVisibleDescriptorHint(new Reference<>(null));
        }
    }

    private void showDescriptorHint(BuildingDescriptor descriptor) {
        hideDescriptorHint();
        log.info("showing hint");
        Reference<GameObject> go =
                getGameObject()
                        .buildChild(
                                "descriptor_hint",
                                new TransformUI(),
                                (self) -> {
                                    TransformUI hintTransform =
                                            self.getTransform(TransformUI.class);
                                    hintTransform.setParentAnchor(-0.05f, -0.7f, 1.05f, -0.05f);

                                    UITextRect component =
                                            new UITextRect(
                                                    String.format(
                                                            "%s\nAttack Strength: %d\nDefence Strength: %d\nGeneration Value: %d\nCost: %d",
                                                            descriptor.getName().toUpperCase(),
                                                            descriptor.getAttack(),
                                                            descriptor.getDefence(),
                                                            descriptor.getTokenGeneration(),
                                                            descriptor.getCost()));
                                    component.setRectTexture(
                                            UIManager.getInstance()
                                                    .getAppearance()
                                                    .getHintTexture()
                                                    .clone());
                                    self.addComponent(component);
                                });
        setVisibleDescriptorHint(go);
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        ensurePlayerReference();
        updateTokens();
        //        for (int i = 0; i < PredefinedBuildings.getAll().size(); i++) {
        //            BuildingDescriptor buildingDescriptor = PredefinedBuildings.get(i);
        //            Reference<UIButton> buttonReference =
        // mBuildingsCanPlaceButtonsReferences.get(i);
        //            if (Reference.isValid(buttonReference)) {
        //                if (buildingDescriptor.getCost() < getTokens()) {
        //                    buttonReference.get().enable();
        //                } else {
        //                    buttonReference.get().disable();
        //                }
        //            }
        //        }
    }

    private void updateTokens() {
        if(Reference.isValid(mPlayerReference)) {
            setTokens(mPlayerReference.get().getTokens().get());
        }
    }

    private void ensurePlayerReference() {
        if (Reference.isInvalid(mPlayerReference)) {
            mPlayerReference = getParent().getParent().mGetPlayer.getPlayer();
        }
    }
}
