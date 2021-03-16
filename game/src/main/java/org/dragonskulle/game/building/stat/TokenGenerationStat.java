/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

public class TokenGenerationStat extends Stat<Integer> {

    @Override
    protected Integer levelToValue() {
        return mLevel.get();
    }

    @Override
    protected void onDestroy() {

    }
}
