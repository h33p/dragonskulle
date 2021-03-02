/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import static java.lang.Math.sqrt;

public class AxialCoordinates {
    public AxialCoordinates(double q, double r, double s) {
        this.q = q;
        this.r = r;
        this.s = s;
    }

    public final double q;
    public final double r;
    public final double s;

    private static final Matrix_2D rotationMatrix =
            new Matrix_2D(sqrt(3.0), sqrt(3.0) / 2.0, 0.0, 3.0 / 2.0);;

    public CartesianVector toCartesian() {
        double x = (rotationMatrix.a * this.q + rotationMatrix.b * this.r);
        double y = (rotationMatrix.c * this.q + rotationMatrix.d * this.r);
        return new CartesianVector(x, y);
    }
}
