/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.PredefinedBuildings;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIFlatImage;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;

/**
 * The UI Component to display the pre-defined placeable buildings.
 *
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class UIBuildingOptions extends Component implements IOnStart, IFixedUpdate {
    private List<BuildingDescriptor> mBuildingsCanPlace;
    @Setter private BuildingDescriptor mSelectedBuildingDescriptor = PredefinedBuildings.BASE;
    @Setter private Reference<UIButton> mPreviousLock = new Reference<>(null);
    private List<IUIBuildHandler> mBuildingsCanPlaceButtons;
    @Getter private final UIShopSection mParent;
    private Reference<Player> mPlayerReference;
    @Getter @Setter private int mTokens = 0;

    private Reference<UIDescription> mDescription;

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
        // Add the buy button.
        GameObject buttonObject = new GameObject("cancel_button", new TransformUI());
        UIButton button =
                new UIButton(
                        "Cancel",
                        (__, ___) ->
                                getParent()
                                        .getParent()
                                        .mNotifyScreenChange
                                        .call(Screen.DEFAULT_SCREEN));
        buttonObject.addComponent(button);
        TransformUI transformUI = buttonObject.getTransform(TransformUI.class);
        transformUI.setParentAnchor(0.16f, 1.09f, 0.86f, 1.09f + 0.25f);

        // Add the description box.
        UIDescription description = new UIDescription();
        mDescription = description.getReference(UIDescription.class);
        getGameObject().addComponent(description);

        // Add the building selection buttons.
        getGameObject()
                .buildChild(
                        "possible_buildings",
                        new TransformUI(),
                        (self) -> {
                            UIManager manager = UIManager.getInstance();
                            mBuildingsCanPlace = PredefinedBuildings.getAll();
                            mBuildingsCanPlaceButtons =
                                    mBuildingsCanPlace.stream()
                                            .map(this::buildPredefinedBuildingBox)
                                            .collect(Collectors.toList());

                            manager.buildGridUI(
                                    self,
                                    3,
                                    0.07f,
                                    0.2f,
                                    0.35f + 0.6f,
                                    0.3f,
                                    mBuildingsCanPlaceButtons);

                            self.addChild(buttonObject);
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
                                    updateDescription(descriptor);
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
     * Update the description.
     *
     * @param descriptor The descriptor to be shown.
     */
    private void updateDescription(BuildingDescriptor descriptor) {

        getParent().getParent().getSetPredefinedBuildingChosen().setPredefinedBuilding(descriptor);

        if (!Reference.isValid(mDescription)) return;
        UIDescription description = mDescription.get();
        description.update(descriptor);
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        updateTokens();
    }

    /** Update the local tokens. */
    private void updateTokens() {
        Player player = getPlayer();
        if (player == null) return;
        setTokens(player.getTokens().get());
    }

    /**
     * Get the player.
     *
     * @return The {@link Player}, or {@code null}.
     */
    private Player getPlayer() {
        if (!Reference.isValid(mPlayerReference)) {
            // Attempt to get a valid reference.
            mPlayerReference =
                    getParent().getParent().mGetPlayer.getPlayer().getReference(Player.class);

            // If the reference is still invalid, return null.
            if (!Reference.isValid(mPlayerReference)) return null;
        }

        // Return the Player.
        return mPlayerReference.get();
    }
}
