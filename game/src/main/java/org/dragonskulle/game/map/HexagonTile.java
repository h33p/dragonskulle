/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.game.App;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTileStore.TileToStoreActions;
import org.dragonskulle.game.materials.HighlightControls;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * Describes information on individual hexagon map tile.
 *
 * @author Leela Muppala
 * @author Aurimas Blažulionis
 *     <p>Creates each HexagonTile with their 3 coordinates. This stores information about the axial
 *     coordinates of each tile.
 */
@Log
@Accessors(prefix = "m")
public class HexagonTile implements INetSerializable {
    static final Resource<GLTF> TEMPLATES = GLTF.getResource("templates");

    /** Describes a template for land hex tile. */
    static final GameObject LAND_TILE =
            App.TEMPLATES.get().getDefaultScene().findRootObject("Land Hex");

    /** Describes a template for land hex tile. */
    static final GameObject FOG_TILE =
            App.TEMPLATES.get().getDefaultScene().findRootObject("Fog Hex");

    /** Describes a template for water hex tile. */
    static final GameObject WATER_TILE =
            App.TEMPLATES.get().getDefaultScene().findRootObject("Water Hex");

    /** Describes a template for water hex tile. */
    static final GameObject MOUNTAIN_TILE =
            App.TEMPLATES.get().getDefaultScene().findRootObject("Mountains Hex");

    /** Describes the tile type on the map. */
    public static enum TileType {
        LAND((byte) 0),
        WATER((byte) 1),
        MOUNTAIN((byte) 2),
        FOG((byte) -1);

        @Getter private final byte mValue;

        private static final TileType[] VALUES = TileType.values();

        /**
         * Create a {@link TileType}.
         *
         * @param value value used to identify the tile.
         */
        private TileType(byte value) {
            mValue = value;
        }

        /**
         * Get the tile from byte value.
         *
         * @param value value to find.
         * @return {@link TileType} with the value.
         */
        public static TileType getTile(byte value) {
            for (TileType t : VALUES) {
                if (t.mValue == value) {
                    return t;
                }
            }
            return null;
        }
    }

    static final float WATER_THRESHOLD = -0.3f;
    static final float MOUNTAINS_THRESHOLD = 0.8f;
    static final float WATER_OFF = 0.1f;

    /** The Axial Coordinate in the Q Position. */
    @Getter private final int mQ;

    /** The Axial Coordinate in the R . */
    @Getter private final int mR;

    @Setter(AccessLevel.PACKAGE)
    @Getter
    private float mHeight;

    @Getter private TileType mTileType;

    private final TileToStoreActions mHandler;

    /** This states which land mass this tile is on. Set at -1 when not set */
    int mLandMassNumber = -1;

    /**
     * Associated game object.
     *
     * <p>This is specifically package-only, since the game does not need to know about underlying
     * tile objects.
     */
    @Getter(AccessLevel.PACKAGE)
    private GameObject mGameObject;

    /** Controls height and fading of the tile. */
    private Reference<FadeTile> mFadeControl;
    /** Controls height and fading of secondary tile surface. */
    private Reference<FadeTile> mSecondaryFade;

    /** Controls highlighting on the tile. */
    @Getter(AccessLevel.PACKAGE)
    private Reference<HighlightControls> mHighlightControls;

    /** Reference to the {@link Building} that is on the tile. */
    private Reference<Building> mBuilding = null;

    private int mNextBuilding = -1;

    /** A reference to the building that claims the tile, or {@code null}. */
    private Reference<Building> mClaimedBy = null;

    private int mNextClaimedBy = -1;

