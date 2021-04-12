/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

import org.joml.Vector4f;

/**
 * Interface for materials with main colour value.
 *
 * @author Aurimas Blažulionis
 */
public interface IColouredMaterial extends IMaterial {
    Vector4f getColour();

    default float getAlpha() {
        return getColour().w;
    }

    default void setAlpha(float alpha) {
        getColour().w = alpha;
    }
}
