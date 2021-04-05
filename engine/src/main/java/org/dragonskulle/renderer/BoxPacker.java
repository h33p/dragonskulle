/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Box packer. Based on lightmap packing techniques.
 *
 * <p>One of which: https://blackpawn.com/texts/lightmaps/default.html
 *
 * @author Aurimas Blažulionis
 */
class BoxPacker<T extends IBox> {

    /** Represents a packed box entry */
    @Accessors(prefix = "m")
    public static class BoxNode<T extends IBox> {
        @Getter private T mBox;
        @Getter private int mX;
        @Getter private int mY;
        @Getter private int mWidth;
        @Getter private int mHeight;
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

    private BoxNode<T> mRoot;

    public BoxPacker(int width, int height) {
        mRoot = new BoxNode<T>(0, 0, width, height);
    }

    // TODO: Add resizing support

    private BoxNode<T> pack(BoxNode<T> node, T newBox, int gap) {

        if (node.mFilledLeaf) {
            return null;
        } else if (node.mLeft != null && node.mRight != null) {
            BoxNode<T> ret = pack(node.mLeft, newBox, gap);
            if (ret == null) ret = pack(node.mRight, newBox, gap);
            return ret;
        } else {

            int width = newBox.getWidth() + gap;
            int height = newBox.getHeight() + gap;

            // Perfectly fits. Nice!
            if (node.mWidth == width && node.mHeight == height) {
                node.mFilledLeaf = true;
                node.mBox = newBox;
                node.mWidth -= gap;
                node.mHeight -= gap;
                return node;
            } else if (node.mWidth >= width && node.mHeight >= height) {

                int remainingX = node.mWidth - width;
                int remainingY = node.mHeight - height;

                BoxNode<T> newLeft;
                BoxNode<T> newRight;

                if (remainingX < remainingY) {
                    newLeft = new BoxNode<>(node.mX, node.mY, node.mWidth, height);
                    newRight =
                            new BoxNode<>(
                                    node.mX, node.mY + height, node.mWidth, node.mHeight - height);
                } else {
                    newLeft = new BoxNode<>(node.mX, node.mY, width, node.mHeight);
                    newRight =
                            new BoxNode<>(
                                    node.mX + width, node.mY, node.mWidth - width, node.mHeight);
                }

                node.mLeft = newLeft;
                node.mRight = newRight;

                return pack(node.mLeft, newBox, gap);
            } else {
                return null;
            }
        }
    }

    /**
     * Pack a box
     *
     * <p>This method will pack elements with specified minimum gap between them
     *
     * @param newBox box to pack
     * @param gap gap between elements
     * @return packed box, if found a place. {@code null otherwise}
     */
    public BoxNode<T> pack(T newBox, int gap) {
        return pack(mRoot, newBox, gap);
    }

    /**
     * Pack a box
     *
     * <p>This method will pack elements with no minimum gaps between them
     *
     * @param newBox box to pack
     * @return packed box, if found a place. {@code null otherwise}
     */
    public BoxNode<T> pack(T newBox) {
        return pack(newBox, 0);
    }
}
