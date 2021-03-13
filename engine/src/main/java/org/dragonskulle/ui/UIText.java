/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.*;
import org.dragonskulle.core.Resource;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.Texture;
import org.dragonskulle.renderer.Vertex;
import org.dragonskulle.utils.MathUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Class describing a renderable UI text object
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class UIText extends Renderable {
    @Getter private String mText = "";
    @Getter private Resource<Font> mFont;

    @Getter @Setter private float mVerticalAlignment = 1.65f;
    @Getter @Setter private float mHorizontalAlignment = 0.5f;

    /**
     * Constructor for UIText
     *
     * @param colour RGBA float colour value
     * @param font font to use for text
     */
    public UIText(Vector4fc colour, Resource<Font> font) {
        super(null, new UIMaterial(colour, new SampledTexture(font.clone().cast(Texture.class))));
        mFont = font;
    }

    /**
     * Constructor for UIText
     *
     * @param colour RGBA float colour value
     * @param font font to use for text
     * @param text text to render
     */
    public UIText(Vector4fc colour, Resource<Font> font, String text) {
        this(colour, font);
        setText(text);
    }

    /**
     * Constructor for UIText
     *
     * @param colour RGBA float colour value
     */
    public UIText(Vector4fc colour) {
        this(colour, Font.getFontResource("CascadiaCode.ttf"));
    }

    /**
     * Constructor for UIText
     *
     * @param colour RGB float colour value
     */
    public UIText(Vector3fc colour) {
        this(new Vector4f(colour, 1.f));
    }

    /**
     * Constructor for UIText
     *
     * @param colour RGBA float colour value
     * @param text text to render
     */
    public UIText(Vector4fc colour, String text) {
        this(colour);
        setText(text);
    }

    /**
     * Constructor for UIText
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
        if (mText.equals(text)) return;
        mText = text;
        // TODO: probably mark the old mesh as freed or smth so that renderer can know that it
        // should be removed from mesh buffer
        mMesh = buildMesh();
    }

    /** Builds a new mesh */
    private Mesh buildMesh() {
        Font font = mFont.get();

        ArrayList<Vertex> vertices = new ArrayList<>(mText.length() * 4);
        ArrayList<Integer> indices = new ArrayList<>(mText.length() * 6);
        final int[] pos = {0, 0};
        final float scale = 0.003f;

        Vector2f bbmin = new Vector2f(Float.POSITIVE_INFINITY);
        Vector2f bbmax = new Vector2f(Float.NEGATIVE_INFINITY);

        Vector2f startBox = new Vector2f();
        Vector2f endBox = new Vector2f();
        Vector2f startUV = new Vector2f();
        Vector2f endUV = new Vector2f();

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
                            }
                        });

        // Align the mesh, shift all vertices by negative of this
        Vector3f alignmentCenter =
                new Vector3f(
                        MathUtils.lerp(bbmin.x(), bbmax.x(), mHorizontalAlignment),
                        MathUtils.lerp(bbmin.y(), bbmax.y(), mVerticalAlignment),
                        0f);

        // Shift all vertices by alignment
        vertices.forEach(
                v -> {
                    Vector3f newVec = new Vector3f(v.getPos());
                    newVec.sub(alignmentCenter);
                    v.setPos(newVec);
                });

        int[] indicesArray = indices.stream().mapToInt(Integer::intValue).toArray();

        return new Mesh(vertices.toArray(Vertex[]::new), indicesArray);
    }

    @Override
    public float getDepth(Vector3fc camPosition, Vector3f tmpVec) {
        return (float) -getGameObject().getDepth();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFont.free();
    }
}
