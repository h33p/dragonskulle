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
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.*;
import org.dragonskulle.ui.UIManager;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * @author Aurimas Blažulionis
 *     <p>This component provides client sided effects for the map, such as selecting tiles, marking
 *     valid, and invalid tiles, and so on.
 */
@Accessors(prefix = "m")
@Log
public class MapEffects extends Component implements IOnStart, IFrameUpdate {

    /** Describes tile selection option */
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

    /** A simple tile highlight selection interface */
    public static interface IHighlightSelector {
        public HighlightType handleTile(HexagonTile tile);
    }

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
    int mRange = 0;

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
                            handle.getTransform(TransformHex.class).translate(0.03f);

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

    /**
     * Select multiple tiles by selector handler
     *
     * <p>This will iterate through all tiles on the map, and call the selector handler to see if
     * any selection should take place
     *
     * @param selector selector that handles tile selection
     */
    public void selectTiles(IHighlightSelector selector) {
        mMapReference.get().getAllTiles().forEach(t -> selectTile(t, selector.handleTile(t)));
    }

    /**
     * Deselect a tile
     *
     * <p>This method will remove an active selection from the tile.
     *
     * @param tile tile to deselect
     */
    public void deselectTile(HexagonTile tile) {
        selectTile(tile, HighlightType.NONE);
    }

    /**
     * Deselect all tiles
     *
     * <p>This will clear any selection that currently takes place
     */
    public void deselectAllTiles() {
        for (GameObject go : mSelectedTiles.values()) go.destroy();
        mSelectedTiles.clear();
    }

    /**
     * Check whether tile is selected
     *
     * @param tile tile to check
     * @return {@code true} if the tile is currently selected, {@code false} otherwise.
     */
    public boolean isTileSelected(HexagonTile tile) {
        return mSelectedTiles.get(tile) != null;
    }

    @Override
    public void onStart() {
        mMapReference =
                Scene.getActiveScene()
                        .getSingleton(HexagonMap.class)
                        .getReference(HexagonMap.class);
        System.out.println(mMapReference);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        boolean pressed = GameActions.ACTION_1.isActivated();

        Camera mainCam = Scene.getActiveScene().getSingleton(Camera.class);

        if (pressed && mainCam != null) {
            // Retrieve scaled screen coordinates
            Vector2fc screenPos = UIManager.getInstance().getScaledCursorCoords();

            // Convert those coordinates to local coordinates within the map
            Vector3f pos =
                    mainCam.screenToPlane(
                            mMapReference.get().getGameObject().getTransform(),
                            screenPos.x(),
                            screenPos.y(),
                            new Vector3f());

            // Convert those coordinates to axial
            TransformHex.cartesianToAxial(pos);
            // And round them
            TransformHex.roundAxial(pos);

            // And then select the tile
            selectTile(mMapReference.get().getTile((int) pos.x, (int) pos.y), HighlightType.PLAIN);
        }

        if (pressed && !mLastPressed) {
            mClickCounter++;

            if (mClickCounter % 50 > 25) mRange--;
            else mRange++;

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
                    // Apply selection to all tiles that are within mRange from origin
                    selectTiles(
                            (tile) -> {
                                if (tile.length() < mRange)
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
