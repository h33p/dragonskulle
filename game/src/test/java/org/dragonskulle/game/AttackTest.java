/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertEquals;

import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.junit.Test;

/**
 * This class will test the attack component and what happens when you upgrade a building
 *
 * @author DragonSkulle
 */
public class AttackTest {

    /**
     * This will do the actual attacking
     *
     * @param attacker The attacking {@code Building}
     * @param defender The defending {@code Building}
     * @return The percentage of wins
     */
    private float actualAttack(Building attacker, Building defender) {
        float wins = 0f;
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            if (attacker.attack(defender)) {
                wins++;
            }
        }

        float percentageOfWins = wins / iterations;
        return percentageOfWins;
    }

    /** This method tests a basic attack done by 2 buildings */
    @Test
    public void basicAttackTest() {
        Building attacker = new Building();
        Building defender = new Building();

        attacker.onAwake();
        defender.onAwake();

        float percentageOfWins = actualAttack(attacker, defender);

        assertEquals(0.5f, percentageOfWins, 0.1f);
    }

    /**
     * This will test when an attacker is max level and a defender is lowest level that attacker
     * wins on average more
     */
    @Test
    public void attackerMaxTest() {

        Building attacker = new Building();
        Building defender = new Building();

        attacker.onAwake();
        defender.onAwake();

        SyncStat stat = attacker.getStat(StatType.ATTACK);

        for (int i = 0; i < 5; i++) {
            stat.increaseLevel();
        }
        // Update the building on the server.
        attacker.afterStatChange();

        float percentageOfWins = actualAttack(attacker, defender);

        assertEquals(0.7f, percentageOfWins, 0.1f);
    }

    /**
     * This will test that when a defender is max level and attacker isn't that defender wins most
     */
    @Test
    public void defenderMaxTest() {
        Building attacker = new Building();
        Building defender = new Building();

        attacker.onAwake();
        defender.onAwake();

        SyncStat stat = defender.getStat(StatType.DEFENCE);

        for (int i = 0; i < 5; i++) {
            stat.increaseLevel();
        }
        // Update the building on the server.
        defender.afterStatChange();

        float percentageOfWins = actualAttack(attacker, defender);

        assertEquals(0.3f, percentageOfWins, 0.1f);
    }

    /**
     * This will test that the amount of wins for attacking is at 50% when both buildings levels are
     * the same
     */
    @Test
    public void bothEqualTest() {
        Building attacker = new Building();
        Building defender = new Building();

        attacker.onAwake();
        defender.onAwake();

        float percentageOfWins = actualAttack(attacker, defender);

        assertEquals(0.5f, percentageOfWins, 0.1f);

        for (int i = 0; i < 6; i++) {
            SyncStat statAttacker = attacker.getStat(StatType.ATTACK);
            SyncStat statDefender = defender.getStat(StatType.DEFENCE);

            statAttacker.increaseLevel();
            statDefender.increaseLevel();

            attacker.afterStatChange();
            defender.afterStatChange();

            percentageOfWins = actualAttack(attacker, defender);

            assertEquals(0.5f, percentageOfWins, 0.1f);
        }
    }

    /** This is a small test when buildings are at different levels */
    @Test
    public void differentLevelsTest() {
        Building attacker = new Building();
        Building defender = new Building();

        attacker.onAwake();
        defender.onAwake();

        SyncStat statAttacker = attacker.getStat(StatType.ATTACK);
        SyncStat statDefender = defender.getStat(StatType.DEFENCE);

        statAttacker.increaseLevel();
        statAttacker.increaseLevel();

        attacker.afterStatChange();

        float percentageOfWins = actualAttack(attacker, defender);

        assertEquals(0.6f, percentageOfWins, 0.1f);

        statDefender.increaseLevel();
        statDefender.increaseLevel();

        defender.afterStatChange();

        percentageOfWins = actualAttack(attacker, defender);

        assertEquals(0.4f, percentageOfWins, 0.1f);
    }
}
