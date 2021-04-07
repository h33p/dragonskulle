/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.materials;

import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.PBRMaterial;
import org.joml.Vector4fc;

/**
 * @author Aurimas Blažulionis This class converts object's materials to highlightable ones, and
 *     allows to change their highlighting.
 */
@Accessors(prefix = "m")
@Log
public class HighlightControls extends Component implements IOnAwake {

    private Reference<Renderable> mRenderable = new Reference<>(null);
    private final String mChildName;
    private PBRHighlightMaterial mHighlightMaterial;

    public HighlightControls() {
        this(null);
    }

    public HighlightControls(String childName) {
        mChildName = childName;
    }

    public void setHighlight(float r, float g, float b, float a) {
        if (mHighlightMaterial == null) return;
        mHighlightMaterial.getOverlayColour().set(r, g, b, a);
    }

    public void setHighlight(Vector4fc col) {
        setHighlight(col.x(), col.y(), col.z(), col.w());
    }

    private float mCurtime = 0;

    @Override
    public void onAwake() {
        final GameObject go;

        if (mChildName != null && !mChildName.isBlank()) {
            go = getGameObject().findChildByName(mChildName);
        } else {
            go = getGameObject();
        }

        mRenderable = go.getComponent(Renderable.class);

        if (mRenderable != null) {
            mHighlightMaterial = mRenderable.get().getMaterial(PBRHighlightMaterial.class);
            if (mHighlightMaterial == null) {
                PBRMaterial pbrMat = mRenderable.get().getMaterial(PBRMaterial.class);
                if (pbrMat == null) return;
                mHighlightMaterial = new PBRHighlightMaterial(pbrMat);
                mRenderable.get().setMaterial(mHighlightMaterial.incRefCount());
            }
        }
    }

    @Override
    protected void onDestroy() {}
}
