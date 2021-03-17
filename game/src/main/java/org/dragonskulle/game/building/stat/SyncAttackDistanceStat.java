/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

/**
 * Contains the value used to determine the range of attack.
 *
 * @author Craig Wilbourne
 */
public class SyncAttackDistanceStat extends SyncStat<Integer> {

    @Override
    protected Integer getValueFromLevel() {
        return 2;
    }
}
