/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

/**
 * Contains the value used to determine the range of view.
 *
 * @author Craig Wilbourne
 */
public class ViewDistanceStat extends Stat<Integer> {

    @Override
    protected Integer getValueFromLevel() {
        return 3;
    }

    @Override
    protected void onDestroy() {}
}
