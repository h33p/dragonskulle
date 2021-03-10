/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import static java.lang.Math.sqrt;

import java.util.Arrays;
import lombok.extern.java.Log;

/** The Hexagon Tile Object */
@Log
public class HexagonTile {

    /** This is the axial storage system for each tile */
    private final int q;

    private final int r;
    private final int s;

    HexagonTile(int q, int r, int s) {
        this.q = q;
        this.r = r;
        this.s = s;
        if (q + r + s != 0) {
            log.warning("The coordinates do not add up to 0");
        }
    }

    /** The length of the tile */
    private int length() {
        return (int) ((Math.abs(q) + Math.abs(r) + Math.abs(s)) / 2);
    }

    /** Rotation matrix for converting to cartesian form */
    private static final Matrix_2D rotationMatrix =
            new Matrix_2D(sqrt(3.0), sqrt(3.0) / 2.0, 0.0, 3.0 / 2.0);

    @Override
    public String toString() {
        return Arrays.toString(new int[] {this.q, this.r, this.s});
    }
}
