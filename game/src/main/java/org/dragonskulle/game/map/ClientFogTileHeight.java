/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.ClientNetworkManager;
import org.dragonskulle.network.components.NetworkManager;
import org.joml.Vector3f;

/**
 * Add depth to the fog tiles.
 *
 * @author Aurimas Blažulionis
 *     <p>This component adds a sense of depth to the fog tiles when they are further away from the
 *     player
 */
@Accessors(prefix = "m")
public class ClientFogTileHeight extends Component implements IOnStart, ILateFrameUpdate {

    private HexagonTile mTile;
    private TransformHex mTransform;
    private Reference<NetworkManager> mNetworkManager;
    private int mNetId = -1;
    private Reference<Player> mPlayer;
    private Reference<FadeTile> mFadeTile;
    private float mMaxVisibility = Float.NEGATIVE_INFINITY;

    @Override
    public void onStart() {
        mTransform = getGameObject().getTransform(TransformHex.class);

        if (mTransform == null) {
            return;
        }

        HexagonMap map = Scene.getActiveScene().getSingleton(HexagonMap.class);

        if (map == null) {
            return;
        }

        Vector3f pos = mTransform.getLocalPosition(new Vector3f());

        mTile = map.getTile((int) pos.x, (int) pos.y);

        mNetworkManager = Scene.getActiveScene().getSingletonRef(NetworkManager.class);

        if (!Reference.isValid(mNetworkManager)) {
            return;
        }

        ClientNetworkManager mClientManager = mNetworkManager.get().getClientManager();

        if (mClientManager == null) {
            return;
        }

        mNetId = mClientManager.getNetId();

        mPlayer = mNetworkManager.get().getIdSingletons(mNetId).getRef(Player.class);

        mFadeTile = getGameObject().getComponent(FadeTile.class);
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {
        if (mTile == null || mTile.getTileType() != TileType.FOG) {
            return;
        }

        if (mNetId == -1 || !Reference.isValid(mNetworkManager)) {
            return;
        }

        if (!Reference.isValid(mPlayer)) {
            mPlayer = mNetworkManager.get().getIdSingletons(mNetId).getRef(Player.class);
            if (!Reference.isValid(mPlayer)) {
                return;
            }
        }

        if (!Reference.isValid(mFadeTile)) {
            return;
        }

        float val = Math.min(mPlayer.get().getTileViewability(mTile), 0f);

        mMaxVisibility = Math.max(val, mMaxVisibility);

        val = 5 * (float) Math.pow(1.4f, mMaxVisibility) - 5;

        mFadeTile.get().setState(true, val + HexagonTile.WATER_THRESHOLD + HexagonTile.WATER_OFF);
    }

    @Override
    protected void onDestroy() {}
}
