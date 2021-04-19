/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.player.Player;

/**
 * @author Aurimas Blažulionis
 *     <p>This component draws a visual fog of war for the players
 */
@Accessors(prefix = "m")
@Log
public class FogOfWar extends Component implements IOnStart, ILateFrameUpdate {

    private HashMap<HexagonTile, Reference<FogTile>> mFogTiles = new HashMap<>();
    private Reference<HexagonMap> mMapReference = null;
    @Getter @Setter private Reference<Player> mActivePlayer;

    static final Resource<GLTF> TEMPLATES = GLTF.getResource("templates");

    private static final GameObject FOG_OBJECT =
            TEMPLATES.get().getDefaultScene().findRootObject("Cloud Hex");

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
                .forEach(tile -> setFog(tile, !activePlayer.isTileViewable(tile)));
    }

    @Override
    protected void onDestroy() {
        for (Reference<FogTile> fogTile : mFogTiles.values()) {
            if (Reference.isValid(fogTile)) {
                fogTile.get().getGameObject().destroy();
            }
        }

        mFogTiles.clear();
    }

    private boolean ensureMapReference() {
        if (Reference.isValid(mMapReference)) {
            return true;
        }
        mMapReference = Scene.getActiveScene().getSingletonRef(HexagonMap.class);
        return Reference.isValid(mMapReference);
    }

    private void setFog(HexagonTile tile, boolean enable) {
        Reference<FogTile> tileRef = mFogTiles.get(tile);

        if (Reference.isValid(tileRef)) {
            tileRef.get().setFog(enable);
            return;
        } else if (!enable) {
            return;
        }

        GameObject go =
                GameObject.instantiate(
                        FOG_OBJECT, new TransformHex(tile.getQ(), tile.getR(), tile.getHeight()));

        tileRef = go.getComponent(FogTile.class);

        if (tileRef == null) {
            FogTile fogTile = new FogTile();
            go.addComponent(fogTile);
            tileRef = fogTile.getReference(FogTile.class);
        }

        mMapReference.get().getGameObject().addChild(go);

        mFogTiles.put(tile, tileRef);
    }
}
