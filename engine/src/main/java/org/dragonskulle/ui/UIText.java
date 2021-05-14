/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Resource;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.Texture;
import org.dragonskulle.renderer.Vertex;
import org.dragonskulle.renderer.components.Light;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.ui.UIManager.IUIBuildHandler;
import org.dragonskulle.utils.MathUtils;
import org.joml.FrustumIntersection;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Class describing a renderable UI text object.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UIText extends Renderable implements IOnAwake, IFrameUpdate, IUIBuildHandler {
    @Getter private String mText = "";
    @Getter private Resource<Font> mFont;

    @Getter(AccessLevel.PACKAGE)
    private int mCursorPos = -1;

    private float mCursorTime = 0f;
    private float mInterval = 0.5f;

    @Getter @Setter private float mVerticalAlignment = 0f;
    @Getter @Setter private float mHorizontalAlignment = 0f;

    @Getter @Setter private float mDepthShift = 0f;

    private float mTargetAspectRatio = 0f;

    private Mesh mNormalText;
    private Mesh mTextWithCursor;

    @Override
    public void onAwake() {
        setText(mText);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (mCursorPos < 0) {
            return;
        }

        float delta = Engine.getInstance().getCurTime() - mCursorTime;

        float mod = (delta % mInterval) / mInterval;

        if (mod > 0.5f) {
            setMesh(mNormalText);
        } else {
            setMesh(mTextWithCursor);
        }
    }

    @Override
    public void handleUIBuild(GameObject go) {
        go.addComponent(this);
    }

    /** Constructor for {@link UIText}. */
    public UIText() {
        this(UIManager.getInstance().getAppearance());
    }

    /**
     * Constructor for {@link UIText}.
     *
     * @param appearance custom appearance to set.
     */
    public UIText(UIAppearance appearance) {
        this(appearance.getTextColour(), appearance.getTextFont().clone());
    }

    /**
     * Constructor for {@link UIText}.
     *
     * @param text initial text to set.
     */
    public UIText(String text) {
        this();
        mText = text;
    }

    /**
     * Constructor for {@link UIText}.
     *
     * @param colour RGBA float colour value
     * @param font font to use for text
     */
    public UIText(Vector4fc colour, Resource<Font> font) {
        super(null, new UIMaterial(colour, new SampledTexture(font.clone().cast(Texture.class))));
        mFont = font;
    }

    /**
     * Constructor for {@link UIText}.
     *
     * @param colour RGBA float colour value
     * @param font font to use for text
     * @param text text to render
     */
    public UIText(Vector4fc colour, Resource<Font> font, String text) {
        this(colour, font);
        mText = text;
    }

    /**
     * Constructor for {@link UIText}.
     *
     * @param colour RGB float colour value
     * @param font font to use for text
     * @param text text to render
     */
    public UIText(Vector3fc colour, Resource<Font> font, String text) {
        this(new Vector4f(colour, 1.f), font);
        mText = text;
    }

    /**
     * Constructor for {@link UIText}.
     *
     * @param colour RGBA float colour value
     */
    public UIText(Vector4fc colour) {
        this(colour, UIManager.getInstance().getAppearance().getTextFont());
    }

    /**
     * Constructor for {@link UIText}.
     *
     * @param colour RGB float colour value
     */
    public UIText(Vector3fc colour) {
        this(new Vector4f(colour, 1.f));
    }

    /**
     * Constructor for {@link UIText}.
     *
     * @param colour RGBA float colour value
     * @param text text to render
     */
    public UIText(Vector4fc colour, String text) {
        this(colour);
        mText = text;
    }

    /**
     * Constructor for {@link UIText}.
     *
     * @param colour RGB float colour value
     * @param text text to render
     */
    public UIText(Vector3fc colour, String text) {
        this(new Vector4f(colour, 1.f), text);
    }

    /**
     * Set a new text value to render
     *
     * <p>This method can be computationally expensive to call regularly. It is recommended to set
     * text up once.
     *
     * @param text new text value
     */
    public void setText(String text) {
        if (mText.equals(text) && getMesh() != null) {
            return;
        }
        mText = text;

        buildMesh();

        setMesh(mCursorPos < 0 ? mNormalText : mTextWithCursor);

        TransformUI transform = getGameObject().getTransform(TransformUI.class);
        if (transform != null) {
            transform.setTargetAspectRatio(mTargetAspectRatio);
        }
    }

    /**
     * Sets the cursor position
     *
     * <p>Sets the cursor position, and makes it visible.
     *
     * @param position the cursor position. If the position is non-negative, it will place the
     *     cursor before the specified character. If position lies out of [0; mText.length()] bounds
     *     (is negative, or higher than the text length), the cursor will simply not show up.
     */
    void setCursorPos(int position) {
        if (mCursorPos == position) {
            return;
        }

        mCursorPos = position;
        mCursorTime = Engine.getInstance().getCurTime();

        buildMesh();

        setMesh(mCursorPos < 0 ? mNormalText : mTextWithCursor);

        TransformUI transform = getGameObject().getTransform(TransformUI.class);
        if (transform != null) {
            transform.setTargetAspectRatio(mTargetAspectRatio);
        }
    }

    /** Builds a new mesh. */
    private void buildMesh() {
        Font font = mFont.get();

        ArrayList<Vertex> vertices = new ArrayList<>(mText.length() * 4);
        ArrayList<Integer> indices = new ArrayList<>(mText.length() * 6);

        /* All startBox positions go here. */
        ArrayList<Vector2f> preCharPositions = new ArrayList<>(mText.length());
        /* All endBox positions go here. */
        ArrayList<Vector2f> postCharPositions = new ArrayList<>(mText.length());
        final int[] pos = {0, 0};
        final float scale = 0.003f;

        Vector2f bbmin = new Vector2f(Float.POSITIVE_INFINITY);
        Vector2f bbmax = new Vector2f(Float.NEGATIVE_INFINITY);

        Vector2f startBox = new Vector2f();
        Vector2f endBox = new Vector2f();
        Vector2f startUV = new Vector2f();
        Vector2f endUV = new Vector2f();

        preCharPositions.add(new Vector2f(pos[0], pos[1]));
        postCharPositions.add(new Vector2f());

        // Put every character onto the mesh
        mText.chars()
                .forEach(
                        c -> {
                            if (c == '\n') {
                                pos[1] += Font.LINE_HEIGHT;
                                pos[0] = 0;
                            } else if (c == '\r') {
                                pos[0] = 0;
                            } else {
                                font.getGlyph(c, pos, startBox, endBox, startUV, endUV);
                                startBox.mul(scale);
                                endBox.mul(scale);
                                bbmin.min(startBox);
                                bbmax.max(endBox);
                                Mesh.addQuadToList(
                                        vertices, indices, startBox, endBox, startUV, endUV);
                                postCharPositions
                                        .get(postCharPositions.size() - 1)
                                        .set(startBox.x, pos[1] * scale);
                                postCharPositions.add(new Vector2f(endBox.x, pos[1] * scale));
                            }

                            preCharPositions.add(new Vector2f(pos[0], pos[1]));
                        });

        float width = bbmax.x() - bbmin.x();
        float height = bbmax.y() - bbmin.y();

        float aspect = width / height;

        float widthMul = 1.f / width;
        float heightMul = 1.f / height;

        Vector2f bbcenter =
                new Vector2f(
                        MathUtils.lerp(bbmin.x(), bbmax.x(), 0f * mHorizontalAlignment),
                        MathUtils.lerp(bbmin.y(), bbmax.y(), mVerticalAlignment));

        // Align the mesh, shift all vertices by negative of this
        Vector3f alignmentCenter = new Vector3f(bbcenter.x(), bbcenter.y(), 0f);

        bbmin.sub(bbcenter);
        bbmax.sub(bbcenter);

        // Shift all vertices by alignment
        vertices.forEach(
                v -> {
                    Vector3f newVec = new Vector3f(v.getPos());
                    newVec.sub(alignmentCenter);
                    newVec.mul(widthMul, heightMul, 1f);
                    v.getPos().set(newVec);
                });

        int[] indicesArray = indices.stream().mapToInt(Integer::intValue).toArray();

        mTargetAspectRatio = aspect;

        mNormalText = new Mesh(vertices.stream().toArray(Vertex[]::new), indicesArray);

        final boolean withCursor = mCursorPos >= 0 && mCursorPos <= mText.length();

        if (withCursor) {
            Vector2f p = preCharPositions.get(mCursorPos);
            pos[0] = (int) p.x;
            pos[1] = (int) p.y;
            Vector2f p2 = postCharPositions.get(mCursorPos);

            font.getGlyph('|', pos, startBox, endBox, startUV, endUV);
            startBox.mul(scale);
            endBox.mul(scale);
            float xdiff = p2.x - startBox.x - 0.01f;
            startBox.x += xdiff;
            endBox.x += xdiff;

            startBox.sub(alignmentCenter.x, alignmentCenter.y);
            startBox.mul(widthMul, heightMul);

            endBox.sub(alignmentCenter.x, alignmentCenter.y);
            endBox.mul(widthMul, heightMul);

            Mesh.addQuadToList(
                    vertices,
                    indices,
                    startBox,
                    endBox,
                    startUV,
                    endUV,
                    new Vector4f(1f, 1f, 1f, 0.8f));

            indicesArray = indices.stream().mapToInt(Integer::intValue).toArray();
            mTextWithCursor = new Mesh(vertices.stream().toArray(Vertex[]::new), indicesArray);
        } else {
            mTextWithCursor = null;
        }
    }

    @Override
    public float getDepth(Vector3fc camPosition, Vector3f tmpVec) {
        return (float) -getGameObject().getDepth() + mDepthShift;
    }

    @Override
    public boolean frustumCull(FrustumIntersection intersection) {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFont.free();
    }

    @Override
    public void writeVertexInstanceData(int offset, ByteBuffer buffer, List<Light> lights) {
        Matrix4fc mat = getGameObject().getTransform().getWorldMatrix();
        mMaterial.writeVertexInstanceData(offset, buffer, mat, lights);
    }
}
