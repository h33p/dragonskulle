/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.*;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;

/**
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
            20; // hopefully we can remove this, need to delay until the building was created

    public BuildingProps(Building building) {
        mBuilding = building;
    }

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
        if (prop != null) prop.updateProp(newStat.getValue());
        log.info("ran visuals for stat " + type);
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onAwake() {
        mMap = Scene.getActiveScene().getSingleton(HexagonMap.class);
    }

    private void createProps() {
        TileProp[] props = {mAttackProp, mDefenceProp, mTokenGenProp};
        StatType[] shopProps = {
            StatType.ATTACK, StatType.DEFENCE, StatType.TOKEN_GENERATION
        }; // i know theres a getter somewhere
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
            if (prop == null) createProp(shopProps[i], tile);
        }
    }

    private void markSpawnProps(boolean state) {
        this.mShouldRespawnProps = state;
    }

    private void createProp(StatType type, HexagonTile dest) {
        if (dest.getTileType() != HexagonTile.TileType.LAND) return;
        switch (type) {
            case ATTACK:
                mAttackProp = new TileProp(type);
                mMap.getGameObject()
                        .buildChild(
                                "attack_prop",
                                new TransformHex(dest.getQ(), dest.getR(), 0),
                                self -> self.addComponent(mAttackProp));
                dest.setProp(mAttackProp);
                break;
            case DEFENCE:
                mDefenceProp = new TileProp(type);
                mMap.getGameObject()
                        .buildChild(
                                "defence_prop",
                                new TransformHex(dest.getQ(), dest.getR(), 0),
                                self -> self.addComponent(mDefenceProp));
                dest.setProp(mAttackProp);
                break;
            case TOKEN_GENERATION:
                mTokenGenProp = new TileProp(type);
                mMap.getGameObject()
                        .buildChild(
                                "tgen_prop",
                                new TransformHex(dest.getQ(), dest.getR(), 0),
                                self -> self.addComponent(mTokenGenProp));
                dest.setProp(mAttackProp);
                break;
        }
    }

    private List<HexagonTile>
            getTileTriad() { // there must be a much simpler way to get the same random unclaimed
        // tiles on both server and client
        List<HexagonTile> triad =
                mBuilding.getSurroundingTiles().stream()
                        .filter((t) -> !t.hasProp())
                        .sorted(
                                (a, b) -> {
                                    if (a == null) return -1;
                                    if (b == null) return 1;
                                    int c = Integer.compare(a.getQ(), b.getQ());
                                    return (c == 0)
                                            ? Integer.compare(a.getR(), b.getR())
                                            : c * (-1);
                                })
                        .collect(Collectors.toList());
        if (triad.size() > 3) triad = triad.subList(0, 3);
        log.warning("triad: " + triad);
        return triad;
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
