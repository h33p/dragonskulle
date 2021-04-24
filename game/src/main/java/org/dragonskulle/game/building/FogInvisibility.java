/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.ArrayList;
import java.util.List;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.renderer.components.Renderable;

/**
 * Make buildings invisible in fog.
 *
 * @author Aurimas Blažulionis
 *     <p>This class is a temporary solution to true object dormancy, where object will become
 *     invisible/only get updated when seen by the player. It hides objects when they are on the fog
 *     tile type.
 */
public class FogInvisibility extends Component implements IOnAwake, ILateFrameUpdate {

    /** Internal list of renderables to control the visibility for. */
    private final List<Reference<Renderable>> mRenderables = new ArrayList<>();
    /** Internal reference to the {@link Building} component on the object. */
    private Reference<Building> mBuilding;

    @Override
    public void onAwake() {
        getGameObject().getComponents(Renderable.class, mRenderables);
        getGameObject().getComponentsInChildren(Renderable.class, mRenderables);
        mBuilding = getGameObject().getComponent(Building.class);
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {
        if (!Reference.isValid(mBuilding)) {
            return;
        }

        HexagonTile tile = mBuilding.get().getTile();

        boolean visible = tile != null && tile.getTileType() != TileType.FOG;

        mRenderables.stream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(r -> r.setEnabled(visible));
    }

    @Override
    public void onDestroy() {}
}
