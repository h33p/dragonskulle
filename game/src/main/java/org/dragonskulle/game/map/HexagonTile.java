/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTileStore.TileToStoreActions;
import org.dragonskulle.game.materials.HighlightControls;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * @author Leela Muppala
 *     <p>Creates each HexagonTile with their 3 coordinates. This stores information about the axial
 *     coordinates of each tile.
 */
@Log
@Accessors(prefix = "m")
public class HexagonTile implements INetSerializable {

    static final Resource<GLTF> TEMPLATES = GLTF.getResource("templates");

    /** Describes a template for land hex tile. */
    static final GameObject LAND_TILE =
            TEMPLATES.get().getDefaultScene().findRootObject("Land Hex");

    /** Describes a template for water hex tile. */
    static final GameObject WATER_TILE =
            TEMPLATES.get().getDefaultScene().findRootObject("Water Hex");

    /** Describes a template for water hex tile. */
    static final GameObject MOUNTAIN_TILE =
            TEMPLATES.get().getDefaultScene().findRootObject("Mountains Hex");

    public static enum TileType {
        LAND((byte) 0),
        WATER((byte) 1),
        MOUNTAIN((byte) 2);

        @Getter private final byte mValue;

        private static final TileType[] VALUES = TileType.values();

        private TileType(byte value) {
            mValue = value;
        }

        static TileType getTile(byte value) {
            for (TileType t : VALUES) {
                if (t.mValue == value) {
                    return t;
                }
            }
            return null;
        }
    }

    private static final float WATER_THRESHOLD = -0.3f;
    private static final float MOUNTAINS_THRESHOLD = 0.8f;

    /** This is the axial storage system for each tile. */
    @Getter private final int mQ;

    @Getter private final int mR;

    @Getter private float mHeight;

    @Getter private TileType mTileType;

    private final TileToStoreActions mHandler;

    /** This states which land mass this tile is on. Set at -1 when not set */
    int landMassNumber = -1;
    /**
     * Associated game object.
     *
     * <p>This is specifically package-only, since the game does not need to know about underlying
     * tile objects.
     */
    @Getter(AccessLevel.PACKAGE)
    private GameObject mGameObject;

    private Reference<HeightController> mHeightController;
    private Reference<HeightController> mSecondaryController;

    @Getter(AccessLevel.PACKAGE)
    private Reference<HighlightControls> mHighlightControls;

    /** Reference to the {@link Building} that is on the tile. */
    private Reference<Building> mBuilding = new Reference<Building>(null);

    /** A reference to the building that claims the tile, or {@code null}. */
    private Reference<Building> mClaimedBy = null;

    /**
     * Constructor that creates the HexagonTile.
     *
     * @param q The first coordinate.
     * @param r The second coordinate.
     * @param handler handler passed by {@link HexagonTileStore} to be called on changes
     */
    HexagonTile(int q, int r, float height, TileToStoreActions handler) {
        this.mQ = q;
        this.mR = r;
        this.mHeight = handler.getNetworkManager().isClient() ? WATER_THRESHOLD : height;
        this.mHandler = handler;

        if (height <= WATER_THRESHOLD) {
            mTileType = TileType.WATER;
        } else if (height >= MOUNTAINS_THRESHOLD) {
            mTileType = TileType.MOUNTAIN;
        } else {
            mTileType = TileType.LAND;
        }

        buildGameObject();
    }

    /**
     * The length of the tile from the origin.
     *
     * @return The length of the tile from the origin.
     */
    public int length() {
        return (int) ((Math.abs(mQ) + Math.abs(mR) + Math.abs(getS())) / 2);
    }

    public int distTo(int q, int r) {
        int s = -q - r;

        return (int) ((Math.abs(q - mQ) + Math.abs(r - mR) + Math.abs(s - getS())) / 2);
    }

    /**
     * Retrieve the third (cube) coordinate.
     *
     * <p>This coordinate will always be equal to -getQ() -getR()
     */
    public int getS() {
        return -mQ - mR;
    }

    @Override
    public String toString() {
        return Arrays.toString(new int[] {this.mQ, this.mR});
    }

    /**
     * Set which {@link Building} claims the HexagonTile. Cannot claim the tile if another building
     * already claimed it.
     *
     * @param building The Building which claimed the tile.
     * @return {@code true} if the claim was successful; otherwise {@code false}.
     */
    public boolean setClaimedBy(Building building) {
        if (building == null || isClaimed()) {
            return false;
        }

        mClaimedBy = building.getReference(Building.class);
        mHandler.update(this);

        return true;
    }

    /** Remove any claim over the HexagonTile. */
    public void removeClaim() {
        if (mClaimedBy != null) {
            mHandler.update(this);
        }
        mClaimedBy = null;
    }

