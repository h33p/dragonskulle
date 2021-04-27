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
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.PredefinedBuildings;
import org.dragonskulle.game.player.network_data.BuildData;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIFlatImage;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;

/**
 * The UI Component to display the pre-defined placeable buildings.
 *
 * @author Oscar L
 */
@Log
@Accessors(prefix = "m")
public class UIBuildingOptions extends Component implements IOnStart, IFixedUpdate {
    private List<BuildingDescriptor> mBuildingsCanPlace;
    @Setter private BuildingDescriptor mSelectedBuildingDescriptor;
    @Setter @Getter private Reference<UITextRect> mDescriptorTextRef = new Reference<>(null);
    private Reference<GameObject> mPossibleBuildingComponent;
    @Setter private Reference<UIButton> mPreviousLock = new Reference<>(null);
    private List<IUIBuildHandler> mBuildingsCanPlaceButtons;
    @Setter private Reference<GameObject> mVisibleDescriptorHint = new Reference<>(null);
    @Getter private final UIShopSection mParent;
    private Reference<Player> mPlayerReference;
    @Getter @Setter private int mTokens = 0;

    /**
     * Constructor.
     *
     * @param mParent its parent, this avoids passing tonnes of callbacks
     */
    public UIBuildingOptions(UIShopSection mParent) {
        this.mParent = mParent;
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy. */
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
                                new TransformUI(),
                                (self) -> {
                                    TransformUI t = self.getTransform(TransformUI.class);
                                    UIManager manager = UIManager.getInstance();
                                    mBuildingsCanPlace = PredefinedBuildings.getAll();
                                    mBuildingsCanPlaceButtons =
                                            mBuildingsCanPlace.stream()
                                                    .map(this::buildPredefinedBuildingBox)
                                                    .collect(Collectors.toList());

                                    manager.buildGridUI(
                                            self,
                                            4,
                                            -0.08f,
                                            0.2f,
                                            0.41f,
                                            0.25f,
                                            mBuildingsCanPlaceButtons);

                                    self.buildChild(
                                            "build_selected_tile",
                                            new TransformUI(true),
                                            (me) -> {
                                                UIButton button =
                                                        new UIButton("Build", this::buildOnClick);
                                                me.addComponent(button);
                                                TransformUI transformUI =
                                                        me.getTransform(TransformUI.class);
                                                transformUI.setParentAnchor(
                                                        0.16f, 0.6f, 0.86f, 1.1f);
                                            });
                                });

        getGameObject()
                .buildChild(
                        "descriptor_hint",
                        new TransformUI(),
                        (self) -> {
                            TransformUI hintTransform = self.getTransform(TransformUI.class);
                            hintTransform.setParentAnchor(-0.05f, -0.7f, 1.05f, -0.05f);

                            BuildingDescriptor descriptor = PredefinedBuildings.BASE;
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
                            setDescriptorTextRef(component.getReference(UITextRect.class));
                        });
    }

    /**
     * Build a graphic from a building descriptor.
     *
     * @param descriptor the descriptor
     * @return build handler
     */
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
                                if (!mSelectedBuildingDescriptor.equals(descriptor)) {
                                    showDescriptorHint(descriptor);
                                }
                                setPreviousLock(me.getReference(UIButton.class));
                                setSelectedBuildingDescriptor(descriptor);
                                me.setLockPressed(true);
                            },
                            null,
                            null);

            if (descriptor.equals(PredefinedBuildings.BASE)) {
                setPreviousLock(but.getReference(UIButton.class));
                setSelectedBuildingDescriptor(descriptor);
                but.setLockPressed(true);
            }
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

    /**
     * Sets the visible descriptor in the 'hint'ish box.
     *
     * @param descriptor the descriptor
     */
    private void showDescriptorHint(BuildingDescriptor descriptor) {
        log.info("showing hint");
        Reference<UITextRect> descriptorTextRef = getDescriptorTextRef();
        if (Reference.isValid(descriptorTextRef)) {
            Reference<UIText> labelText = descriptorTextRef.get().getLabelText();
            if (Reference.isValid(labelText)) {
                String txt =
                        String.format(
                                "%s\nAttack Strength: %d\nDefence Strength: %d\nGeneration Value: %d\nCost: %d",
                                descriptor.getName().toUpperCase(),
                                descriptor.getAttack(),
                                descriptor.getDefence(),
                                descriptor.getTokenGeneration(),
                                descriptor.getCost());

                labelText.get().setText(txt);
            }
        }
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        ensurePlayerReference();
        updateTokens();
    }

    /** Update the local tokens. */
    private void updateTokens() {
        if (Reference.isValid(mPlayerReference)) {
            setTokens(mPlayerReference.get().getTokens().get());
        }
    }

    /** Ensure that the player reference isValid, otherwise fetch. */
    private void ensurePlayerReference() {
        if (Reference.isInvalid(mPlayerReference)) {
            mPlayerReference =
                    getParent().getParent().mGetPlayer.getPlayer().getReference(Player.class);
        }
    }

    /**
     * The onClick handler for the build button.
     *
     * @param __ ignored
     * @param ___ ignored
     */
    private void buildOnClick(UIButton __, float ___) {
        if (getParent().getParent().mGetHexChosen.getHex() != null) {
            Reference<Player> player =
                    getParent().getParent().mGetPlayer.getPlayer().getReference(Player.class);
            if (Reference.isValid(player)) {
                player.get()
                        .getClientBuildRequest()
                        .invoke(
                                new BuildData(
                                        getParent().getParent().mGetHexChosen.getHex(),
                                        PredefinedBuildings.getIndex(mSelectedBuildingDescriptor)));

                getParent().markDidBuild(true);
            }

            getParent().getParent().mNotifyScreenChange.call(Screen.BUILDING_SELECTED_SCREEN);
        }
    }
}
