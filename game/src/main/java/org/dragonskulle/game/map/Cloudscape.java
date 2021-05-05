/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.App;
import org.dragonskulle.game.NoiseUtil;
import org.dragonskulle.game.player.Player;

/**
 * @author Aurimas Blažulionis
 *     <p>This component draws a visual clouds for the players in undiscovered map areas.
 */
@Accessors(prefix = "m")
public class Cloudscape extends Component implements IOnStart, ILateFrameUpdate {

    private HashMap<HexagonTile, Reference<FadeTile>> mCloudTiles = new HashMap<>();
    private Reference<HexagonMap> mMapReference = null;
    @Getter @Setter private Reference<Player> mActivePlayer;

    private static final GameObject FOG_OBJECT =
            App.TEMPLATES.get().getDefaultScene().findRootObject("Cloud Hex");

    private static final float[][] OCTAVES = {
        {0.07f, 1f, 0.5f},
        {0.15f, 0.4f, 0.4f},
        {0.3f, 0.6f, 0f},
        {0.6f, 0.4f, 0f}
    };

    @Override
    public void onStart() {
        Scene.getActiveScene().registerSingleton(this);
        ensureMapReference();
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {

        if (!ensureMapReference()) {
            return;
        }

        Player activePlayer = mActivePlayer != null ? mActivePlayer.get() : null;

        if (activePlayer == null) {
            return;
        }

        mMapReference
                .get()
                .getAllTiles()
                .forEach(
                        tile ->
                                setClouds(
                                        tile,
                                        !activePlayer.gameEnd()
                                                && activePlayer.getTileViewability(tile) < -1
                                                && perlinCheck(tile)));
    }

    @Override
    protected void onDestroy() {
        for (Reference<FadeTile> cloudTile : mCloudTiles.values()) {
            if (Reference.isValid(cloudTile)) {
                cloudTile.get().getGameObject().destroy();
            }
        }

        mCloudTiles.clear();
    }

    private boolean ensureMapReference() {
        if (Reference.isValid(mMapReference)) {
            return true;
        }
        mMapReference = Scene.getActiveScene().getSingletonRef(HexagonMap.class);
        return Reference.isValid(mMapReference);
    }

    private boolean perlinCheck(HexagonTile tile) {
        return NoiseUtil.getHeight(tile.getQ(), tile.getR(), 0, OCTAVES) > 0.8f;
    }

    private void setClouds(HexagonTile tile, boolean enable) {
        Reference<FadeTile> tileRef = mCloudTiles.get(tile);

        if (Reference.isValid(tileRef)) {
            tileRef.get().setState(enable, tile.getHeight());
            return;
        } else if (!enable) {
            return;
        }

        GameObject go =
                GameObject.instantiate(
                        FOG_OBJECT, new TransformHex(tile.getQ(), tile.getR(), tile.getHeight()));

        tileRef = go.getComponent(FadeTile.class);

        if (tileRef == null) {
            FadeTile cloudTile = new FadeTile();
            go.addComponent(cloudTile);
            tileRef = cloudTile.getReference(FadeTile.class);
        }

        mMapReference.get().getGameObject().addChild(go);

        mCloudTiles.put(tile, tileRef);
    }
}
