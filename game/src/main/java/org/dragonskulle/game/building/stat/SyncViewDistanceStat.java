/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import org.dragonskulle.game.building.Building;

/**
 * Contains the value used to determine the range of view.
 *
 * @author Craig Wilbourne
 */
public class SyncViewDistanceStat extends SyncStat<Integer> {

    public SyncViewDistanceStat(Building building) {
        super(building);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Integer getValue() {
        return 3;
    }
}
