/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.*;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.SampledTexture;

/**
 * Class describing a rectangle with text
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UITextRect extends Component implements IOnAwake {
    protected Reference<UIRenderable> mRenderable;
    protected UIMaterial mMaterial;

    @Getter @Setter protected SampledTexture mRectTexture = null;

    @Getter @Setter protected UIAppearence mAppearence = UIManager.getInstance().getAppearence();

    private UIText mLabelTextComp;
    @Getter private Reference<UIText> mLabelText;

    public UITextRect() {}

    public UITextRect(UIText label) {
        mLabelTextComp = label;
    }

    public UITextRect(String label) {
        mLabelTextComp = new UIText(label);
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
            getGameObject()
                    .buildChild(
                            "label",
                            new TransformUI(true),
                            (handle) -> {
                                handle.getTransform(TransformUI.class)
                                        .setMargin(
                                                mAppearence.getRectTextHorizMargin(),
                                                mAppearence.getRectTextVertMargin());
                                mLabelText = mLabelTextComp.getReference(UIText.class);
                                handle.addComponent(mLabelTextComp);
                            });
        }
    }

    @Override
    protected void onDestroy() {}
}
