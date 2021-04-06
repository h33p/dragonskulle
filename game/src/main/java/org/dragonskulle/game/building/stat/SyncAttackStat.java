/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import org.dragonskulle.game.building.Building;

/**
 * Contains the value used to make an attack.
 *
 * @author Craig Wilbourne
 */
public class SyncAttackStat extends SyncStat<Integer> {

    public SyncAttackStat(Building building) {
        super(building);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Integer getValue() {
        return get() + 1;
    }
}
