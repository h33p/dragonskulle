/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import org.dragonskulle.game.building.Building;

/**
 * Contains the value used to determine the range of attack.
 *
 * @author Craig Wilbourne
 */
public class SyncAttackDistanceStat extends SyncStat {

    public SyncAttackDistanceStat(Building building) {
        super(building);
    }

    @Override
    public int getValue() {
        return 2;
    }
}
