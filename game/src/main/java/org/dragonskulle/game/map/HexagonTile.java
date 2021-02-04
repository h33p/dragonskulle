package org.dragonskulle.game.map;

import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.sqrt;

public class HexagonTile {
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

    public HexagonTile add(HexagonTile b) {
        return new HexagonTile(q + b.q, r + b.r, s + b.s);
    }


    public HexagonTile subtract(HexagonTile b) {
        return new HexagonTile(q - b.q, r - b.r, s - b.s);
    }


    public HexagonTile scale(int k) {
        return new HexagonTile(q * k, r * k, s * k);
    }


    public HexagonTile rotateLeft() {
        return new HexagonTile(-s, -q, -r);
    }


    public HexagonTile rotateRight() {
        return new HexagonTile(-r, -s, -q);
    }

    static public ArrayList<HexagonTile> directions = new ArrayList<HexagonTile>() {{
        add(new HexagonTile(1, 0, -1));
        add(new HexagonTile(1, -1, 0));
        add(new HexagonTile(0, -1, 1));
        add(new HexagonTile(-1, 0, 1));
        add(new HexagonTile(-1, 1, 0));
        add(new HexagonTile(0, 1, -1));
    }};

    static public HexagonTile direction(int direction) {
        return HexagonTile.directions.get(direction);
    }


    public HexagonTile neighbor(int direction) {
        return add(HexagonTile.direction(direction));
    }

    static public ArrayList<HexagonTile> diagonals = new ArrayList<HexagonTile>() {{
        add(new HexagonTile(2, -1, -1));
        add(new HexagonTile(1, -2, 1));
        add(new HexagonTile(-1, -1, 2));
        add(new HexagonTile(-2, 1, 1));
        add(new HexagonTile(-1, 2, -1));
        add(new HexagonTile(1, 1, -2));
    }};

    public HexagonTile diagonalNeighbor(int direction) {
        return add(HexagonTile.diagonals.get(direction));
    }


    public int length() {
        return (int) ((Math.abs(q) + Math.abs(r) + Math.abs(s)) / 2);
    }


    public int distance(HexagonTile b) {
        return subtract(b).length();
    }

    private static final Matrix_2D rotationMatrix = new Matrix_2D(sqrt(3.0), sqrt(3.0) / 2.0, 0.0, 3.0 / 2.0);;
    public CartesianVector toCartesian(){
        double x = (rotationMatrix.a * this.q + rotationMatrix.b * this.r);
        double y = (rotationMatrix.c * this.q + rotationMatrix.d * this.r);
        return new CartesianVector(x, y);
    }

    @Override
    public String toString(){
        return Arrays.toString(new int[]{this.q, this.r, this.s});
    }
}