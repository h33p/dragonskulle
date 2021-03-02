/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import static java.lang.Math.*;

import java.util.Arrays;

public class CartesianVector {
    private static final Matrix_2D rotationMatrix =
            new Matrix_2D(sqrt(3.0) / 3.0, -1.0 / 3.0, 0.0, 2.0 / 3.0);

    public CartesianVector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public final double x;
    public final double y;

    public AxialCoordinates toAxial() {
        double q = rotationMatrix.a * this.x + rotationMatrix.b * this.y;
        double r = rotationMatrix.c * this.x + rotationMatrix.d * this.y;
        return axialRound(
                q, r,
                -q - r); // will be fractional so need to convert from fractional to nearest int
    }

    AxialCoordinates axialRound(double q_frac, double r_frac, double s_frac) {
        int q = (int) (round(q_frac));
        int r = (int) (round(r_frac));
        int s = (int) (round(s_frac));
        double q_diff = abs(q - q_frac);
        double r_diff = abs(r - r_frac);
        double s_diff = abs(s - s_frac);
        if (q_diff > r_diff && q_diff > s_diff) {
            q = -r - s;
        } else if (r_diff > s_diff) {
            r = -q - s;
        } else {
            s = -q - r;
        }
        return new AxialCoordinates(q, r, s);
    }

    @Override
    public String toString() {
        return Arrays.toString(new double[] {this.x, this.y});
    }
}
