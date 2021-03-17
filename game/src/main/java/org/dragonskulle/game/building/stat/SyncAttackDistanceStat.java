/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

/**
 * Contains the value used to determine the range of attack.
 *
 * @author Craig Wilbourne
 */
public class SyncAttackDistanceStat extends SyncStat<Integer> {

    @Override
    public Integer getValue() {
        return 2;
    }
}
