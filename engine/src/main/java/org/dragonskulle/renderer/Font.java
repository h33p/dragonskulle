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
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@Accessors(prefix = "m")
@Getter
public final class Font extends Texture {

    private static class Glyph implements IBox {
        @Getter private int mWidth;
        @Getter private int mHeight;
        private int mYBearing;
        private int mXBearing;
        private int mAdvance;
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

    private interface IBox {
        int getWidth();

        int getHeight();
    }

    private static class BoxNode<T extends IBox> {
        @Getter private T mBox;
        private int mX;
        private int mY;
        private int mWidth;
        private int mHeight;
        private BoxNode<T> mLeft;
        private BoxNode<T> mRight;
        private boolean mFilledLeaf = false;

        public BoxNode(int x, int y, int width, int height) {
            mX = x;
            mY = y;
            mWidth = width;
            mHeight = height;
        }
    }

    /**
     * Box packer. Based on lightmap packing techniques
     *
     * <p>One of which: https://blackpawn.com/texts/lightmaps/default.html
     */
    private static class BoxPacker<T extends IBox> {

        private BoxNode<T> mRoot;

        public BoxPacker(int width, int height) {
            mRoot = new BoxNode<T>(0, 0, width, height);
        }

        // TODO: Add resizing support

        private BoxNode<T> pack(BoxNode<T> node, T newBox) {

            if (node.mFilledLeaf) {
                return null;
            } else if (node.mLeft != null && node.mRight != null) {
                BoxNode<T> ret = pack(node.mLeft, newBox);
                if (ret == null) ret = pack(node.mRight, newBox);
                return ret;
            } else {

                int width = newBox.getWidth();
                int height = newBox.getHeight();

                // Perfectly fits. Nice!
                if (node.mWidth == width && node.mHeight == height) {
                    node.mFilledLeaf = true;
                    node.mBox = newBox;
                    return node;
                } else if (node.mWidth >= width && node.mHeight >= height) {

                    int remainingX = node.mWidth - width;
                    int remainingY = node.mHeight - height;

                    BoxNode<T> newLeft;
                    BoxNode<T> newRight;

                    if (remainingX < remainingY) {
                        newLeft = new BoxNode<T>(node.mX, node.mY, node.mWidth, height);
                        newRight =
                                new BoxNode<T>(
                                        node.mX,
                                        node.mY + height,
                                        node.mWidth,
                                        node.mHeight - height);
                    } else {
                        newLeft = new BoxNode<T>(node.mX, node.mY, width, node.mHeight);
                        newRight =
                                new BoxNode<T>(
                                        node.mX + width,
                                        node.mY,
                                        node.mWidth - width,
                                        node.mHeight);
                    }

                    node.mLeft = newLeft;
                    node.mRight = newRight;

                    return pack(node.mLeft, newBox);
                } else {
                    return null;
                }
            }
        }

        public BoxNode<T> pack(T newBox) {
            return pack(mRoot, newBox);
        }
    }

    private Map<Integer, BoxNode<Glyph>> mCharToGlyph = new TreeMap<>();

    private static final int[][] GLYPH_RANGES = {
        {' ', '~'},
    };

    private static final int ATLAS_SIZE = 1024;
    private static final int LINE_HEIGHT = 200;

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
                                                    0,
                                                    pOffsetToStart.get(0),
                                                    pOffsetToNext.get(0),
                                                    i));
                                }
                            }

                            glyphList.sort(
                                    (a, b) ->
                                            -Integer.compare(
                                                    a.getWidth() * a.getHeight(),
                                                    b.getWidth() * b.getHeight()));

                            for (Glyph glyph : glyphList) {
                                BoxNode<Glyph> packedGlyph = packer.pack(glyph);

                                ret.mBuffer.rewind();
                                tmpBuffer.rewind();
                                stbtt_MakeCodepointBitmap(
                                        info,
                                        tmpBuffer,
                                        glyph.mWidth,
                                        glyph.mHeight,
                                        LINE_HEIGHT * 2,
                                        scale,
                                        scale,
                                        glyph.mCode);
                                tmpBuffer.rewind();

                                for (int i = 0; i < glyph.mHeight; i++) {
                                    for (int o = 0; o < glyph.mWidth; o++) {
                                        ret.mBuffer.position(
                                                ((packedGlyph.mX + o)
                                                                + (packedGlyph.mY + i) * ret.mWidth)
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
