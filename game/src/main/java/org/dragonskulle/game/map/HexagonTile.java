/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.game.building.Building;
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

    /** This is the axial storage system for each tile */
    @Getter private final int mQ;

    @Getter private final int mR;

    @Getter private final int mS;

    /**
     * Associated game object.
     *
     * <p>This is specifically package-only, since the game does not need to know about underlying
     * tile objects.
     */
    @Getter(AccessLevel.PACKAGE)
    private final GameObject mGameObject;

    /** Reference to the {@link Building} that is on the tile. */
    private Reference<Building> mBuilding = new Reference<Building>(null);

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
    HexagonTile(int q, int r, int s) {
        this.mQ = q;
        this.mR = r;
        this.mS = s;
        mGameObject = GameObject.instantiate(LAND_TILE, new TransformHex(mQ, mR));
        if (q + r + s != 0) {
            log.warning("The coordinates do not add up to 0");
        }
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
     * Get the ID of the Player who claimed the HexagonTile.
     *
     * <p>If {@link Player} does not need to be accessed, or is only accessed to get the owner ID,
     * then this should be used.
     *
     * @return The owner ID of the Player as an {@link Integer}, or {@code null} if there is no
     *     claimant.
     */
    public Integer getClaimantId() {
        if (!isClaimed()) {
            return null;
        }
        return mClaimedBy.get().getOwnerId();
    }

    /**
     * Store the {@link Building} on the HexagonTile.
     *
     * <p>To stop storing the Building, {@code null} can be provided.
     *
     * @param building The Building on the tile, or {@code null} if there is no Building.
     */
    public void setBuilding(Building building) {
        // If null is provided, set a reference to null.
        if (building == null) {
            mBuilding = new Reference<Building>(null);
            return;
        }
        mBuilding = building.getReference(Building.class);
    }

    /**
     * Get the {@link Building} on the tile, if it exists.
     *
     * @return The Building on the HexagonTile, otherwise {@code null}.
     */
    public Building getBuilding() {
        if (mBuilding == null || mBuilding.isValid() == false) return null;

        return mBuilding.get();
    }

    /**
     * Get whether there is a {@link Building} on this tile.
     *
     * @return Whether there is a building on this tile.
     */
    public boolean hasBuilding() {
        return getBuilding() != null;
    }
}
