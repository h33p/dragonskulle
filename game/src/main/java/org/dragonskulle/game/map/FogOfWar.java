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
import org.dragonskulle.core.Time;
import org.dragonskulle.game.player.Player;

/**
 * @author Aurimas Blažulionis
 *     <p>This component draws a visual fog of war for the players
 */
@Accessors(prefix = "m")
@Log
public class FogOfWar extends Component implements IOnStart, ILateFrameUpdate {

    private HashMap<HexagonTile, Reference<GameObject>> mFogTiles = new HashMap<>();
    private Reference<HexagonMap> mMapReference = null;
    @Getter @Setter private Reference<Player> mActivePlayer;

    static final Resource<GLTF> TEMPLATES = GLTF.getResource("templates");

    private static final GameObject FOG_OBJECT =
            TEMPLATES.get().getDefaultScene().findRootObject("Cloud Hex");

    @Override
    public void onStart() {
        Scene.getActiveScene().registerSingleton(this);
        ensureMapReference();
        log.info(mMapReference.toString());
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

        double time = Time.getPreciseTimeInSeconds();

        mMapReference
                .get()
                .getAllTiles()
                .forEach(
                        tile -> {
                            // TODO: do this better, in O(1)
                            Boolean contains =
                                    activePlayer
                                            .getOwnedBuildingsAsStream()
                                            .filter(Reference::isValid)
                                            .map(Reference::get)
                                            .map(b -> (Boolean) b.getViewableTiles().contains(tile))
                                            .filter(b -> b == true)
                                            .findFirst()
                                            .orElse(null);
                            setFog(tile, contains == null);
                        });
    }

    @Override
    protected void onDestroy() {
        for (Reference<GameObject> go : mFogTiles.values()) {
            if (go.isValid()) {
                go.get().destroy();
            }
        }

        mFogTiles.clear();
    }

    private boolean ensureMapReference() {
        if (mMapReference != null) {
            return true;
        }
        mMapReference =
                Scene.getActiveScene()
                        .getSingleton(HexagonMap.class)
                        .getReference(HexagonMap.class);
        return mMapReference != null;
    }

    private void setFog(HexagonTile tile, boolean enable) {
        if (!enable) {
            Reference<GameObject> go = mFogTiles.remove(tile);
            if (go != null && go.isValid()) {
                go.get().destroy();
            }
            return;
        }

        if (mFogTiles.containsKey(tile)) {
            return;
        }

        GameObject go =
                GameObject.instantiate(
                        FOG_OBJECT, new TransformHex(tile.getQ(), tile.getR(), tile.getHeight()));

        mMapReference.get().getGameObject().addChild(go);

        mFogTiles.put(tile, go.getReference());
    }
}
