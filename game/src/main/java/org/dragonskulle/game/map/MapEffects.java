/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.game.materials.HighlightControls;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.utils.MathUtils;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Client sided highlight effects on the map tiles.
 *
 * @author Aurimas Blažulionis
 *     <p>This component provides client sided effects for the map, such as selecting tiles, marking
 *     valid, and invalid tiles, and so on.
 */
@Accessors(prefix = "m")
public class MapEffects extends Component implements IOnStart, ILateFrameUpdate {

    /** Describes tile highlight option. */
    public static enum StandardHighlightType {
        VALID,
        INVALID,
        PLAIN,
        ATTACK,
        SELECT,
        SELECT_INVALID;

        /**
         * Convert {@link StandardHighlightType} to colour selection.
         *
         * @return selection colour of this tile.
         */
        public Vector4fc asSelection() {
            switch (this) {
                case VALID:
                    return VALID_MATERIAL;
                case INVALID:
                    return INVALID_MATERIAL;
                case PLAIN:
                    return PLAIN_MATERIAL;
                case ATTACK:
                    return ATTACK_MATERIAL;
                case SELECT:
                    return SELECT_MATERIAL;
                case SELECT_INVALID:
                    return SELECT_INVALID_MATERIAL;
                default:
                    return null;
            }
        }
    }

    /** Class controlling pulsating highlighting. */
    private static class PulseHighlight {
        private Vector4fc mTargetColour;
        private final Vector4f mOut = new Vector4f(0f);
        private float mInvPeriod;
        private float mMinLerp;
        private final float mStartTime;
        private float mEndTime;

        /**
         * Constructor for {@link PulseHighlight}.
         *
         * @param targetColour target colour of the highlight
         * @param period period for one oscillation of the highlight
         * @param minVal minimum value of oscillation
         * @param startTime when the pulsating stated
         * @param endTime when the pulsating will end (used by {@link MapEffects} to remove from the
         *     list)
         */
        public PulseHighlight(
                Vector4fc targetColour,
                float period,
                float minVal,
                float startTime,
                float endTime) {
            mStartTime = startTime;
            updateValues(targetColour, period, minVal, endTime);
        }

        /**
         * Set variables for {@link PulseHighlight}.
         *
         * @param targetColour target colour of the highlight
         * @param period period for one oscillation of the highlight
         * @param minVal minimum value of oscillation
         * @param endTime when the pulsating will end (used by {@link MapEffects} to remove from the
         *     list)
         */
        public void updateValues(
                Vector4fc targetColour, float period, float minVal, float endTime) {
            mTargetColour = targetColour;
            mInvPeriod = 1f / period;
            mMinLerp = minVal;
            mEndTime = endTime;
        }

        /**
         * Handle pulsating.
         *
         * <p>This method will interpolate the given colour value to have pulsating.
         *
         * @param curtime current engine time
         * @param cur current colour
         * @return interpolated colour
         */
        private Vector4fc handle(float curtime, Vector4fc cur) {
            float periods = (curtime - mStartTime) * mInvPeriod;
            float lerp = (float) Math.sin(Math.PI * periods) * 0.5f + 0.5f;
            cur.lerp(mTargetColour, MathUtils.lerp(mMinLerp, 1, lerp), mOut);
            return mOut;
        }
    }

    /** A simple interface that gets called to overlay. */
    public static interface IHighlightOverlay {
        /**
         * Handle highlight overlay.
         *
         * @param effects effects instance on which overlay can be added.
         */
        public void onOverlay(MapEffects effects);
    }

    /** A simple tile highlight selection interface. */
    public static interface IHighlightSelector {
        /**
         * Handle tile selection.
         *
         * @param tile tile to highlight.
         * @param currentSelection current highlight colour.
         * @return new highlight colour. Use {@code null}, or {@code currentSelection} to not change
         *     highlight.
         */
        public Vector4fc handleTile(HexagonTile tile, Vector4fc currentSelection);
    }

    public static final Vector4fc VALID_MATERIAL = highlightSelectionFromColour(0.1f, 0.6f, 0f);
    public static final Vector4fc INVALID_MATERIAL = highlightSelectionFromColour(1f, 0f, 0f, 0.1f);
    public static final Vector4fc PLAIN_MATERIAL = highlightSelectionFromColour(0.7f, 0.94f, 0.98f);
    public static final Vector4fc ATTACK_MATERIAL =
            highlightSelectionFromColour(0.9f, 0.3f, 0.3f, 0.5f);
    public static final Vector4fc FOG_MATERIAL = new Vector4f(0.035f, 0.035f, 0.043f, 1.5f);
    public static final Vector4fc SELECT_MATERIAL = highlightSelectionFromColour(0.1f, 0.9f, 0.7f);
    public static final Vector4fc SELECT_INVALID_MATERIAL =
            highlightSelectionFromColour(0.9f, 0.1f, 0.7f);
    public static final Vector4fc CLEARED_MATERIAL = new Vector4f(0f);

    /** Internal reference to the hexagon map. */
    private Reference<HexagonMap> mMapReference = null;

    /** Turn on to enable default highlighting (territory bounds). */
    @Getter @Setter private boolean mDefaultHighlight = true;
    /** This interface gets called to allow overlaying any selections on top. */
    @Getter @Setter private IHighlightOverlay mHighlightOverlay = null;

    /** List of active pulse highlight on each tile. */
    private final Map<HexagonTile, PulseHighlight> mPulseHighlights = new HashMap<>();

