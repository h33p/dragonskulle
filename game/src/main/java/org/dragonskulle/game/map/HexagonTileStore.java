/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import com.flowpowered.noise.Noise;
import com.flowpowered.noise.NoiseQuality;
import java.util.Arrays;
import java.util.stream.Stream;
import lombok.extern.java.Log;
import org.dragonskulle.utils.MathUtils;

/**
 * @author Leela Muppala
 *     <p>This class generates and stores a map of tiles with appropriate coordinates. Hexagon map
 *     objects are also created and stored.
 */
@Log
class HexagonTileStore {
    private HexagonTile[][] mTiles;
    private final int mCoordShift;
    private final int mSeed;

    /** Hex(q,r) is stored as array[r+shift][q+shift] Map is created and stored in HexMap. */
    public HexagonTileStore(int size, int seed) {
        mTiles = new HexagonTile[size][size];
        mSeed = seed;
        mCoordShift = size / 2;

        int max_empty = getSpaces(size); // The max number of empty spaces in one row of the array
        int loop = size / 2;
        int empty = max_empty;

        /* Generates the first part of the map */
        for (int r = 0; r < loop; r++) {

            int inside_empty = empty;

            for (int q = 0; q < size; q++) {
                if (inside_empty > 0) {
                    // No tile in this location
                    inside_empty--;
                } else {
                    int q1 = q - mCoordShift;
                    int r1 = r - mCoordShift;
                    float height = getHeight(q1, r1);
                    setTile(new HexagonTile(q1, r1, height));
                }
            }
            empty--;
        }

        /* Generates the middle part of the map */
        int r_m = (size / 2);
        for (int q = 0; q < size; q++) {
            int q1 = q - mCoordShift;
            int r1 = r_m - mCoordShift;
            float height = getHeight(q1, r1);
            setTile(new HexagonTile(q1, r1, height));
        }

        /* Generates the last part of the map */
        loop = (size / 2) + 1;
        int min_val = size - max_empty; // TODO is this needed?  Doesn't seem to be used
        int current_val = size;
        for (int r = loop; r < size; r++) {
            current_val--; // The number of cells with actual coordinates, it decreases with every
            // row
            int inside_val = 1;
            for (int q = 0; q < size; q++) {
                if (inside_val <= current_val) {
                    int q1 = q - mCoordShift;
                    int r1 = r - mCoordShift;
                    float height = getHeight(q1, r1);
                    setTile(new HexagonTile(q1, r1, height));
                    inside_val++;
                } // otherwise we do not need a tile
            }
        }
    }

    /**
     * Get a hexagon tile.
     *
     * @param q q coordinate of the tile
     * @param r r coordinate of the tile
     */
    public HexagonTile getTile(int q, int r) {
        q += mCoordShift;
        r += mCoordShift;

        if (q < 0 || r < 0 || q >= mTiles.length || r >= mTiles.length) {
            return null;
        }

        return mTiles[q][r];
    }

    /**
     * Get a stream of all hexagon tiles.
     *
     * @return stream of all non-null hexagon tiles in the map
     */
    public Stream<HexagonTile> getAllTiles() {
        return Arrays.stream(mTiles).flatMap(Arrays::stream).filter(x -> x != null);
    }

    private void setTile(HexagonTile tile) {
        int q = tile.getQ() + mCoordShift;
        int r = tile.getR() + mCoordShift;
        mTiles[q][r] = tile;
    }

    /**
     * Provides a number that shows the number of nulls to add to a HexagonMap. Can only input odd
     * numbered size due to hexagon shape.
     *
     * @param input - The size of the hexagon map.
     * @return Returns the number of spaces to add in the 2d array to generate a hexagon shape.
     */
    private static int getSpaces(int input) {
        if (input % 2 == 0) {
            log.warning("The size is not an odd number");
        }
        return (input - 1) / 2;
    }

    private static final float NOISE_STEP = 0.2f;

    private static final float[][] OCTAVES = {
        {0.1f, 0.9f},
        {0.3f, 0.2f},
        {0.6f, 0.1f}
    };

    private float getHeight(int q, int r) {

        float sum = 0f;

        for (float[] vals : OCTAVES) {
            sum +=
                    (float)
                                    Noise.valueCoherentNoise3D(
                                            q * vals[0],
                                            r * vals[0],
                                            (-q - r) * vals[0],
                                            mSeed,
                                            NoiseQuality.BEST)
                            * vals[1];
        }

        return MathUtils.roundStep(sum, NOISE_STEP);
    }
}
