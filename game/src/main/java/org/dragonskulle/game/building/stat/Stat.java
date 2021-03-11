package org.dragonskulle.game.building.stat;

abstract class Stat<T> {

	public static final int LEVEL_MIN = 0;
	public static final int LEVEL_MAX = 5;
	
	public abstract T map(int level);
	
	protected int boundLevel(int level) {
		if(level < LEVEL_MIN) {
			return LEVEL_MIN;
		} else if(level > LEVEL_MAX) {
			return LEVEL_MAX;
		}
		return level;
	}
	
}
