package org.dragonskulle.game.building.stat;

public class ViewDistanceStat extends Stat<Integer> {

	@Override
	protected Integer levelToValue() {
		// View distance is always 3.
		return 3;
	}

}
