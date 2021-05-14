/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.java.Log;
import org.dragonskulle.game.NoiseUtil;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.NetworkMessage;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.ServerNetworkManager;
import org.dragonskulle.network.components.sync.ISyncVar;
import org.dragonskulle.utils.IOUtils;
import org.dragonskulle.utils.MathUtils;

/**
 * Stores and synchronizes the hexagon map.
 *
 * @author Leela Muppala
 * @author Aurimas Blažulionis
 *     <p>This class generates and stores a map of tiles with appropriate coordinates. Hexagon map
 *     objects are also created and stored.
 */
@Log
class HexagonTileStore implements ISyncVar {
    private HexagonTile[][] mTiles;
    private final int mCoordShift;
    private final int mSeed;
    private final Map<Integer, boolean[][]> mViewedTileMask;
    private final Map<Integer, boolean[][]> mTileMask;
    private final Map<Integer, boolean[]> mTileRowMask;
    private final Map<Integer, boolean[]> mDirty;
    private final HexagonMap mMap;
    private final TileToStoreActions mHandler = new TileToStoreActions();

    /** Actions a {@link HexagonTile} can invoke on the tile store. */
    class TileToStoreActions {
        /**
         * Invoked whenever a tile is updated.
         *
         * @param tile which tile updated.
         */
        void update(HexagonTile tile) {
            int q = tile.getQ() + mCoordShift;
            int r = tile.getR() + mCoordShift;

            ServerNetworkManager serverManager = mMap.getNetworkManager().getServerManager();

            if (serverManager == null) return;

            Collection<ServerClient> clients = serverManager.getClients();

            if (clients == null) return;

            for (ServerClient c : clients) {

                if (!c.isInGame()) {
                    continue;
                }

                Integer id = c.getNetworkID();
                Player p = serverManager.getIdSingletons(id).get(Player.class);

                // Spectators (non-players) can view all tiles
                if (p != null && !p.gameEnd() && !p.isTileViewable(mTiles[q][r])) continue;

                getTileMask(id)[q][r] = true;
                getTileRowMask(id)[q] = true;
                getDirty(id)[0] = true;
            }
        }

        /**
         * Get the network manager.
         *
         * @return network manager that owns the map.
         */
        NetworkManager getNetworkManager() {
            return mMap.getNetworkManager();
        }

        /**
         * Update (change) tile game object.
         *
         * @param tile which tile's object changed.
         */
        void updateGameObject(HexagonTile tile) {
            mMap.updateTileGameObject(tile);
        }
    }

    @Override
    public boolean isDirty(int clientId) {

        Integer id = clientId;

        dirtyViewableTiles(id);

        boolean[] dirty = mDirty.get(id);

        if (dirty == null) {
            dirty = new boolean[1];
            mDirty.put(id, dirty);
        }

        return dirty[0];
    }

    /**
     * Get the viewed tiles mask for particular player.
     *
     * @param id integer ID for the player in question.
     * @return mask for the player.
     */
    boolean[][] getViewedTileMask(Integer id) {
        boolean[][] viewedTileMask = mViewedTileMask.get(id);

        if (viewedTileMask == null) {
            viewedTileMask = new boolean[mTiles.length][mTiles.length];
            mViewedTileMask.put(id, viewedTileMask);
        }

        return viewedTileMask;
    }

    /**
     * Get the tile dirty mask for particular player.
     *
     * @param id integer ID for the player in question.
     * @return mask for the player.
     */
    private boolean[][] getTileMask(Integer id) {
        boolean[][] tileMask = mTileMask.get(id);

        if (tileMask == null) {
            tileMask = new boolean[mTiles.length][mTiles.length];
            mTileMask.put(id, tileMask);
        }

        return tileMask;
    }

    /**
     * Get the updated rows mask for particular player.
     *
     * @param id integer ID for the player in question.
     * @return mask for the player.
     */
    private boolean[] getTileRowMask(Integer id) {
        boolean[] tileRowMask = mTileRowMask.get(id);

        if (tileRowMask == null) {
            tileRowMask = new boolean[mTiles.length];
            mTileRowMask.put(id, tileRowMask);
        }

        return tileRowMask;
    }

