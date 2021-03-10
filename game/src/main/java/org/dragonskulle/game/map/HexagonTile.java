/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import static java.lang.Math.sqrt;

import java.util.Arrays;
import lombok.extern.java.Log;

/** The Hexagon Tile Object */
@Log
public class HexagonTile {

    /** This is the axial storage system for each tile */
    private final int mQ;

    private final int mR;
    private final int mS;

    HexagonTile(int q, int r, int s) {
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

    /** Rotation matrix for converting to cartesian form */
    private static final Matrix_2D rotationMatrix =
            new Matrix_2D(sqrt(3.0), sqrt(3.0) / 2.0, 0.0, 3.0 / 2.0);

    @Override
    public String toString() {
        return Arrays.toString(new int[] {this.mQ, this.mR, this.mS});
    }
}
