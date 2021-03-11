package org.dragonskulle.game.building.stat;

public class ViewDistanceStat extends Stat<Integer> {

	@Override
	protected Integer levelToValue() {
		return mLevel;
	}

}
