/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

public class ViewDistanceStat extends Stat<Integer> {

    @Override
    protected Integer levelToValue() {
        return 3;
    }

    @Override
    protected void onDestroy() {}
}
