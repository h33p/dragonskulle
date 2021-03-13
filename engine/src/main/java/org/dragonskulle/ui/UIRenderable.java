/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import static org.dragonskulle.utils.MathUtils.lerp;

import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.*;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.Texture;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

/**
 * Class describing a renderable UI object
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UIRenderable extends Renderable implements IOnAwake {
    /** Maintain aspect ration of the UI element with its texture */
    @Getter @Setter private boolean mMaintainAspect = true;

    /**
     * Controls which scale dimension the UI element should prioritize.
     *
     * <p>It is applied whenever textures are not square sized.
     *
     * <p>Given if the value is 0, the generated shape will have full width, but smaller/higher
     * height.
     *
     * <p>When value is 1, the generated shape will have full height, but smaller/higher width.
     *
     * <p>Values inbetween blend between these 2 states, while 0.5 simply fits the shape within the
     * scale bounds.
     */
    @Getter @Setter private float mWidthHeightBlend = 0f;

    /**
     * Aspect ratio of the object.
     *
     * <p>This will be main texture.width / texture.height
     */
    @Getter protected float mAspectRatio = 1.f;

    private final Matrix4f mTmpMatrix = new Matrix4f();

    private final Vector3f mTmpCursorPos = new Vector3f();

    /** Default constructor for UIRenderable */
    public UIRenderable() {
        super(Mesh.QUAD, new UIMaterial());
    }

    /**
     * Constructor for UIRenderable
     *
     * @param colour colour of the rendered UI element
     * @param texture texture of the rendered UI element
     */
    public UIRenderable(Vector4fc colour, SampledTexture texture) {
        super(Mesh.QUAD, new UIMaterial(colour, texture));
    }

    /**
     * Constructor for UIRenderable
     *
     * @param colour colour of the rendered UI element
     */
    public UIRenderable(Vector4fc colour) {
        super(Mesh.QUAD, new UIMaterial(colour));
    }

    /**
     * Constructor for UIRenderable
     *
     * @param texture texture of the rendered UI element
     */
    public UIRenderable(SampledTexture texture) {
        super(Mesh.QUAD, new UIMaterial(texture));
    }

    @Override
    public void onAwake() {
        SampledTexture[] texs = mMaterial.getFragmentTextures();
        Texture tex =
                texs != null && texs.length > 0 && texs[0] != null && texs[0].getTexture() != null
                        ? texs[0].getTexture().get()
                        : null;

        if (tex != null) mAspectRatio = (float) tex.getWidth() / (float) tex.getHeight();
    }

    @Override
    public void writeVertexInstanceData(int offset, ByteBuffer buffer) {
        updateTmpMatrix();
        mMaterial.writeVertexInstanceData(offset, buffer, mTmpMatrix);
    }

    @Override
    public float getDepth(Vector3fc camPosition, Vector3f tmpVec) {
        return (float) -getGameObject().getDepth();
    }

    public boolean cursorOver() {
        updateTmpMatrix();

        Vector2f cursorCoords = UIManager.getInstance().getScaledCursorCoords();

        mTmpMatrix.invert();

        mTmpCursorPos.set(cursorCoords.x(), cursorCoords.y(), 0f);

        mTmpCursorPos.mulPosition(mTmpMatrix);

        return mTmpCursorPos.x() >= -1.f
                && mTmpCursorPos.x() <= 1.f
                && mTmpCursorPos.y() >= -1.f
                && mTmpCursorPos.y() <= 1.f;
    }

    private void updateTmpMatrix() {
        mTmpMatrix.set(getGameObject().getTransform().getWorldMatrix());

        if (mMaintainAspect)
            mTmpMatrix.scaleLocal(
                    lerp(1f, mAspectRatio, mWidthHeightBlend),
                    lerp(1f / mAspectRatio, 1.f, mWidthHeightBlend),
                    1f);
    }
}
