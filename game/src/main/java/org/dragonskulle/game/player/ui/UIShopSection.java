/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIMaterial;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;

/**
 * This is a section of the shop which can contain a sub-screen to its super window. For example the
 * shop can show additional options like in {@code ShopState.MY_BUILDING_SELECTED}.
 *
 * @author Oscar L
 */
@Accessors(prefix = "m")
@Log
public class UIShopSection extends Component implements IOnStart, IFrameUpdate {
    @Getter private ShopState mState = ShopState.MY_BUILDING_SELECTED;
    @Setter @Getter private ShopState mLastState = ShopState.CLOSED;
    private Reference<GameObject> mNewBuildingPanel;
    private Reference<GameObject> mUpgradePanel;

    @Getter(AccessLevel.PROTECTED)
    private final UIMenuLeftDrawer mParent;

    @Setter @Getter private Reference<GameObject> mCurrentPanel = new Reference<>(null);
    private Reference<UIText> mTitleRef;
    private Reference<TransformUI> mTransform;
    private boolean mShopIsTranslated = true;
    @Setter private boolean mShouldTranslateShop = false;
    private float mStep = 0.02f;
    private float mLastY = 0.68f;
    private TransformUI textTransform;
    private Reference<UIBuildingUpgrade> mUpgradePanelComponent;

    /**
     * Constructor.
     *
     * @param mParent
     */
    public UIShopSection(UIMenuLeftDrawer mParent) {
        this.mParent = mParent;
    }

    @Override
    public void frameUpdate(float deltaTime) {
        //        translateShop();
    }

    /** The Shop state. This controls what can be seen at what time. */
    public enum ShopState {
        CLOSED,
        BUILDING_NEW,
        ATTACK_SCREEN,
        MY_BUILDING_SELECTED
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy. */
    @Override
    protected void onDestroy() {}

    /**
     * Sets the shop state.
     *
     * @param state the state
     */
    protected void setState(ShopState state) {
        getParent().mUpdateBuildingSelected.update();
        if (state != getLastState()) {
            shouldTranslateShopIfNotVisible();
            String newText = "Shop is Closed";
            if (state.equals(ShopState.CLOSED)) {
                show(mNewBuildingPanel, false);
                show(mCurrentPanel, false);
                setLastState(state);
                if (Reference.isValid(mTitleRef)) {
                    mTitleRef.get().setText(newText);
                }
                setShouldTranslateShop(true);
                swapPanels(new Reference<>(null));
                return;
            }
            Reference<GameObject> newPanel;
            log.warning("setting visible state to " + state);
            switch (state) {
                case ATTACK_SCREEN:
                    newPanel = new Reference<>(null);
                    break;
                case BUILDING_NEW:
                    newPanel = mNewBuildingPanel;
                    newText = "New Building";
                    break;
                case MY_BUILDING_SELECTED:
                    newPanel = mUpgradePanel;
                    newText = "Upgrade Building";
                    break;
                default:
                    log.warning("Menu hasn't been updated to reflect this screen yet  " + state);
                    if (Reference.isValid(mTitleRef)) {
                        mTitleRef.get().setText(newText);
                    }
                    setState(ShopState.CLOSED);
                    return;
            }

            if (Reference.isValid(mTitleRef)) {
                mTitleRef.get().setText(newText);
            }
            setLastState(state);
            swapPanels(newPanel);
        }
    }

    private void shouldTranslateShopIfNotVisible() {
        if (mShopIsTranslated) {
            setShouldTranslateShop(true);
        }
    }

    private void translateShop() {
        if (mShouldTranslateShop) {
            if (Reference.isValid(mTransform)) {
                // 0.08f, 0.68f, 1 - 0.08f, 1 - 0.03f
                TransformUI t = mTransform.get();
                mLastY = mLastY + (mShopIsTranslated ? -(mStep) : mStep);
                t.setParentAnchor(0.08f, mLastY, 1 - 0.08f, 1 - 0.03f);
                float y = textTransform.getPosition().y();
                log.info("y: " + y);
                if (0.4f < y && y < 0.86f) {
                    textTransform.translate(0, (mShopIsTranslated ? -0.02f : 0.02f));
                }
                if (mLastY >= 0.88f) {
                    setShouldTranslateShop(false);
                    mShopIsTranslated = true;
                } else if (mLastY <= 0.68f) {
                    setShouldTranslateShop(false);
                    mShopIsTranslated = false;
                }
            }
        }
    }

    /**
     * Show the component's GO.
     *
     * @param component the component
     * @param shouldShow true if should show
     */
    private void show(Reference<GameObject> component, boolean shouldShow) {
        if (Reference.isValid(component)) {
            component.get().setEnabled(shouldShow);
        }
    }

    /**
     * Swaps the visible panels.
     *
     * @param newPanel the new panel
     */
    private void swapPanels(Reference<GameObject> newPanel) {
        log.info("swapping panels");
        // if there is a screen being shown
        // deactivate the panel
        if (Reference.isValid(mCurrentPanel)) {
            mCurrentPanel.get().setEnabled(false);
        }

        log.info("maybe showing newPanel");
        if (Reference.isValid(newPanel)) {
            newPanel.get().setEnabled(true);
        }
        mCurrentPanel = newPanel;
    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        UIBuildingUpgrade uiBuildingUpgrade = new UIBuildingUpgrade(this);
        mUpgradePanel =
                getGameObject()
                        .buildChild(
                                "upgrade_object",
                                new TransformUI(true),
                                (self) -> self.addComponent(uiBuildingUpgrade));
        mUpgradePanelComponent = uiBuildingUpgrade.getReference(UIBuildingUpgrade.class);
        show(mUpgradePanel, false);

        UIBuildingOptions uiBuildingOptions =
                new UIBuildingOptions(this); // TODO this is what needs improving now
        mNewBuildingPanel =
                getGameObject()
                        .buildChild(
                                "building_object",
                                new TransformUI(true),
                                (self) -> self.addComponent(uiBuildingOptions));
        show(mNewBuildingPanel, false);

        // Outer window stuff
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        //        0.08f, 0.86f, 1 - 0.08f, 1 - 0.03f when using transform
        tran.setParentAnchor(0.08f, 0.68f, 1 - 0.08f, 1 - 0.03f);
        mTransform = tran.getReference(TransformUI.class);
        UIRenderable renderable = new UIRenderable(new SampledTexture("white.bmp"));
        ((UIMaterial) renderable.getMaterial()).getColour().set(0.235, 0.219, 0.235, 1);
        getGameObject().addComponent(renderable);
        Reference<GameObject> textObj =
                getGameObject()
                        .buildChild(
                                "main_shop_text",
                                new TransformUI(true),
                                (self) -> {
                                    UIText mWindowText = new UIText("Shop is Closed");
                                    self.addComponent(mWindowText);
                                    mTitleRef = mWindowText.getReference(UIText.class);
                                });

        textTransform = textObj.get().getTransform(TransformUI.class);
        textTransform.setParentAnchor(0.05f, 0f);
        textTransform.translate(0, -0.22f); // remove if using transforms
    }
}