    /**
     * Constructor that creates the HexagonTile.
     *
     * @param q The first coordinate.
     * @param r The second coordinate.
     * @param height the height of the tile, this decides the {@link TileType}
     * @param handler handler passed by {@link HexagonTileStore} to be called on changes
     */
    HexagonTile(int q, int r, float height, TileToStoreActions handler) {
        this.mQ = q;
        this.mR = r;
        this.mHeight =
                handler.getNetworkManager().isClient() ? WATER_THRESHOLD + WATER_OFF : height;
        this.mHandler = handler;

        if (handler.getNetworkManager().isClient()) {
            mTileType = TileType.FOG;
        } else if (height <= WATER_THRESHOLD) {
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

    /**
     * Get tile distance to particular coordinate.
     *
     * @param q Q-axis value.
     * @param r R-axis value.
     * @return integer distance from the tile to the given coordinate.
     */
    public int distTo(int q, int r) {
        int s = -q - r;

        return (int) ((Math.abs(q - mQ) + Math.abs(r - mR) + Math.abs(s - getS())) / 2);
    }

    /**
     * Retrieve the third (cube) coordinate.
     *
     * <p>This coordinate will always be equal to -getQ() -getR()
     *
     * @return The S value.
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

        building.onClaimTile(this);
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
     * Get the {@link Building} who has claimed this tile.
     *
     * @return The Building who has claimed this tile; otherwise {@code null}.
     */
    public Building getClaimedBy() {
        if (!isClaimed()) {
            return null;
        }

        // This should never occur on the server.
        //
        // However, on clients this is a perfectly valid occurance
        if (!Reference.isValid(mClaimedBy)) {
            return null;
        }

        return mClaimedBy.get();
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

    /**
     * Determines if for a given player if this tile is buildable upon.
     *
     * @param player the player to build
     * @return true if buildable, false otherwise
     */
    public boolean isBuildable(Player player) {

        if (this.getTileType() != TileType.LAND) {
            return false;
        }

        if (player == null) {
            log.fine("player was null so false");
            return false;
        }

        HexagonMap map = player.getMap();
        if (map == null) {
            log.fine("Map is null.");
            return false;
        }

        if (isClaimed()) {
            log.fine("Tile already claimed.");
            return false;
        }

        if (hasBuilding()) {
            log.fine("Building already on tile.");
            return false;
        }

        // Ensure that the tile is in the buildable range of at least one owned building.
        boolean buildable = false;
        for (Reference<Building> buildingReference : player.getOwnedBuildings()) {
            if (Reference.isValid(buildingReference)
                    && buildingReference.get().getBuildableTiles().contains(this)) {
                buildable = true;
                break;
            }
        }

        if (!buildable) {
            log.fine("Building not in buildable range/on suitable tile.");
            return false;
        }

        return true;
    }

    @Override
    public void serialize(DataOutput stream, int clientId) throws IOException {
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
    public void deserialize(DataInput stream) throws IOException {
        float height = stream.readFloat();
        TileType newType = TileType.getTile(stream.readByte());

        if (newType != mTileType) {
            mHeight = -1f;
            updateHeight(false, true);
            mHeight = height;
            mTileType = newType;
            buildGameObject();
        } else {
            mHeight = height;
            updateHeight(true, true);
        }

        NetworkManager manager = mHandler.getNetworkManager();

        mNextBuilding = stream.readInt();

        if (mNextBuilding >= 0) {
            onBuildingChange(manager);
        } else if (Reference.isValid(mBuilding)) {
            setBuilding(null);
        }

        mNextClaimedBy = stream.readInt();

        if (mNextClaimedBy >= 0) {
            onClaimChange(manager);
        } else if (Reference.isValid(mClaimedBy)) {
            removeClaim();
        }
    }

    /**
     * Invoked on the client whenever there is a building change on the tile.
     *
     * @param manager network manager to use
     */
    private void onBuildingChange(NetworkManager manager) {

        if (mNextBuilding < 0) {
            return;
        }

        NetworkObject obj = manager.getObjectById(mNextBuilding);

        Reference<Building> b =
                obj == null ? null : obj.getGameObject().getComponent(Building.class);

        if (!Reference.isValid(b)) {
            // This could occur when spawn event happens after this update.
            Engine.getInstance().scheduleEndOfLoopEvent(() -> onBuildingChange(manager));
            log.fine("Deserialized a building, but did not find it locally!");
        } else if (b != mBuilding) {
            setBuilding(b.get());
            mNextBuilding = -1;
        }
    }

    /**
     * Invoked on the client whenever there is a claimant change on the tile.
     *
     * @param manager network manager to use
     */
    private void onClaimChange(NetworkManager manager) {

        if (mNextClaimedBy < 0) {
            return;
        }

        NetworkObject obj = manager.getObjectById(mNextClaimedBy);

        Reference<Building> b =
                obj == null ? null : obj.getGameObject().getComponent(Building.class);

        if (!Reference.isValid(b)) {
            // This could occur when spawn event happens after this update.
            Engine.getInstance().scheduleEndOfLoopEvent(() -> onClaimChange(manager));
            log.fine("Deserialized a claimant, but did not find it locally!");
        } else if (b != mClaimedBy) {
            removeClaim();
            setClaimedBy(b.get());
            mNextClaimedBy = -1;
        }
    }

    /**
     * Get the surface height of the tile.
     *
     * @return surface height of the tile. It might be different from actual height, it is the
     *     current visual height.
     */
    public float getSurfaceHeight() {
        return mGameObject.getTransform(TransformHex.class).getHeight();
    }

    /**
     * Update the height of the tile.
     *
     * @param fadeIn whether the tile should fade in, or fade out
     * @param destroyOnFadeOut set to automatically destroy the object if it fades out
     */
    void updateHeight(boolean fadeIn, boolean destroyOnFadeOut) {

        mFadeControl.get().setDestroyOnFadeOut(destroyOnFadeOut);

        if (Reference.isValid(mSecondaryFade)) {
            mSecondaryFade.get().setDestroyOnFadeOut(destroyOnFadeOut);
        }

        if (mTileType == TileType.WATER) {
            mGameObject.getTransform(TransformHex.class).setHeight(WATER_THRESHOLD);

            mSecondaryFade.get().setState(fadeIn, mHeight - WATER_THRESHOLD - WATER_OFF);
        } else {
            mFadeControl.get().setState(fadeIn, mHeight);
        }
    }

    /** Builds the tile object, and notifies {@link HexagonMap} of this change. */
    private void buildGameObject() {
        mSecondaryFade = null;

        switch (mTileType) {
            case WATER:
                mGameObject =
                        GameObject.instantiate(
                                WATER_TILE, new TransformHex(mQ, mR, WATER_THRESHOLD));

                GameObject floor = mGameObject.findChildByName("Water Floor");

                mSecondaryFade = floor.getComponent(FadeTile.class);
                break;
            case MOUNTAIN:
                mGameObject =
                        GameObject.instantiate(
                                MOUNTAIN_TILE, new TransformHex(mQ, mR, WATER_THRESHOLD));
                break;
            case FOG:
                mGameObject =
                        GameObject.instantiate(FOG_TILE, new TransformHex(mQ, mR, WATER_THRESHOLD));
                break;
            default:
                mGameObject =
                        GameObject.instantiate(
                                LAND_TILE, new TransformHex(mQ, mR, WATER_THRESHOLD));
        }

        mFadeControl = mGameObject.getComponent(FadeTile.class);

        switch (mTileType) {
            case WATER:
                mSecondaryFade.get().setState(true, mHeight - WATER_THRESHOLD - 0.1f);
                mFadeControl.get().setState(true, WATER_THRESHOLD);
                mFadeControl.get().setFadeValue(0f);
                break;
            case FOG:
                mFadeControl.get().setState(true, mHeight);
                break;
            default:
                mFadeControl.get().setState(true, mHeight);
                mFadeControl.get().setFadeValue(0f);
                break;
        }

        mHighlightControls = mGameObject.getComponent(HighlightControls.class);

        mHandler.updateGameObject(this);
    }
}