    /**
     * Get whether a valid claim has been made.
     *
     * @return Whether the tile is claimed by a building.
     */
    public boolean isClaimed() {
        return Reference.isValid(mClaimedBy);
    }

    /**
     * Get the {@link Player} who has claimed this tile (either because there is a building on it or
     * because it is adjacent to a building).
     *
     * @return The Player who has claimed this tile; otherwise {@code null}.
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
     * @return The owner ID of the Player as an {@link Integer}; otherwise {@code null}.
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
        mHandler.update(this);
        // If null is provided, set a reference to null.
        if (building == null) {
            mBuilding = null;
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
        if (!Reference.isValid(mBuilding)) {
            return null;
        }

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

    @Override
    public void serialize(DataOutputStream stream, int clientId) throws IOException {
        stream.writeFloat(mHeight);
        stream.writeByte(mTileType.getValue());

        Building b = getBuilding();
        final int id;

        if (b != null) {
            id = b.getNetworkObject().getId();
        } else {
            id = -1;
        }

        stream.writeInt(id);

        final int claimId;

        if (Reference.isValid(mClaimedBy)) {
            claimId = mClaimedBy.get().getNetworkObject().getId();
        } else {
            claimId = -1;
        }

        stream.writeInt(claimId);
    }

    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        mHeight = stream.readFloat();
        TileType newType = TileType.getTile(stream.readByte());

        if (newType != mTileType) {
            mTileType = newType;
            mGameObject.destroy();
            buildGameObject();
        } else {
            updateHeight();
        }

        NetworkManager manager = mHandler.getNetworkManager();

        int buildingId = stream.readInt();

        if (buildingId != -1) {
            // We need to schedule an event here, because the capital may have spawned (will have
            // spawned) after the data update message
            Engine.getInstance()
                    .scheduleEndOfLoopEvent(
                            () -> {
                                NetworkObject obj = manager.getObjectById(buildingId);

                                Reference<Building> b =
                                        obj == null
                                                ? null
                                                : obj.getGameObject().getComponent(Building.class);

                                if (!Reference.isValid(b)) {
                                    log.severe(
                                            "Deserialized a building, but did not find it locally!");
                                } else if (b != mBuilding) {
                                    setBuilding(b.get());
                                }
                            });
        }

        int claimId = stream.readInt();

        if (claimId != -1) {
            // Same here
            Engine.getInstance()
                    .scheduleEndOfLoopEvent(
                            () -> {
                                NetworkObject obj = manager.getObjectById(claimId);

                                Reference<Building> b =
                                        obj == null
                                                ? null
                                                : obj.getGameObject().getComponent(Building.class);

                                if (!Reference.isValid(b)) {
                                    log.severe(
                                            "Deserialized a claimant, but did not find it locally!");
                                } else if (b != mClaimedBy) {
                                    removeClaim();
                                    setClaimedBy(b.get());
                                }
                            });
        }
    }

    private void updateHeight() {
        if (mTileType == TileType.WATER) {
            mGameObject.getTransform(TransformHex.class).setHeight(WATER_THRESHOLD);

            mSecondaryController.get().setTargetHeight(mHeight - WATER_THRESHOLD - 0.1f);
        } else {
            mHeightController.get().setTargetHeight(mHeight);
        }
    }

    private void buildGameObject() {
        mSecondaryController = null;

        switch (mTileType) {
            case WATER:
                mGameObject =
                        GameObject.instantiate(
                                WATER_TILE, new TransformHex(mQ, mR, WATER_THRESHOLD));

                GameObject floor = mGameObject.findChildByName("Water Floor");

                mSecondaryController = floor.getComponent(HeightController.class);
                break;
            case MOUNTAIN:
                mGameObject =
                        GameObject.instantiate(
                                MOUNTAIN_TILE, new TransformHex(mQ, mR, WATER_THRESHOLD));
                break;
            default:
                mGameObject =
                        GameObject.instantiate(
                                LAND_TILE, new TransformHex(mQ, mR, WATER_THRESHOLD));
        }

        mHeightController = mGameObject.getComponent(HeightController.class);

        switch (mTileType) {
            case WATER:
                mSecondaryController.get().setTargetHeight(mHeight - WATER_THRESHOLD - 0.1f);
                mHeightController.get().setTargetHeight(WATER_THRESHOLD);
                break;
            default:
                mHeightController.get().setTargetHeight(mHeight);
                break;
        }

        Reference<HighlightControls> controls = mGameObject.getComponent(HighlightControls.class);
        if (controls == null) {
            mHighlightControls = new Reference<>(null);
        } else {
            mHighlightControls = controls;
        }

        mHandler.updateGameObject(this);
    }
}
