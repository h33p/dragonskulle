/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

/**
 * Contains the value used to generate a set number of tokens.
 *
 * @author Craig Wilbourne
 */
public class TokenGenerationStat extends Stat<Integer> {

    @Override
    protected Integer getValueFromLevel() {
        return mLevel.get();
    }

    @Override
    protected void onDestroy() {}
}