    /** Active player used for fog highlights. */
    @Getter @Setter private Reference<Player> mActivePlayer;

    /**
     * Create a new {@link Vector4f} with the specified values.
     *
     * @param r The red.
     * @param g The green.
     * @param b The blue.
     * @return The new HighlightSelection.
     */
    public static Vector4f highlightSelectionFromColour(float r, float g, float b) {
        return highlightSelectionFromColour(r, g, b, 0.25f);
    }

    /**
     * Create a new {@link Vector4f} with the specified values.
     *
     * @param r The red.
     * @param g The green.
     * @param b The blue.
     * @param alpha The alpha.
     * @return The new HighlightSelection.
     */
    public static Vector4f highlightSelectionFromColour(float r, float g, float b, float alpha) {
        return new Vector4f(r, g, b, alpha);
    }

    /**
     * Select a single tile, overriding previous selection.
     *
     * @param tile tile to select
     * @param selection type of highlight to use
     */
    public void highlightTile(HexagonTile tile, Vector4fc selection) {

        if (tile == null || selection == null) {
            return;
        }

        // Only allow fog to have fog highlight
        if (tile.getTileType() == TileType.FOG) {
            selection = FOG_MATERIAL;
        }

        if (!ensureMapReference()) {
            return;
        }

        Reference<HighlightControls> controls = tile.getHighlightControls();

        if (Reference.isValid(controls)) {
            controls.get().setHighlight(selection);
        }
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
        mMapReference
                .get()
                .getAllTiles()
                .forEach(t -> highlightTile(t, selector.handleTile(t, getTileHighlight(t))));
    }

    /**
     * Deselect a tile
     *
     * <p>This method will remove an active selection from the tile.
     *
     * @param tile tile to deselect
     */
    public void unhighlightTile(HexagonTile tile) {
        highlightTile(tile, CLEARED_MATERIAL);
    }

    /**
     * Deselect all tiles.
     *
     * <p>This will clear any selection that currently takes place
     */
    public void unhighlightAllTiles() {
        if (!ensureMapReference()) {
            return;
        }

        mMapReference.get().getAllTiles().forEach(this::unhighlightTile);
    }

    /**
     * Check whether tile is highlighted.
     *
     * @param tile tile to check
     * @return The {@link Vector4f} if the tile has any highlighting, {@code null} otherwise.
     */
    public Vector4f getTileHighlight(HexagonTile tile) {
        Reference<HighlightControls> controls = tile.getHighlightControls();

        if (Reference.isValid(controls)) {
            return controls.get().getTargetColour();
        }

        return null;
    }

    /**
     * Highlight all tiles of a building, and make them pulse.
     *
     * @param building building tiles to highlight
     * @param colour colour of the highlight
     * @param minVal minimum interpolation to the colour
     * @param period how long does one oscillation take
     * @param duration how long should the highlight be active for
     */
    public void pulseHighlight(
            Building building, Vector4fc colour, float minVal, float period, float duration) {
        if (building == null) {
            return;
        }

        building.getClaimedTiles()
                .forEach(t -> pulseHighlight(t, colour, minVal, period, duration));
    }

    /**
     * Highlight a single tile, and make it pulse.
     *
     * @param tile tile to highlight
     * @param colour colour of the highlight
     * @param minVal minimum interpolation to the colour
     * @param period how long does one oscillation take
     * @param duration how long should the highlight be active for
     */
    public void pulseHighlight(
            HexagonTile tile, Vector4fc colour, float minVal, float period, float duration) {

        if (tile == null) {
            return;
        }

        mPulseHighlights.compute(
                tile,
                (__, v) -> {
                    float curtime = Engine.getInstance().getCurTime();
                    if (v == null) {
                        return new PulseHighlight(
                                colour, period, minVal, curtime, curtime + duration);
                    }

                    v.updateValues(colour, period, minVal, curtime + duration);

                    return v;
                });
    }

    /**
     * Highlight all tiles using default colours.
     *
     * <p>This option essentially draws map bounds of different player teritories
     */
    private void defaultHighlight() {
        Player activePlayer = Reference.isValid(mActivePlayer) ? mActivePlayer.get() : null;

        highlightTiles(
                (tile, __) -> {
                    if (activePlayer != null && !activePlayer.gameEnd()) {
                        if (!activePlayer.isTileViewable(tile)) {
                            return FOG_MATERIAL;
                        }
                    }

                    Player owner = tile.getClaimant();
                    if (owner != null) {
                        return owner.getPlayerHighlightSelection();
                    }
                    return CLEARED_MATERIAL;
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

        float curtime = Engine.getInstance().getCurTime();

        highlightTiles(
                (tile, curval) -> {
                    PulseHighlight hl = mPulseHighlights.get(tile);

                    if (hl == null) {
                        return null;
                    }

                    if (hl.mEndTime < curtime) {
                        mPulseHighlights.remove(tile);
                        return null;
                    }

                    return hl.handle(curtime, curval);
                });
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

    /**
     * Ensure the map reference is valid, and try to update if it's not.
     *
     * @return {@code true} if the hexagon map reference is valid, {@code false} if it's not.
     */
    private boolean ensureMapReference() {
        if (Reference.isValid(mMapReference)) {
            return true;
        }

        if (Scene.getActiveScene() == null) {
            return false;
        }

        mMapReference = Scene.getActiveScene().getSingletonRef(HexagonMap.class);
        return Reference.isValid(mMapReference);
    }
}
