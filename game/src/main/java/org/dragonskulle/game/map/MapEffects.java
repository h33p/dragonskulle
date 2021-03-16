/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.HashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.*;
import org.joml.Vector4f;

/**
 * @author Aurimas Blažulionis
 *     <p>This component provides client sided effects for the map, such as selecting tiles, marking
 *     valid, and invalid tiles, and so on.
 */
@Accessors(prefix = "m")
@Log
public class MapEffects extends Component implements IOnStart, IFrameUpdate {

    public static final UnlitMaterial VALID_MATERIAL =
            new UnlitMaterial(new SampledTexture("white.bmp"), new Vector4f(0f, 1f, 0.2f, 0.5f));
    public static final UnlitMaterial INVALID_MATERIAL =
            new UnlitMaterial(new SampledTexture("white.bmp"), new Vector4f(1f, 0.08f, 0f, 0.5f));
    public static final UnlitMaterial PLAIN_MATERIAL =
            new UnlitMaterial(
                    new SampledTexture("white.bmp"), new Vector4f(0.7f, 0.94f, 0.98f, 0.5f));

    private HashMap<HexagonTile, GameObject> mSelectedTiles = new HashMap<>();
    private Reference<HexagonMap> mMapReference = null;

    private boolean mLastPressed = false;
    private int mClickCounter = 0;

    public static enum HighlightType {
        IGNORE(-2),
        NONE(-1),
        VALID(0),
        INVALID(1),
        PLAIN(2);

        @Accessors(prefix = "m")
        @Getter
        private final int mValue;

        HighlightType(int type) {
            this.mValue = type;
        }
    }

    public static interface IHighlightSelector {
        public HighlightType handleTile(HexagonTile tile);
    }

    /**
     * Select a single tile, overriding previous selection
     *
     * @param tile tile to select
     * @param highlightType type of highlight to use
     */
    public void selectTile(HexagonTile tile, HighlightType highlightType) {

        if (tile == null || highlightType == HighlightType.IGNORE) return;

        GameObject effectObject = mSelectedTiles.remove(tile);
        if (effectObject != null) effectObject.destroy();
        if (highlightType == HighlightType.NONE) return;

        effectObject =
                new GameObject(
                        "selection effect",
                        new TransformHex(tile.getQ(), tile.getR()),
                        (handle) -> {
                            handle.getTransform(TransformHex.class).translate(0.1f);

                            IMaterial mat = null;

                            switch (highlightType) {
                                case VALID:
                                    mat = VALID_MATERIAL.incRefCount();
                                    break;
                                case INVALID:
                                    mat = INVALID_MATERIAL.incRefCount();
                                    break;
                                case PLAIN:
                                    mat = PLAIN_MATERIAL.incRefCount();
                                    break;
                                default:
                                    break;
                            }

                            Renderable rend = new Renderable(Mesh.HEXAGON, mat);
                            handle.addComponent(rend);
                        });

        mSelectedTiles.put(tile, effectObject);

        mMapReference.get().getGameObject().addChild(effectObject);
    }

    /** Select multiple tiles by selector handler */
    public void selectTiles(IHighlightSelector selector) {
        mMapReference.get().getAllTiles().forEach(t -> selectTile(t, selector.handleTile(t)));
    }

    public void deselectTile(HexagonTile tile) {
        selectTile(tile, HighlightType.NONE);
    }

    public void deselectAllTiles() {
        for (GameObject go : mSelectedTiles.values()) go.destroy();
        mSelectedTiles.clear();
    }

    public boolean isTileSelected(HexagonTile tile) {
        return mSelectedTiles.get(tile) != null;
    }

    @Override
    public void onStart() {
        mMapReference =
                Engine.getInstance().getCurrentScene().getGameObjects().stream()
                        .map(go -> go.getComponent(HexagonMap.class))
                        .filter(r -> r != null)
                        .filter(Reference::isValid)
                        .findFirst()
                        .orElse(null);
        System.out.println(mMapReference);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        boolean pressed = GameActions.ACTION_1.isActivated();
        if (pressed && !mLastPressed) {
            mClickCounter++;

            switch (mClickCounter) {
                case 1:
                    selectTile(mMapReference.get().getTile(1, 1), HighlightType.PLAIN);
                    break;
                case 2:
                    selectTile(mMapReference.get().getTile(1, 1), HighlightType.INVALID);
                    break;
                case 3:
                    selectTile(mMapReference.get().getTile(1, 1), HighlightType.VALID);
                    break;
                default:
                    selectTiles(
                            (tile) -> {
                                if (tile.length() < mClickCounter)
                                    return HighlightType.values()[mClickCounter % 5];
                                return HighlightType.NONE;
                            });
            }
        }
        mLastPressed = pressed;
    }

    @Override
    protected void onDestroy() {
        deselectAllTiles();
    }
}
