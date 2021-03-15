/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Renderable;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.renderer.*;
import org.dragonskulle.renderer.TextureMapping.TextureFiltering;
import org.dragonskulle.renderer.TextureMapping.TextureWrapping;

/**
 * @author Leela Muppala
 *     <p>Creates each HexagonTile with their 3 coordinates. This stores information about the axial
 *     coordinates of each tile.
 */
@Log
@Accessors(prefix = "m")
public class HexagonTile {
    static final GameObject LAND_TILE =
            new GameObject(
                    "land",
                    (go) -> {
                        Mesh mesh = Mesh.HEXAGON;
                        SampledTexture texture =
                                new SampledTexture(
                                        "map/grass.png",
                                        new TextureMapping(
                                                TextureFiltering.NEAREST, TextureWrapping.REPEAT));
                        UnlitMaterial mat = new UnlitMaterial(texture);
                        go.addComponent(new Renderable(mesh, mat));
                    });

    /** This is the axial storage system for each tile */
    @Getter private final int mQ;

    @Getter private final int mR;

    @Getter private final int mS;

    @Getter(AccessLevel.PACKAGE)
    private final GameObject mGameObject;

    /**
     * Constructor that creates the HexagonTile with a test to see if all the coordinates add up to
     * 0.
     *
     * @param q The first coordinate.
     * @param r The second coordinate.
     * @param s The third coordinate.
     */
    HexagonTile(int q, int r, int s) {
        this.mQ = q;
        this.mR = r;
        this.mS = s;
        mGameObject = GameObject.instantiate(LAND_TILE, new TransformHex(mQ, mR));
        if (q + r + s != 0) {
            log.warning("The coordinates do not add up to 0");
        }
    }

    /** The length of the tile from the origin */
    public int length() {
        return (int) ((Math.abs(mQ) + Math.abs(mR) + Math.abs(mS)) / 2);
    }

    @Override
    public String toString() {
        return Arrays.toString(new int[] {this.mQ, this.mR, this.mS});
    }
}
