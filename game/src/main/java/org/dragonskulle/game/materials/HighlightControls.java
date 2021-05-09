/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.materials;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.PBRMaterial;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * @author Aurimas Blažulionis This class converts object's materials to highlightable ones, and
 *     allows to change their highlighting.
 */
@Accessors(prefix = "m")
public class HighlightControls extends Component implements IOnAwake, IFrameUpdate {

    private List<Reference<Renderable>> mRenderables = new ArrayList<>();
    private final String mChildName;
    @Getter private List<PBRHighlightMaterial> mHighlightMaterials = new ArrayList<>();
    @Getter private final Vector4f mTargetColour = new Vector4f(0f);

    public HighlightControls() {
        this(null);
    }

    public HighlightControls(String childName) {
        mChildName = childName;
    }

    public void setHighlight(float r, float g, float b, float a) {
        if (mHighlightMaterials == null) {
            return;
        }
        mTargetColour.set(r, g, b, a);
    }

    public void setHighlight(Vector4fc col) {
        setHighlight(col.x(), col.y(), col.z(), col.w());
    }

    @Override
    public void onAwake() {
        final GameObject go;

        if (mChildName != null && !mChildName.equals("")) {
            go = getGameObject().findChildByName(mChildName);
        } else {
            go = getGameObject();
        }

        go.getComponents(Renderable.class, mRenderables);

        for (Reference<Renderable> renderable : mRenderables) {
            PBRHighlightMaterial highlightMaterial =
                    renderable.get().getMaterial(PBRHighlightMaterial.class);
            if (highlightMaterial == null) {
                PBRMaterial pbrMat = renderable.get().getMaterial(PBRMaterial.class);
                if (pbrMat == null) {
                    return;
                }
                highlightMaterial = new PBRHighlightMaterial(pbrMat);
                renderable.get().setMaterial(highlightMaterial.incRefCount());
            }

            mHighlightMaterials.add(highlightMaterial);
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        for (PBRHighlightMaterial mat : mHighlightMaterials) {
            mat.getOverlayColour().lerp(mTargetColour, Math.min(1, deltaTime * 10f));
        }
    }

    @Override
    protected void onDestroy() {}
}
