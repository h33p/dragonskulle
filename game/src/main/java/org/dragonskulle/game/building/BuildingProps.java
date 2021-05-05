/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;

/**
 * Building Props are displayed around the {@link Building}, they will update depending on the
 * {@link SyncStat} value and type.
 *
 * @author Oscar L
 * @author Leela
 */
@Accessors(prefix = "m")
@Log
public class BuildingProps extends Component implements IOnAwake, IFrameUpdate, IOnStart {
    @Setter private Building mBuilding;
    private TileProp mAttackProp;
    private TileProp mDefenceProp;
    private TileProp mTokenGenProp;
    private HexagonMap mMap;

    @Accessors(fluent = true, prefix = "m")
    @Getter()
    private boolean mShouldRespawnProps = true;

    private int mSpawnPropAttempts = 0;
    private final int mPropLimit =
            30; // hopefully we can remove this, need to delay until the building was created
    // i know theres a getter somewhere
    private StatType[] mShopProps =
            new StatType[] {StatType.ATTACK, StatType.DEFENCE, StatType.TOKEN_GENERATION};

    /**
     * Constructor.
     *
     * @param building the building
     */
    public BuildingProps(Building building) {
        mBuilding = building;
    }

    /**
     * When a stat changes, tell its assigned Prop to update its mesh.
     *
     * @param type the type
     */
    public void onStatChange(StatType type) {
        // will calculate which prop to show
        TileProp prop = null;
        SyncStat newStat = mBuilding.getStat(type);
        switch (type) {
            case ATTACK:
                prop = this.mAttackProp;
                break;
            case DEFENCE:
                prop = this.mDefenceProp;
                break;
            case TOKEN_GENERATION:
                prop = this.mTokenGenProp;
                break;
        }
        if (prop != null) prop.updateProp(newStat.getLevel());
        log.info("treated stat " + type);
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onAwake() {
        mMap = Scene.getActiveScene().getSingleton(HexagonMap.class);
    }

    /**
     * Creates all props for the {@link Building}. It will not always be able to build every type of
     * prop due to Tile availability.
     */
    private void createProps() {
        if (mBuilding == null) return;

        TileProp[] props = {mAttackProp, mDefenceProp, mTokenGenProp};
        List<HexagonTile> possibleTriad = getTileTriad();
        if (possibleTriad.size() == 0) {
            markSpawnProps(mSpawnPropAttempts++ <= mPropLimit);
            return;
        }
        markSpawnProps(false);
        for (int i = 0; i < possibleTriad.size(); i++) {
            HexagonTile tile = possibleTriad.get(i);
            if (tile == null) continue;
            TileProp prop = props[i];
            SyncStat stat = mBuilding.getStat(mShopProps[i]);
            if (prop == null) createProp(stat, tile);
        }
    }

    /**
     * Used as a flag to show if the Prop was succesfully spawned. This is needed because it will
     * sometimes be ran before the {@link Building} has been spawned.
     *
     * @param state the state, true or false
     */
    private void markSpawnProps(boolean state) {
        this.mShouldRespawnProps = state;
    }

    /**
     * Creates a singular prop for the building and places it on the tile.
     *
     * @param stat the stat
     * @param dest the dest
     */
    private void createProp(SyncStat stat, HexagonTile dest) {
        if (dest.getTileType() != HexagonTile.TileType.LAND) return;
        switch (stat.getType()) {
            case ATTACK:
                mAttackProp = new TileProp(StatType.ATTACK, stat.getLevel(), dest.getHeight());
                mMap.getGameObject()
                        .buildChild(
                                "attack_prop",
                                new TransformHex(dest.getQ(), dest.getR(), 0),
                                self -> self.addComponent(mAttackProp));
                dest.setProp(mAttackProp);
                break;
            case DEFENCE:
                mDefenceProp = new TileProp(StatType.DEFENCE, stat.getLevel(), dest.getHeight());
                mMap.getGameObject()
                        .buildChild(
                                "defence_prop",
                                new TransformHex(dest.getQ(), dest.getR(), 0),
                                self -> self.addComponent(mDefenceProp));
                dest.setProp(mAttackProp);
                break;
            case TOKEN_GENERATION:
                mTokenGenProp =
                        new TileProp(StatType.TOKEN_GENERATION, stat.getLevel(), dest.getHeight());
                mMap.getGameObject()
                        .buildChild(
                                "tgen_prop",
                                new TransformHex(dest.getQ(), dest.getR(), 0),
                                self -> self.addComponent(mTokenGenProp));
                dest.setProp(mAttackProp);
                break;
        }
    }

    /**
     * Select at most 3 tiles surrounding the building to have props placed on them. If 3 cannot be
     * found, {@code null} is put in the array.
     *
     * @return the tile triad
     */
    private List<HexagonTile> getTileTriad() {
        // there must be a much simpler way to get the same random unclaimed
        // tiles on both server and client
        List<HexagonTile> triad =
                mBuilding.getSurroundingTiles().stream()
                        .filter((t) -> !t.hasProp())
                        .sorted(BuildingProps::compareHexagonTileCoords)
                        .collect(Collectors.toList());
        return triad.size() > 3 ? triad.subList(0, 3) : triad;
    }

    /**
     * Compare two {@link HexagonTile}'s so that they can be ordered.
     *
     * @param a tile one
     * @param b tile two
     * @return an integer, in the same form as {@link Integer#compare(int, int)}
     */
    private static int compareHexagonTileCoords(HexagonTile a, HexagonTile b) {
        if (a == null) return -1;
        if (b == null) return 1;
        int c = Integer.compare(a.getQ(), b.getQ());
        return (c == 0) ? Integer.compare(a.getR(), b.getR()) : c * (-1);
    }

    @Override
    public void frameUpdate(float deltaTime) {
        if (shouldRespawnProps()) createProps();
    }

    @Override
    public void onStart() {
        createProps();
    }
}
