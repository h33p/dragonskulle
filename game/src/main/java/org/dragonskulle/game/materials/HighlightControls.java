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
 * Controls highlights of tiles.
 *
 * @author Aurimas Blažulionis
 *     <p>This class converts object's materials to highlightable ones, and allows to change their
 *     highlighting.
 */
@Accessors(prefix = "m")
public class HighlightControls extends Component implements IOnAwake, IFrameUpdate {

    /** Renderables to control the highlights for. */
    private List<Reference<Renderable>> mRenderables = new ArrayList<>();
    /** Target child for highlighting. */
    private final String mChildName;
    /** List of highlight materials to control. */
    @Getter private List<PBRHighlightMaterial> mHighlightMaterials = new ArrayList<>();
    /** Target highlight colour. */
    @Getter private final Vector4f mTargetColour = new Vector4f(0f);

    /** Default constructor for {@link HighlightControls}. */
    public HighlightControls() {
        this(null);
    }

    /**
     * Constructor for {@link HighlightControls}.
     *
     * @param childName target child to find on start. {@code null} to use self.
     */
    public HighlightControls(String childName) {
        mChildName = childName;
    }

    /**
     * Set the object highlight.
     *
     * @param r red colour component.
     * @param g green colour component.
     * @param b blue colour component.
     * @param a alpha colour component.
     */
    public void setHighlight(float r, float g, float b, float a) {
        mTargetColour.set(r, g, b, a);
    }

    /**
     * Set the object highlight.
     *
     * @param col target colour to set.
     */
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
