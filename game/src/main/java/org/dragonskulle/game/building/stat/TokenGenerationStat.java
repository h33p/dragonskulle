package org.dragonskulle.game.building.stat;

public class TokenGenerationStat extends Stat<Integer> {

	@Override
	protected Integer levelToValue() {
		return mLevel;
	}

}
