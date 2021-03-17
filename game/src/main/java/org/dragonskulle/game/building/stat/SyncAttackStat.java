/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

/**
 * Contains the value used to make an attack.
 *
 * @author Craig Wilbourne
 */
public class SyncAttackStat extends SyncStat<Integer> {

    @Override
    protected Integer getValueFromLevel() {
        return get() + 1;
    }
}
