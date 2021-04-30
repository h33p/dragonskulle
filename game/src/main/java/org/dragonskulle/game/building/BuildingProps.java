package org.dragonskulle.game.building;

import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;

import java.util.Set;
@Accessors(prefix = "m")
@Log
public class BuildingProps extends Component {

    private SyncStat mDefenceStat;
    private SyncStat mAttackStat;
    private SyncStat mTokenStat;
    private Building mBuilding;

    /**
     * The base prop mesh.
     */
    private Reference<BuildingProps> mBuildingProps;

    private void onStatChange(Building building){


        Set<HexagonTile> claimedTiles = building.getClaimedTiles();

        mAttackStat = building.getStat(StatType.ATTACK);
        mDefenceStat = building.getStat(StatType.DEFENCE);
        mTokenStat = building.getStat(StatType.TOKEN_GENERATION);



    }

    @Override
    protected void onDestroy() {

    }
}
