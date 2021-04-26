/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.materials.HighlightControls;
import org.dragonskulle.game.player.Player;
import org.joml.Vector4f;

/**
 * @author Aurimas Blažulionis
 *     <p>This component provides client sided effects for the map, such as selecting tiles, marking
 *     valid, and invalid tiles, and so on.
 */
@Accessors(prefix = "m")
public class MapEffects extends Component implements IOnStart, ILateFrameUpdate {

    /** Describes tile highlight option. */
    public static enum StandardHighlightType {
        VALID(0),
        INVALID(1),
        PLAIN(2),
        ATTACK(3),
        PLACE(4);

        @Accessors(prefix = "m")
        @Getter
        private final int mValue;

        StandardHighlightType(int type) {
            this.mValue = type;
        }

        public HighlightSelection asSelection() {
            switch (this) {
                case VALID:
                    return VALID_MATERIAL;
                case INVALID:
                    return INVALID_MATERIAL;
                case PLAIN:
                    return PLAIN_MATERIAL;
                case ATTACK:
                    return ATTACK_MATERIAL;
                case PLACE:
                    return PLACE_MATERIAL;
                default:
                    return null;
            }
        }
    }

    /** A class describing a sleection. Value of null means ignoring */
    public static class HighlightSelection {
        private boolean mClear;
        private Vector4f mOverlay = new Vector4f();

        public static final HighlightSelection CLEARED = cleared();

        private HighlightSelection() {}

        public static HighlightSelection ignored() {
            return null;
        }

        public static HighlightSelection with(Vector4f overlay) {
            HighlightSelection ret = new HighlightSelection();
            ret.mOverlay.set(overlay);
            return ret;
        }

        private static HighlightSelection cleared() {
            HighlightSelection ret = new HighlightSelection();
            ret.mClear = true;
            return ret;
        }
    }

    /** A simple interface that gets called to overlay. */
    public static interface IHighlightOverlay {
        public void onOverlay(MapEffects effects);
    }

    /** A simple tile highlight selection interface. */
    public static interface IHighlightSelector {
        public HighlightSelection handleTile(HexagonTile tile, HighlightSelection currentSelection);
    }

    public static final HighlightSelection VALID_MATERIAL =
            highlightSelectionFromColour(0.1f, 0.6f, 0f);
    public static final HighlightSelection INVALID_MATERIAL =
            highlightSelectionFromColour(1f, 0.08f, 0f);
    public static final HighlightSelection PLAIN_MATERIAL =
            highlightSelectionFromColour(0.7f, 0.94f, 0.98f);
    public static final HighlightSelection ATTACK_MATERIAL =
            highlightSelectionFromColour(0.9f, 0.3f, 0.3f);
    public static final HighlightSelection FOG_MATERIAL =
            highlightSelectionFromColour(0.1f, 0.1f, 0.13f);
    public static final HighlightSelection PLACE_MATERIAL =
            highlightSelectionFromColour(0.3f,1.0f,0.7f);

    private HashMap<HexagonTile, HighlightSelection> mHighlightedTiles = new HashMap<>();
    private Reference<HexagonMap> mMapReference = null;

    /** Turn on to enable default highlighting (territory bounds). */
    @Getter @Setter private boolean mDefaultHighlight = true;
    /** This interface gets called to allow overlaying any selections on top. */
    @Getter @Setter private IHighlightOverlay mHighlightOverlay = null;

    @Getter @Setter private Reference<Player> mActivePlayer;

    public static HighlightSelection highlightSelectionFromColour(float r, float g, float b) {
        return HighlightSelection.with(new Vector4f(r, g, b, 0.2f));
    }

    /**
     * Select a single tile, overriding previous selection.
     *
     * @param tile tile to select
     * @param selection type of highlight to use
     */
    public void highlightTile(HexagonTile tile, HighlightSelection selection) {
        highlightTile(tile, selection, true);
        mDefaultHighlight = false;
    }

    private void highlightTile(HexagonTile tile, HighlightSelection selection, boolean removeOld) {

        if (tile == null || selection == null) {
            return;
        }

        if (removeOld) {
            mHighlightedTiles.remove(tile);
        }
        if (selection.mClear) {
            Reference<HighlightControls> controls = tile.getHighlightControls();

            if (Reference.isValid(controls)) {
                controls.get().setHighlight(0, 0, 0, 0);
            }
            return;
        }
        if (!ensureMapReference()) {
            return;
        }

        Reference<HighlightControls> controls = tile.getHighlightControls();

        if (Reference.isValid(controls)) {
            controls.get().setHighlight(selection.mOverlay);
        }

        mHighlightedTiles.put(tile, selection);
    }

    /**
     * Select multiple tiles by selector handler.
     *
     * <p>This will iterate through all tiles on the map, and call the selector handler to see if
     * any selection should take place
     *
     * @param selector selector that handles tile selection
     */
    public void highlightTiles(IHighlightSelector selector) {
        mMapReference.get().getAllTiles().forEach(t -> highlightTile(t, selector.handleTile(t, mHighlightedTiles.get(t))));
        mDefaultHighlight = false;
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
        mDefaultHighlight = false;
    }

    /**
     * Deselect all tiles.
     *
     * <p>This will clear any selection that currently takes place
     */
    public void unhighlightAllTiles() {
        for (HexagonTile tile : mHighlightedTiles.keySet()) {
            highlightTile(tile, HighlightSelection.CLEARED, false);
        }
        mHighlightedTiles.clear();
    }

    /**
     * Check whether tile is selected.
     *
     * @param tile tile to check
     * @return {@code true} if the tile is currently selected, {@code false} otherwise.
     */
    public boolean isTileHighlighted(HexagonTile tile) {
        return mHighlightedTiles.containsKey(tile);
    }

    /** Get the current highlight type for the tile.
     *
     * @param tile tile to check the highlight for
     * @return highlight selection of the tile. It can be one of the {@link StandardHighlightType}, a custom one, or {@code null}.
     */
    public HighlightSelection getCurrentHighlight(HexagonTile tile) {
        return mHighlightedTiles.get(tile);
    }

    /**
     * Highlight all tiles using default colours.
     *
     * <p>This option essentially draws map bounds of different player teritories
     */
    private void defaultHighlight() {

        Player activePlayer = mActivePlayer != null ? mActivePlayer.get() : null;

        highlightTiles(
                (tile, __) -> {
                    if (activePlayer != null && !activePlayer.hasLost()) {
                        if (!activePlayer.isTileViewable(tile)) {
                            return FOG_MATERIAL;
                        }
                    }

                    Player owner = tile.getClaimant();
                    if (owner != null) {
                        return owner.getPlayerHighlightSelection();
                    }
                    return HighlightSelection.CLEARED;
                });
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {

        if (!ensureMapReference()) {
            return;
        }

        if (mDefaultHighlight) {
            defaultHighlight();
            if (mHighlightOverlay != null) {
                mHighlightOverlay.onOverlay(this);
            }
            mDefaultHighlight = true;
        } else if (mHighlightOverlay != null) {
            mHighlightOverlay.onOverlay(this);
        }
    }

    @Override
    public void onStart() {
        Scene.getActiveScene().registerSingleton(this);
        ensureMapReference();
    }

    @Override
    protected void onDestroy() {
        unhighlightAllTiles();
    }

    private boolean ensureMapReference() {
        if (Reference.isValid(mMapReference)) {
            return true;
        }
        mMapReference = Scene.getActiveScene().getSingletonRef(HexagonMap.class);
        return Reference.isValid(mMapReference);
    }
}
