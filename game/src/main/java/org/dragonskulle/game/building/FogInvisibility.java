/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.renderer.components.Renderable;

/**
 * Make objects invisible in fog.
 *
 * @author Aurimas Blažulionis
 *     <p>This class is a temporary solution to true object dormancy, where object will become
 *     invisible/only get updated when seen by the player. It hides objects when they are on the fog
 *     tile type.
 */
@Log
public class FogInvisibility extends Component implements IOnStart, ILateFrameUpdate {

    /** Internal list of renderables to control the visibility for. */
    private final List<Reference<Renderable>> mRenderables = new ArrayList<>();
    /** Internal reference to the {@link StickToTile} component on the object. */
    private Reference<StickToTile> mStickToTile;

    @Override
    public void onStart() {
        getGameObject().getComponents(Renderable.class, mRenderables);
        getGameObject().getComponentsInChildren(Renderable.class, mRenderables);
        mStickToTile = getGameObject().getComponent(StickToTile.class);

        mRenderables.stream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(r -> r.setEnabled(false));
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {
        if (!Reference.isValid(mStickToTile)) {
            return;
        }

        HexagonTile tile = mStickToTile.get().getTile();

        boolean visible = tile != null && tile.getTileType() != TileType.FOG;

        mRenderables.stream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(r -> r.setEnabled(visible));

        // Remove self, because we will never become invisible afterwards
        if (visible) {
            getGameObject().removeComponent(this);
        }
    }

    @Override
    public void onDestroy() {}
}
