/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.misc;

import org.dragonskulle.utils.MathUtils;
import org.joml.Vector3f;

/**
 * Allows to work with colour temperatures.
 *
 * @author Aurimas Blažulionis
 *     <p>Taken from:
 *     https://tannerhelland.com/2012/09/18/convert-temperature-rgb-algorithm-code.html
 */
public class Temperature {

    /**
     * Convert kelvin colour temperature to RGB float values.
     *
     * @param kelvin input temperature.
     * @param min minimum RGB clamp value.
     * @param dest destination vector
     * @return dest.
     */
    public static Vector3f colourTemperature(float kelvin, float min, Vector3f dest) {
        kelvin *= 0.01f;

        if (kelvin <= 66) {
            dest.x = 1;
            dest.y = MathUtils.clamp(0.39f * (float) Math.log(kelvin) - 0.63184f, min, 1);
            if (kelvin <= 11) {
                dest.z = 0;
            } else {
                dest.z =
                        MathUtils.clamp(0.5432f * (float) Math.log(kelvin - 10) - 1.19625f, min, 1);
            }
        } else {
            dest.x = MathUtils.clamp(1.283f * (float) Math.pow(kelvin - 60, -0.1332047592), min, 1);
            dest.y = MathUtils.clamp(1.13f * (float) Math.pow(kelvin - 60, -0.0755148492), min, 1);
            dest.z = 1;
        }

        return dest;
    }
}
