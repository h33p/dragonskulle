/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import static java.lang.Math.sqrt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

// based on https://www.redblobgames.com/grids/hexagons/implementation.html

/** The Hexagon Tile Object */
public class HexagonTile implements Serializable {

    /**
     * This is the axial storage system for each tile
     *
     * @param q
     * @param r
     * @param s
     */
    public HexagonTile(int q, int r, int s) {
        this.q = q;
        this.r = r;
        this.s = s;
        if (q + r + s != 0) {
            throw new IllegalArgumentException("q + r + s must be 0");
        }
    }

    public final int q;
    public final int r;
    public final int s;

    /** These arithmetic operations on two vectors for moving from one hex to another */
    public HexagonTile add(HexagonTile b) {
        return new HexagonTile(q + b.q, r + b.r, s + b.s);
    }

    public HexagonTile subtract(HexagonTile b) {
        return new HexagonTile(q - b.q, r - b.r, s - b.s);
    }

    /** Scales tile by k */
    public HexagonTile scale(int k) {
        return new HexagonTile(q * k, r * k, s * k);
    }

    /** rotates the Tile left */
    public HexagonTile rotateLeft() {
        return new HexagonTile(-s, -q, -r);
    }

    /** rotates the tile right */
    public HexagonTile rotateRight() {
        return new HexagonTile(-r, -s, -q);
    }

    /**
     * Moving one space in hex coordinates involves changing one of the three coordinates by +1 and
     * changing another by -1 (the sum must remain 0) This results in 6 possible changes, 6
     * different permutations Offsets for each direction by adding offset tile
     */
    public static ArrayList<HexagonTile> directions =
            new ArrayList<HexagonTile>() {
                {
                    add(new HexagonTile(1, 0, -1));
                    add(new HexagonTile(1, -1, 0));
                    add(new HexagonTile(0, -1, 1));
                    add(new HexagonTile(-1, 0, 1));
                    add(new HexagonTile(-1, 1, 0));
                    add(new HexagonTile(0, 1, -1));
                }
            };

    /**
     * Getter for a particular direction
     *
     * @param direction - an int to retrieve the direction
     * @return a particular direction from the ArrayList of directions
     */
    public static HexagonTile direction(int direction) {
        return HexagonTile.directions.get(direction);
    }

    /**
     * Getter for particular neighbour
     *
     * @param direction - the direction is one of the directions from the ArrayList of directions
     * @return neighbouring HexTile by adding a HexTile and the direction that is passes as the
     *     parameter
     */
    public HexagonTile neighbor(int direction) {
        return add(HexagonTile.direction(direction));
    }

    /**
     * This moves to a diagonal space in the hex coordinates by changing one of the 3 coordinates by
     * +-2 and the other two by Diagonal neighbours by adding offsets
     */
    public static ArrayList<HexagonTile> diagonals =
            new ArrayList<HexagonTile>() {
                {
                    add(new HexagonTile(2, -1, -1));
                    add(new HexagonTile(1, -2, 1));
                    add(new HexagonTile(-1, -1, 2));
                    add(new HexagonTile(-2, 1, 1));
                    add(new HexagonTile(-1, 2, -1));
                    add(new HexagonTile(1, 1, -2));
                }
            };

    /**
     * Getter for particular diagonal neighbour
     *
     * @param direction - an int parameter that get one of the diagonal neighbours
     * @return a neighbouring HexTile by adding a HexTile and a diagonal direction
     */
    public HexagonTile diagonalNeighbor(int direction) {
        return add(HexagonTile.diagonals.get(direction));
    }

    /** The length of the tile */
    public int length() {
        return (int) ((Math.abs(q) + Math.abs(r) + Math.abs(s)) / 2);
    }

    /** Distance between two tiles */
    public int distance(HexagonTile b) {
        return subtract(b).length();
    }

    /** Rotation matrix for converting to cartesian form */
    private static final Matrix_2D rotationMatrix =
            new Matrix_2D(sqrt(3.0), sqrt(3.0) / 2.0, 0.0, 3.0 / 2.0);

    /** Getter for cartesian form of the tile */
    public CartesianVector getCartesian() {
        double x = (rotationMatrix.a * this.q + rotationMatrix.b * this.r);
        double y = (rotationMatrix.c * this.q + rotationMatrix.d * this.r);
        return new CartesianVector(x, y);
    }

    @Override
    public String toString() {
        return Arrays.toString(new int[] {this.q, this.r, this.s});
    }
}
