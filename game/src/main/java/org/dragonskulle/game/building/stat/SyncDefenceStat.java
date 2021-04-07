/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import org.dragonskulle.game.building.Building;

/**
 * Contains the value used to defend against an attack.
 *
 * @author Craig Wilbourne
 */
public class SyncDefenceStat extends SyncStat {

    public SyncDefenceStat(Building building) {
        super(building);
    }

    @Override
    public int getValue() {
        // The defence value is identical to the current level number plus one.
        return getLevel() + 1;
    }
}
