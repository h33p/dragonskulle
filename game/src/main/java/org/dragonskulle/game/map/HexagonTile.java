/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.materials.HighlightControls;
import org.dragonskulle.game.player.Player;

/**
 * @author Leela Muppala
 *     <p>Creates each HexagonTile with their 3 coordinates. This stores information about the axial
 *     coordinates of each tile.
 */
@Log
@Accessors(prefix = "m")
public class HexagonTile {

    static final Resource<GLTF> TEMPLATES = GLTF.getResource("templates");

    /** Describes a template for land hex tile */
    static final GameObject LAND_TILE =
            TEMPLATES.get().getDefaultScene().getGameObjects().stream()
                    .filter(go -> go.getName().equals("Land Hex"))
                    .findFirst()
                    .orElse(null);

    /** Describes a template for water hex tile */
    static final GameObject WATER_TILE =
            TEMPLATES.get().getDefaultScene().getGameObjects().stream()
                    .filter(go -> go.getName().equals("Water Hex"))
                    .findFirst()
                    .orElse(null);

    /** Describes a template for water hex tile */
    static final GameObject MOUNTAIN_TILE =
            TEMPLATES.get().getDefaultScene().getGameObjects().stream()
                    .filter(go -> go.getName().equals("Mountains Hex"))
                    .findFirst()
                    .orElse(null);

    public static enum TileType {
        LAND((byte) 0),
        WATER((byte) 1),
        MOUNTAIN((byte) 2);

        @Getter private final byte mValue;

        private TileType(byte value) {
            mValue = value;
        }
    }

    private static final float WATER_THRESHOLD = -0.3f;
    private static final float MOUNTAINS_THRESHOLD = 0.8f;

    /** This is the axial storage system for each tile */
    @Getter private final int mQ;

    @Getter private final int mR;

    @Getter private final int mS;

    @Getter private final float mHeight;

    @Getter private final TileType mTileType;

    /**
     * Associated game object.
     *
     * <p>This is specifically package-only, since the game does not need to know about underlying
     * tile objects.
     */
    @Getter(AccessLevel.PACKAGE)
    private final GameObject mGameObject;

    @Getter(AccessLevel.PACKAGE)
    private final Reference<HighlightControls> mHighlightControls;

    /** Building that is on the tile */
    @Getter @Setter private Building mBuilding;

    /** A reference to the building that claims the tile, or {@code null}. */
    private Reference<Building> mClaimedBy = new Reference<Building>(null);

    /**
     * Constructor that creates the HexagonTile with a test to see if all the coordinates add up to
     * 0.
     *
     * @param q The first coordinate.
     * @param r The second coordinate.
     * @param s The third coordinate.
     */
    HexagonTile(int q, int r, int s, float height) {
        this.mQ = q;
        this.mR = r;
        this.mS = s;
        this.mHeight = height;
        if (q + r + s != 0) {
            log.warning("The coordinates do not add up to 0");
        }

        if (height <= WATER_THRESHOLD) mTileType = TileType.WATER;
        else if (height >= MOUNTAINS_THRESHOLD) mTileType = TileType.MOUNTAIN;
        else mTileType = TileType.LAND;

        switch (mTileType) {
            case WATER:
                mGameObject =
                        GameObject.instantiate(
                                WATER_TILE, new TransformHex(mQ, mR, WATER_THRESHOLD));
                mGameObject
                        .findChildByName("Water Floor")
                        .getTransform(Transform3D.class)
                        .setPosition(0f, 0f, height - WATER_THRESHOLD - 0.1f);
                break;
            case MOUNTAIN:
                mGameObject =
                        GameObject.instantiate(MOUNTAIN_TILE, new TransformHex(mQ, mR, height));
                break;
            default:
                mGameObject = GameObject.instantiate(LAND_TILE, new TransformHex(mQ, mR, height));
        }

        Reference<HighlightControls> controls = mGameObject.getComponent(HighlightControls.class);
        if (controls == null) mHighlightControls = new Reference<>(null);
        else mHighlightControls = controls;
    }

    /**
     * The length of the tile from the origin.
     *
     * @return The length of the tile from the origin.
     */
    public int length() {
        return (int) ((Math.abs(mQ) + Math.abs(mR) + Math.abs(mS)) / 2);
    }

    public int distTo(int q, int r) {
        int s = -q - r;

        return (int) ((Math.abs(q - mQ) + Math.abs(r - mR) + Math.abs(s - mS)) / 2);
    }

    @Override
    public String toString() {
        return Arrays.toString(new int[] {this.mQ, this.mR, this.mS});
    }

    /**
     * Set which {@link Building} claims the tile. Do not claim the tile if another building already
     * claimed it.
     *
     * @param building The building which claimed the tile.
     * @return {@code true} if the claim was successful, otherwise {@code false} if the tile is
     *     already claimed.
     */
    public boolean setClaimedBy(Building building) {
        if (isClaimed()) return false;

        mClaimedBy = building.getReference(Building.class);
        return true;
    }

    /**
     * Whether the tile is claimed by a building.
     *
     * @return Whether the tile is claimed by a building.
     */
    public boolean isClaimed() {
        return (mClaimedBy != null && mClaimedBy.isValid());
    }

    /**
     * Get the {@link Player} who has claimed this tile (either because there is a building on it or
     * because it is adjacent to a building).
     *
     * @return The Player who has claimed this tile, or {@code null} if no Player claims it.
     */
    public Player getClaimant() {
        if (!isClaimed()) {
            return null;
        }
        return mClaimedBy.get().getOwner();
    }

    /**
     * Get whether there is a {@link Building} on this tile.
     *
     * @return Whether there is a building on this tile.
     */
    public boolean hasBuilding() {
        return mBuilding != null;
    }
}
