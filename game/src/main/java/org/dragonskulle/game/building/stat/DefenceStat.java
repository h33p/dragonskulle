/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

/**
 * Contains the value used to defend against an attack.
 *
 * @author Craig Wilbourne
 */
public class DefenceStat extends Stat<Double> implements DoubleMap {

    private final double mValueMin = 0.25;
    private final double mValueMax = 1.0;

    private double mapLevel() {
        return map(
                mValueMin,
                mValueMax,
                Double.valueOf(mLevel.get()),
                Double.valueOf(LEVEL_MIN),
                Double.valueOf(LEVEL_MAX));
    }

    @Override
    protected Double getValueFromLevel() {
        return mapLevel();
    }

    @Override
    protected void onDestroy() {}
}