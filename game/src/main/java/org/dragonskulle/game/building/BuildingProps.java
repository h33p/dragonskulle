package org.dragonskulle.game.building;

import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;

import java.util.Set;

public class BuildingProps {

    private SyncStat mStat;

    private Building mBuilding;


    private void onStatChange(Building building){

        mStat = building.getStat(StatType.ATTACK);

       Set<HexagonTile> claimedTiles = building.getClaimedTiles();



    }

}
