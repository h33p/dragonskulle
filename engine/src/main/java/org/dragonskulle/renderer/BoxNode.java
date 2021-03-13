/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
class BoxNode<T extends IBox> {
    @Getter private T mBox;
    @Getter private int mX;
    @Getter private int mY;
    @Getter private int mWidth;
    @Getter private int mHeight;
    @Getter private BoxNode<T> mLeft;
    @Getter private BoxNode<T> mRight;
    @Getter private boolean mFilledLeaf = false;

    public BoxNode(int x, int y, int width, int height) {
        mX = x;
        mY = y;
        mWidth = width;
        mHeight = height;
    }
}
