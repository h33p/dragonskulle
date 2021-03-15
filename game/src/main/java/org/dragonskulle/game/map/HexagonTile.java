/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.Arrays;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * @author Leela Muppala
 *     <p>Creates each HexagonTile with their 3 coordinates. This stores information about the axial
 *     coordinates of each tile.
 */
@Accessors(prefix = "m")
@Log
public class HexagonTile {

    /** This is the axial storage system for each tile */
    @Getter private final int mQ;

    @Getter private final int mR;

    @Getter private final int mS;

    /**
     * Constructor that creates the HexagonTile with a test to see if all the coordinates add up to
     * 0.
     *
     * @param q The first coordinate.
     * @param r The second coordinate.
     * @param s The third coordinate.
     */
    public HexagonTile(int q, int r, int s) {
        this.mQ = q;
        this.mR = r;
        this.mS = s;
        if (q + r + s != 0) {
            log.warning("The coordinates do not add up to 0");
        }
    }

    /** The length of the tile */
    private int length() {
        return (int) ((Math.abs(mQ) + Math.abs(mR) + Math.abs(mS)) / 2);
    }

    @Override
    public String toString() {
        return Arrays.toString(new int[] {this.mQ, this.mR, this.mS});
    }
}
