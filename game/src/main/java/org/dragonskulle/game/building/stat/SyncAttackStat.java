/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import org.dragonskulle.game.building.Building;

/**
 * Contains the value used to make an attack.
 *
 * @author Craig Wilbourne
 */
public class SyncAttackStat extends SyncStat {

    public SyncAttackStat(Building building) {
        super(building);
    }

    @Override
    public int getValue() {
        return getLevel() + 1;
    }
}
