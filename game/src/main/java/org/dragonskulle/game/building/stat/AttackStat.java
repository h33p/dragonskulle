package org.dragonskulle.game.building.stat;

public class AttackStat extends Stat<Integer> {

	@Override
	public Integer map(int level) {
		level = boundLevel(level);
		
		return level * 100;
	}
	
}