    /**
     * Get the dirty mask for particular player.
     *
     * @param id integer ID for the player in question.
     * @return mask for the player. Size of 1.
     */
    private boolean[] getDirty(Integer id) {
        boolean[] dirty = mDirty.get(id);

        if (dirty == null) {
            dirty = new boolean[1];
            mDirty.put(id, dirty);
        }

        return dirty;
    }

    /**
     * Dirty viewable tiles for particular player.
     *
     * @param id integer ID for the player in question.
     */
    private void dirtyViewableTiles(Integer id) {
        Player p = mMap.getNetworkManager().getIdSingletons(id).get(Player.class);

        boolean[][] viewedTileMask = getViewedTileMask(id);
        boolean[][] tileMask = getTileMask(id);
        boolean[] tileRowMask = getTileRowMask(id);
        boolean[] dirty = getDirty(id);

        if (p != null && p.hasLost()) p = null;
        if (p == null || p.gameEnd()) p = null;

        for (int q = 0; q < viewedTileMask.length; q++) {
            for (int r = 0; r < viewedTileMask.length; r++) {
                HexagonTile tile = mTiles[q][r];
                boolean viewable = p == null || p.isTileViewable(tile);
                if (!viewedTileMask[q][r] && viewable) {
                    viewedTileMask[q][r] = true;
                    tileMask[q][r] = true;
                    tileRowMask[q] = true;
                    dirty[0] = true;
                } else if (viewedTileMask[q][r] && !viewable) {
                    viewedTileMask[q][r] = false;
                }
            }
        }
    }

    @Override
    public void resetDirtyFlag(int clientId) {
        Integer id = clientId;

        boolean[][] tileMask = mTileMask.get(id);

        if (tileMask == null) {
            tileMask = new boolean[mTiles.length][mTiles.length];
            mTileMask.put(id, tileMask);
        }

        for (boolean[] mask : tileMask) {
            Arrays.fill(mask, false);
        }

        boolean[] tileRowMask = mTileRowMask.get(id);

        if (tileRowMask == null) {
            tileRowMask = new boolean[mTiles.length];
            mTileRowMask.put(id, tileRowMask);
        }

        Arrays.fill(tileRowMask, false);

        boolean[] dirty = mDirty.get(id);

        if (dirty == null) {
            dirty = new boolean[1];
            mDirty.put(clientId, dirty);
        }

        dirty[0] = false;
    }

    @Override
    public void deserialize(DataInput stream) throws IOException {

        final int maskSize = NetworkMessage.maskSizeInBytes(mTiles.length);
        boolean[] tileRowMask =
                NetworkMessage.getMaskFromBytes(IOUtils.readNBytes(stream, maskSize));

        for (int q = 0; q < tileRowMask.length; q++) {
            if (!tileRowMask[q]) continue;

            boolean[] mask = NetworkMessage.getMaskFromBytes(IOUtils.readNBytes(stream, maskSize));

            for (int r = 0; r < mask.length; r++) {
                if (!mask[r]) continue;

                HexagonTile tile = mTiles[q][r];

                if (tile == null) continue;

                tile.deserialize(stream);
            }
        }
    }

    @Override
    public void serialize(DataOutput stream, int clientId) throws IOException {
        Integer id = clientId;

        boolean[][] tileMask = mTileMask.get(id);

        if (tileMask == null) {
            tileMask = new boolean[mTiles.length][mTiles.length];
            mTileMask.put(id, tileMask);
        }

        boolean[] tileRowMask = mTileRowMask.get(id);

        if (tileRowMask == null) {
            tileRowMask = new boolean[mTiles.length];
            mTileRowMask.put(id, tileRowMask);
        }

        stream.write(NetworkMessage.convertBoolArrayToBytes(tileRowMask));

        for (int q = 0; q < tileRowMask.length; q++) {
            if (!tileRowMask[q]) continue;

            boolean[] mask = tileMask[q];

            stream.write(NetworkMessage.convertBoolArrayToBytes(mask));

            for (int r = 0; r < mask.length; r++) {
                if (!mask[r]) continue;

                HexagonTile tile = mTiles[q][r];

                if (tile == null) continue;

                tile.serialize(stream, clientId);
            }
        }
    }

