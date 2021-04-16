/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

import org.joml.Vector4f;

/**
 * Interface for materials with main colour value.
 *
 * @author Aurimas Blažulionis
 */
public interface IColouredMaterial extends IMaterial {
    /**
     * Get the main colour of the material.
     *
     * <p>This is up to the material to decide what is the default colour, but usually it is the
     * diffuse colour
     *
     * @return the default colour of the material. This reference should be directly modifiable
     */
    Vector4f getColour();

    /**
     * Gets the alpha value of the main colour.
     *
     * @return the alpha value
     */
    default float getAlpha() {
        return getColour().w;
    }

    /**
     * Sets the alpha (transparency) value of the main colour.
     *
     * @param alpha the new alpha value
     */
    default void setAlpha(float alpha) {
        getColour().w = alpha;
    }
}
