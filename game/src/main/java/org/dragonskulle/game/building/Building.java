package org.dragonskulle.game.building;

import org.dragonskulle.components.Component;
import org.dragonskulle.game.building.stat.AttackStat;

import lombok.extern.java.Log;

@Log
public class Building extends Component {

	private AttackStat mAttack = new AttackStat();
	
	public Building() {
		log.info("Building.");
		log.info("attack at level -1: " + mAttack.map(-1));
		log.info("attack at level 0: " + mAttack.map(0));
		log.info("attack at level 1: " + mAttack.map(1));
		log.info("attack at level 2: " + mAttack.map(2));
		log.info("attack at level 3: " + mAttack.map(3));
		log.info("attack at level 4: " + mAttack.map(4));
		log.info("attack at level 5: " + mAttack.map(5));
		log.info("attack at level 6: " + mAttack.map(6));
	}
	
	@Override
	protected void onDestroy() {}

}
