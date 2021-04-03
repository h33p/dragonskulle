/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.HashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
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
public class MapEffects extends Component implements IOnStart {

    /** Describes tile highlight option */
    public static enum StandardHighlightType {
        VALID(0),
        INVALID(1),
        PLAIN(2),
        ATTACK(3);

        @Accessors(prefix = "m")
        @Getter
        private final int mValue;

        StandardHighlightType(int type) {
            this.mValue = type;
        }

        public IRefCountedMaterial getMaterial() {
            switch (this) {
                case VALID:
                    return VALID_MATERIAL;
                case INVALID:
                    return INVALID_MATERIAL;
                case PLAIN:
                    return PLAIN_MATERIAL;
                case ATTACK:
                    return ATTACK_MATERIAL;
                default:
                    return null;
            }
        }

        public HighlightSelection asSelection() {
            return HighlightSelection.with(getMaterial());
        }
    }

    /** A class describing a sleection. Value of null means ignoring */
    public static class HighlightSelection {
        private boolean mClear;
        private IRefCountedMaterial mMaterial;

        public static final HighlightSelection CLEARED = cleared();

        private HighlightSelection() {}

        public static HighlightSelection ignored() {
            return null;
        }

        public static HighlightSelection with(IRefCountedMaterial material) {
            HighlightSelection ret = new HighlightSelection();
            ret.mMaterial = material;
            return ret;
        }

        private static HighlightSelection cleared() {
            HighlightSelection ret = new HighlightSelection();
            ret.mClear = true;
            return ret;
        }
    }

    /** A simple tile highlight selection interface */
    public static interface IHighlightSelector {
        public HighlightSelection handleTile(HexagonTile tile);
    }

    public static final IRefCountedMaterial VALID_MATERIAL =
            highlightMaterialFromColour(0f, 1f, 0.2f);
    public static final IRefCountedMaterial INVALID_MATERIAL =
            highlightMaterialFromColour(1f, 0.08f, 0f);
    public static final IRefCountedMaterial PLAIN_MATERIAL =
            highlightMaterialFromColour(0.7f, 0.94f, 0.98f);
    public static final IRefCountedMaterial ATTACK_MATERIAL =
            highlightMaterialFromColour(0.9f, 0.3f, 0.3f);

    private HashMap<HexagonTile, GameObject> mHighlightedTiles = new HashMap<>();
    private Reference<HexagonMap> mMapReference = null;

    public static IRefCountedMaterial highlightMaterialFromColour(float r, float g, float b) {
        return new UnlitMaterial(new SampledTexture("white.bmp"), new Vector4f(r, g, b, 0.5f));
    }

    public static HighlightSelection highlightSelectionFromColour(float r, float g, float b) {
        return HighlightSelection.with(highlightMaterialFromColour(r, g, b));
    }

    /**
     * Select a single tile, overriding previous selection
     *
     * @param tile tile to select
     * @param selection type of highlight to use
     */
    public void highlightTile(HexagonTile tile, HighlightSelection selection) {

        if (tile == null || selection == null) return;

        GameObject effectObject = mHighlightedTiles.remove(tile);
        if (effectObject != null) effectObject.destroy();
        if (selection.mClear) return;
        if (!ensureMapReference()) return;

        effectObject =
                new GameObject(
                        "selection effect",
                        new TransformHex(tile.getQ(), tile.getR()),
                        (handle) -> {
                            handle.getTransform(TransformHex.class).translate(0.03f);

                            IRefCountedMaterial mat = selection.mMaterial.incRefCount();

                            Renderable rend = new Renderable(Mesh.HEXAGON, mat);
                            handle.addComponent(rend);
                        });

        mHighlightedTiles.put(tile, effectObject);

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
    public void highlightTiles(IHighlightSelector selector) {
        mMapReference.get().getAllTiles().forEach(t -> highlightTile(t, selector.handleTile(t)));
    }

    /**
     * Deselect a tile
     *
     * <p>This method will remove an active selection from the tile.
     *
     * @param tile tile to deselect
     */
    public void unhighlightTile(HexagonTile tile) {
        highlightTile(tile, HighlightSelection.CLEARED);
    }

    /**
     * Deselect all tiles
     *
     * <p>This will clear any selection that currently takes place
     */
    public void unhighlightAllTiles() {
        for (GameObject go : mHighlightedTiles.values()) go.destroy();
        mHighlightedTiles.clear();
    }

    /**
     * Check whether tile is selected
     *
     * @param tile tile to check
     * @return {@code true} if the tile is currently selected, {@code false} otherwise.
     */
    public boolean isTileHighlighted(HexagonTile tile) {
        return mHighlightedTiles.get(tile) != null;
    }

    @Override
    public void onStart() {
        Scene.getActiveScene().registerSingleton(this);
        ensureMapReference();
        log.info(mMapReference.toString());
    }

    @Override
    protected void onDestroy() {
        unhighlightAllTiles();
    }

    private boolean ensureMapReference() {
        if (mMapReference != null) return true;
        mMapReference =
                Scene.getActiveScene()
                        .getSingleton(HexagonMap.class)
                        .getReference(HexagonMap.class);
        return mMapReference != null;
    }
}
