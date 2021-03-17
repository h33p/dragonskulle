/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

/**
 * Contains the value used to defend against an attack.
 *
 * @author Craig Wilbourne
 */
public class SyncDefenceStat extends SyncStat<Integer> {

    @Override
    protected Integer getValueFromLevel() {
        return get() + 1;
    }
}
