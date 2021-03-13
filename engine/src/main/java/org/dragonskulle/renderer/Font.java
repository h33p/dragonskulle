/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.joml.Vector2f;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@Accessors(prefix = "m")
@Getter
public final class Font extends Texture {

    private static class Glyph implements IBox {
        @Getter private int mWidth;
        @Getter private int mHeight;
        @Getter private int mYBearing;
        @Getter private int mXBearing;
        @Getter private int mAdvance;
        private int mCode;

        public Glyph(int width, int height, int yBearing, int xBearing, int advance, int code) {
            mWidth = width;
            mHeight = height;
            mYBearing = yBearing;
            mXBearing = xBearing;
            mAdvance = advance;
            mCode = code;
        }
    }

    /**
     * Gets glyph coordinates and advances curpos
     *
     * @param charCode input character
     * @param curpos current cursor position
     * @param outStartPos start of the bounding box that will be written
     * @param outEndPos end of the bounding box that will be written
     * @param outUVStart lower UV coordinates that will be written
     * @param outUVEnd upper UV coordinates that will be written
     */
    public void getGlyph(
            int charCode,
            int[] curpos,
            Vector2f outStartPos,
            Vector2f outEndPos,
            Vector2f outUVStart,
            Vector2f outUVEnd) {
        BoxPacker.BoxNode<Glyph> glyphNode = mCharToGlyph.get(charCode);

        // If not found, fallback to default square box
        if (glyphNode == null) glyphNode = mCharToGlyph.get(0);

        int curX = curpos[0];
        int curY = curpos[1];
        curY += nextLineOffset - glyphNode.getHeight();

        float startX = curX + (float) glyphNode.getBox().getXBearing();
        float endX = startX + (float) glyphNode.getWidth();
        float startY = curY + (float) glyphNode.getBox().getYBearing();
        float endY = startY + (float) glyphNode.getHeight();

        outStartPos.set(startX, startY);
        outEndPos.set(endX, endY);

        float atlasScale = 1.f / (float) ATLAS_SIZE;

        outUVStart.set(glyphNode.getX(), glyphNode.getY());
        outUVEnd.set(
                glyphNode.getX() + glyphNode.getWidth(), glyphNode.getY() + glyphNode.getHeight());

        outUVStart.mul(atlasScale);
        outUVEnd.mul(atlasScale);

        curpos[0] += glyphNode.getBox().getAdvance();

        // todo: stbtt_GetCodepointKernAdvance
    }

    private Map<Integer, BoxPacker.BoxNode<Glyph>> mCharToGlyph = new TreeMap<>();
    private int nextLineOffset = LINE_HEIGHT;

    private static final int[][] GLYPH_RANGES = {
        {' ', '~'},
        {0, 0}
    };

    private static final int ATLAS_SIZE = 2048;
    public static final int LINE_HEIGHT = 128;

    public static Resource<Font> getFontResource(String inName) {
        String name = String.format("fonts/%s", inName);

        return ResourceManager.getResource(
                Font.class,
                (buffer) -> {
                    try {

                        Font ret = new Font();
                        ByteBuffer buf = MemoryUtil.memAlloc(buffer.length);
                        buf.put(buffer);
                        buf.rewind();
                        try (MemoryStack stack = stackPush()) {

                            STBTTFontinfo info = STBTTFontinfo.callocStack(stack);

                            if (!stbtt_InitFont(info, buf, 0)) {
                                MemoryUtil.memFree(buf);
                                return null;
                            }

                            float scale = stbtt_ScaleForPixelHeight(info, (float) LINE_HEIGHT);

                            IntBuffer pAscent = stack.ints(0);
                            IntBuffer pDescent = stack.ints(0);
                            IntBuffer pLineGap = stack.ints(0);
                            stbtt_GetFontVMetrics(info, pAscent, pDescent, pLineGap);

                            ret.nextLineOffset =
                                    (int) ((pAscent.get(0) - pDescent.get(0)) * scale)
                                            + LINE_HEIGHT;

                            IntBuffer pOffsetToNext = stack.ints(0);
                            IntBuffer pOffsetToStart = stack.ints(0);

                            IntBuffer pSX = stack.ints(0);
                            IntBuffer pSY = stack.ints(0);
                            IntBuffer pEX = stack.ints(0);
                            IntBuffer pEY = stack.ints(0);

                            ret.mBuffer = MemoryUtil.memCalloc(ATLAS_SIZE * ATLAS_SIZE * 4);
                            ret.mWidth = ATLAS_SIZE;
                            ret.mHeight = ATLAS_SIZE;
                            ret.mChannels = 4;

                            ByteBuffer tmpBuffer =
                                    MemoryUtil.memCalloc(LINE_HEIGHT * 2 * LINE_HEIGHT * 2);

                            BoxPacker<Glyph> packer = new BoxPacker<>(ret.mWidth, ret.mHeight);

                            ArrayList<Glyph> glyphList = new ArrayList<>();

                            for (int[] range : GLYPH_RANGES) {
                                int start = range[0];
                                int end = range[1];

                                for (int i = start; i <= end; i++) {
                                    stbtt_GetCodepointHMetrics(
                                            info, i, pOffsetToNext, pOffsetToStart);
                                    stbtt_GetCodepointBitmapBox(
                                            info, i, scale, scale, pSX, pSY, pEX, pEY);

                                    glyphList.add(
                                            new Glyph(
                                                    pEX.get(0) - pSX.get(0),
                                                    pEY.get(0) - pSY.get(0),
                                                    pEY.get(0),
                                                    (int) (pOffsetToStart.get(0) * scale),
                                                    (int) (pOffsetToNext.get(0) * scale),
                                                    i));
                                }
                            }

                            glyphList.sort(
                                    (a, b) ->
                                            -Integer.compare(
                                                    a.getWidth() * a.getHeight(),
                                                    b.getWidth() * b.getHeight()));

                            for (Glyph glyph : glyphList) {
                                BoxPacker.BoxNode<Glyph> packedGlyph = packer.pack(glyph, 1);

                                ret.mBuffer.rewind();
                                tmpBuffer.rewind();
                                stbtt_MakeCodepointBitmap(
                                        info,
                                        tmpBuffer,
                                        glyph.getWidth(),
                                        glyph.getHeight(),
                                        LINE_HEIGHT * 2,
                                        scale,
                                        scale,
                                        glyph.mCode);
                                tmpBuffer.rewind();

                                for (int i = 0; i < glyph.mHeight; i++) {
                                    for (int o = 0; o < glyph.mWidth; o++) {
                                        ret.mBuffer.position(
                                                ((packedGlyph.getX() + o)
                                                                + (packedGlyph.getY() + i)
                                                                        * ret.mWidth)
                                                        * 4);
                                        ret.mBuffer.put((byte) 255);
                                        ret.mBuffer.put((byte) 255);
                                        ret.mBuffer.put((byte) 255);
                                        byte alpha = tmpBuffer.get(i * LINE_HEIGHT * 2 + o);
                                        ret.mBuffer.put(alpha);
                                        ret.mBuffer.rewind();
                                    }
                                }

                                ret.mCharToGlyph.put(glyph.mCode, packedGlyph);
                            }
                            MemoryUtil.memFree(tmpBuffer);
                        }
                        MemoryUtil.memFree(buf);
                        return ret;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                },
                name);
    }

    @Override
    public final void free() {
        if (mBuffer != null) {
            MemoryUtil.memFree(mBuffer);
            mBuffer = null;
        }
    }

    public int size() {
        return mBuffer.capacity();
    }
}
