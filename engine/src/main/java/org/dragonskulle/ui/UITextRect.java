/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.*;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;

/**
 * Class describing a rectangle with text
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UITextRect extends Component implements IOnAwake, IUIBuildHandler {
    @Getter protected Reference<UIRenderable> mRenderable;
    protected UIMaterial mMaterial;

    @Getter @Setter protected SampledTexture mRectTexture = null;

    @Getter @Setter protected UIAppearance mAppearance = UIManager.getInstance().getAppearance();

    private UIText mLabelTextComp;
    @Getter private Reference<UIText> mLabelText;

    @Getter @Setter SampledTexture mIcon;

    public UITextRect() {
        mLabelTextComp = new UIText();
    }

    public UITextRect(UIText label) {
        mLabelTextComp = label;
    }

    public UITextRect(String label) {
        mLabelTextComp = new UIText(label);
    }

    @Override
    public void handleUIBuild(GameObject go) {
        go.addComponent(this);
    }

    @Override
    public void onAwake() {
        mRenderable = getGameObject().getComponent(UIRenderable.class);
        if (!Reference.isValid(mRenderable)) {
            getGameObject().addComponent(new UIRenderable(mRectTexture));
            mRenderable = getGameObject().getComponent(UIRenderable.class);
        }

        UIRenderable rend = mRenderable.get();

        if (rend != null) {
            if (rend.getMaterial() instanceof UIMaterial)
                mMaterial = (UIMaterial) rend.getMaterial();
        }

        if (mLabelTextComp != null) {
            if (mIcon != null) {
                getGameObject()
                        .buildChild(
                                "icon",
                                new TransformUI(true),
                                (handle) -> {
                                    TransformUI transform = handle.getTransform(TransformUI.class);
                                    transform.setMargin(
                                            mAppearance.getRectTextHorizMargin(),
                                            mAppearance.getRectTextVertMargin(),
                                            0,
                                            -mAppearance.getRectTextVertMargin());

                                    transform.setParentAnchor(
                                            0, 0, mAppearance.getButtonIconSplit(), 1);

                                    UIRenderable iconRend = new UIRenderable(mIcon);
                                    iconRend.setHoverable(false);
                                    handle.addComponent(iconRend);
                                });
            }

            getGameObject()
                    .buildChild(
                            "label",
                            new TransformUI(true),
                            (handle) -> {
                                TransformUI transform = handle.getTransform(TransformUI.class);
                                transform.setMargin(
                                        mAppearance.getRectTextHorizMargin(),
                                        mAppearance.getRectTextVertMargin());

                                if (mIcon != null) {
                                    transform.setParentAnchor(
                                            mAppearance.getButtonIconSplit(), 0, 1, 1);
                                    transform.setPivotOffset(
                                            Math.max(0f, 0.5f - mAppearance.getButtonIconSplit()),
                                            0.5f);
                                }

                                mLabelText = mLabelTextComp.getReference(UIText.class);
                                handle.addComponent(mLabelTextComp);
                            });
        }
    }

    @Override
    protected void onDestroy() {}
}
