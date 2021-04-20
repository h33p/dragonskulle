/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import com.flowpowered.noise.Noise;
import com.flowpowered.noise.NoiseQuality;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * @author Aurimas Blažulionis This class allows to conveniently generate noise using input octaves
 */
@Accessors(prefix = "m")
@Log
public class NoiseUtil {
    /**
     * Get noise height
     *
     * @param q first coordinate axis
     * @param r second coordinate axis
     * @param seed noise seed
     * @param octaves octaves used for noise. It's multiple rows with 3 columns on each row
     * @return float noise value
     */
    public static float getHeight(int q, int r, int seed, float[][] octaves) {
        float sum = 0f;

        for (float[] vals : octaves) {
            sum +=
                    (float)
                                    (Noise.valueCoherentNoise3D(
                                                    q * vals[0],
                                                    r * vals[0],
                                                    (-q - r) * vals[0],
                                                    seed,
                                                    NoiseQuality.BEST)
                                            + vals[2])
                            * vals[1];
        }

        return sum;
    }
}
