/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import org.dragonskulle.game.building.Building;

/**
 * Contains the value used to defend against an attack.
 *
 * @author Craig Wilbourne
 */
public class SyncDefenceStat extends SyncStat<Integer> {

    public SyncDefenceStat(Building building) {
    	super(building);
	}

	@Override
    public Integer getValue() {
        return get() + 1;
    }
}
