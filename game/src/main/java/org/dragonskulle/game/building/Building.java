/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.game.building.stat.AttackStat;
import org.dragonskulle.game.building.stat.DefenceStat;

@Log
public class Building extends Component {

    private AttackStat mAttack = new AttackStat();
    private DefenceStat mDefence = new DefenceStat();

    public Building() {
        log.info("Building.");
        /*
        log.info("attack at level -1: " + mAttack.map(-1));
        log.info("attack at level 0: " + mAttack.map(0));
        log.info("attack at level 1: " + mAttack.map(1));
        log.info("attack at level 2: " + mAttack.map(2));
        log.info("attack at level 3: " + mAttack.map(3));
        log.info("attack at level 4: " + mAttack.map(4));
        log.info("attack at level 5: " + mAttack.map(5));
        log.info("attack at level 6: " + mAttack.map(6));
        */

        /*
        log.info("defence level: " + mDefence.getLevel());
        log.info("defence value: " + mDefence.getValue());
        mDefence.increaseLevel();
        log.info("defence value: " + mDefence.getValue());
        mDefence.increaseLevel();
        log.info("defence value: " + mDefence.getValue());
        mDefence.decreaseLevel();
        log.info("defence value: " + mDefence.getValue());
        */
        
        /*
        log.info("attack level: " + mAttack.getLevel());
        log.info("attack value: " + mAttack.getValue());
        mAttack.increaseLevel();
        log.info("attack value: " + mAttack.getValue());
        mAttack.increaseLevel();
        log.info("attack value: " + mAttack.getValue());
        mAttack.decreaseLevel();
        log.info("attack value: " + mAttack.getValue());
		*/
        
        String l0 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l1 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l2 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l3 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l4 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        mAttack.increaseLevel();
        String l5 = String.format("Level: %d \tValue: %f\n", mAttack.getLevel(), mAttack.getValue());
        
        
        log.info(
        		l0 + l1 + l2 + l3 + l4 + l5
        );
        
    }

    @Override
    protected void onDestroy() {}
}