    /**
     * Create a {@link HexagonTileStore}.
     *
     * <p>Hex(q,r) is stored as array[r+shift][q+shift] Map is created and stored in HexMap.
     *
     * @param size size of the map.
     * @param seed random seed of the map.
     * @param map back reference tothe map creating this.
     */
    public HexagonTileStore(int size, int seed, HexagonMap map) {
        mTiles = new HexagonTile[size][size];
        mViewedTileMask = new HashMap<>();
        mTileMask = new HashMap<>();
        mTileRowMask = new HashMap<>();
        mDirty = new HashMap<>();
        mSeed = seed;
        mCoordShift = size / 2;
        mMap = map;

        int max_empty = getSpaces(size); // The max number of empty spaces in one row of the array
        int loop = size / 2;
        int empty = max_empty;

        /* Generates the first part of the map */
        for (int r = 0; r < loop; r++) {

            int inside_empty = empty;

            for (int q = 0; q < size; q++) {
                if (inside_empty > 0) {
                    // No tile in this location
                    inside_empty--;
                } else {
                    int q1 = q - mCoordShift;
                    int r1 = r - mCoordShift;
                    float height = getHeight(q1, r1);
                    setTile(new HexagonTile(q1, r1, height, mHandler));
                }
            }
            empty--;
        }

        /* Generates the middle part of the map */
        int rM = (size / 2);
        for (int q = 0; q < size; q++) {
            int q1 = q - mCoordShift;
            int r1 = rM - mCoordShift;
            float height = getHeight(q1, r1);
            setTile(new HexagonTile(q1, r1, height, mHandler));
        }

        /* Generates the last part of the map */
        loop = (size / 2) + 1;
        int current_val = size;
        for (int r = loop; r < size; r++) {
            current_val--; // The number of cells with actual coordinates, it decreases with every
            // row
            int inside_val = 1;
            for (int q = 0; q < size; q++) {
                if (inside_val <= current_val) {
                    int q1 = q - mCoordShift;
                    int r1 = r - mCoordShift;
                    float height = getHeight(q1, r1);
                    setTile(new HexagonTile(q1, r1, height, mHandler));
                    inside_val++;
                } // otherwise we do not need a tile
            }
        }
    }

    /**
     * Get a hexagon tile.
     *
     * @param q q coordinate of the tile
     * @param r r coordinate of the tile
     * @return The {@link HexagonTile} which is at the coordinates (q,r) or {@code null} if nothing
     *     exists there
     */
    public HexagonTile getTile(int q, int r) {
        q += mCoordShift;
        r += mCoordShift;

        if (q < 0 || r < 0 || q >= mTiles.length || r >= mTiles.length) {
            return null;
        }

        return mTiles[q][r];
    }

    /**
     * Get a stream of all hexagon tiles.
     *
     * @return stream of all non-null hexagon tiles in the map
     */
    public Stream<HexagonTile> getAllTiles() {
        return Arrays.stream(mTiles).flatMap(Arrays::stream).filter(x -> x != null);
    }

    /**
     * Set a tile in the internal list.
     *
     * @param tile tile to set.
     */
    private void setTile(HexagonTile tile) {
        int q = tile.getQ() + mCoordShift;
        int r = tile.getR() + mCoordShift;
        mTiles[q][r] = tile;
    }

    /**
     * Provides a number that shows the number of nulls to add to a HexagonMap. Can only input odd
     * numbered size due to hexagon shape.
     *
     * @param input - The size of the hexagon map.
     * @return Returns the number of spaces to add in the 2d array to generate a hexagon shape.
     */
    private static int getSpaces(int input) {
        if (input % 2 == 0) {
            log.warning("The size is not an odd number");
        }
        return (input - 1) / 2;
    }

    /** Rounding factor used to get a stepped look. */
    private static final float NOISE_STEP = 0.2f;

    /** Noise octaves used for map generation. */
    private static final float[][] OCTAVES = {
        {0.1f, 0.9f, 0f},
        {0.3f, 0.2f, 0f},
        {0.6f, 0.1f, 0f}
    };

    /**
     * Get the height at a particular coordinate.
     *
     * @param q Q coordinate of the tile.
     * @param r R coordinate of the tile.
     * @return height at the point.
     */
    private float getHeight(int q, int r) {
        return MathUtils.roundStep(NoiseUtil.getHeight(q, r, mSeed, OCTAVES), NOISE_STEP);
    }
}
