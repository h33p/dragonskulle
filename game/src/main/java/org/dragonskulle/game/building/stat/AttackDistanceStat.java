/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

public class AttackDistanceStat extends Stat<Integer> {

    @Override
    protected Integer getValueFromLevel() {
        return 2;
    }

    @Override
    protected void onDestroy() {}
}
