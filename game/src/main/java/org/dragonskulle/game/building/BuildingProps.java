/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.TransformHex;
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
public class BuildingProps extends Component implements IOnAwake {
    @Setter private Building mBuilding;
    private TileProp mAttackProp;
    private TileProp mDefenceProp;
    private TileProp mTokenGenProp;
    private HexagonMap mMap;

    public BuildingProps(Building building) {
        mBuilding = building;
    }

    public void onStatChange() {
        updateProp(StatType.ATTACK);
        updateProp(StatType.DEFENCE);
        updateProp(StatType.TOKEN_GENERATION);
        log.info("ran visuals");
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onAwake() {
        mMap = Scene.getActiveScene().getSingleton(HexagonMap.class);
        createProps();
    }

    private void createProps() {
        List<HexagonTile> triad = getTileTriad();
        if (triad.size() != 3) return;
        if (mAttackProp == null) createProp(StatType.ATTACK, triad.get(0));
        if (mDefenceProp == null) createProp(StatType.DEFENCE, triad.get(1));
        if (mTokenGenProp == null) createProp(StatType.TOKEN_GENERATION, triad.get(2));
        log.info("created props on tiles");
    }

    private void updateProp(StatType type) {
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
        if (prop != null) prop.updateProp();
        log.info("updated prop");
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
                break;
            case DEFENCE:
                mDefenceProp = new TileProp(type);
                mMap.getGameObject()
                        .buildChild(
                                "defence_prop",
                                new TransformHex(dest.getQ(), dest.getR(), 0),
                                self -> self.addComponent(mDefenceProp));

                break;
            case TOKEN_GENERATION:
                mTokenGenProp = new TileProp(type);
                mMap.getGameObject()
                        .buildChild(
                                "tgen_prop",
                                new TransformHex(dest.getQ(), dest.getR(), 0),
                                self -> self.addComponent(mTokenGenProp));
                break;
        }
    }

    private List<HexagonTile> getTileTriad() {
        List<HexagonTile> surroundingTiles = new ArrayList<>(mBuilding.getSurroundingTiles());
        return IntStream.range(0, surroundingTiles.size())
                .filter(n -> n % 2 == 0)
                .mapToObj(surroundingTiles::get)
                .collect(Collectors.toList());
    }
}
