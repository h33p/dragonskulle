/* (C) 2021 DragonSkulle */

package org.dragonskulle.renderer;

/**
 * Describes a single generic box
 *
 * <p>This interface is used within {@link BoxPacker} to allow packing boxes into bigger boxes.
 *
 * @author Aurimas Blažulionis
 */

interface IBox {
    /** Get the width of underlying box. */

    int getWidth();

    /** Get the height of underlying box. */

    int getHeight();
}
