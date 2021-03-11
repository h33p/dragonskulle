/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

public class DefenseStat extends Stat<Double> {

    @Override
    protected Double levelToValue() {
        switch (mLevel) {
            case 0:
                return 0d;
            case 1:
                return 0.2;
            case 2:
                return 0.4;
            case 3:
                return 0.6;
            case 4:
                return 0.8;
            case 5:
                return 1d;
            default:
                return 0d;
        }
    }
}
