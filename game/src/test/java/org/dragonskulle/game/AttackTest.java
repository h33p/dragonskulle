/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertEquals;

import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.junit.Before;
import org.junit.Test;

/**
 * This class will test the attack component and what happens when you upgrade a building
 *
 * @author DragonSkulle
 */
public class AttackTest {

    private Building mAttacker;
    private Building mDefender;

    @Before
    public void setup() {
        mAttacker = new Building();
        mDefender = new Building();

        mAttacker.onAwake();
        mDefender.onAwake();
    }

    /**
     * This will do the actual attacking
     *
     * @param attacker The attacking {@link Building}
     * @param defender The defending {@link Building}
     * @return The percentage of wins
     */
    private float runAttack(Building attacker, Building defender) {
        float wins = 0f;
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            if (attacker.attack(defender)) {
                wins++;
            }
        }

        return wins / iterations;
    }

    /** This method tests a basic attack done by 2 buildings */
    @Test
    public void basicAttack() {
        float percentageOfWins = runAttack(mAttacker, mDefender);

        assertEquals(0.5f, percentageOfWins, 0.1f);
    }

    /**
     * This will test when an attacker is max level and a defender is lowest level that attacker
     * wins on average more
     */
    @Test
    public void attackerIsMax() {
        SyncStat stat = mAttacker.getStat(StatType.ATTACK);

        stat.set(SyncStat.LEVEL_MAX);
        // Update the building on the server.
        mAttacker.afterStatChange();

        float percentageOfWins = runAttack(mAttacker, mDefender);

        assertEquals(0.7f, percentageOfWins, 0.1f);
    }

    /**
     * This will test that when a defender is max level and attacker isn't that defender wins most
     */
    @Test
    public void defenderIsMax() {
        SyncStat stat = mDefender.getStat(StatType.DEFENCE);

        stat.set(SyncStat.LEVEL_MAX);
        // Update the building on the server.
        mDefender.afterStatChange();

        float percentageOfWins = runAttack(mAttacker, mDefender);

        assertEquals(0.3f, percentageOfWins, 0.1f);
    }

    /**
     * This will test that the amount of wins for attacking is at 50% when both buildings levels are
     * the same
     */
    @Test
    public void equalAttackDefence() {
        float percentageOfWins = runAttack(mAttacker, mDefender);

        assertEquals(0.5f, percentageOfWins, 0.1f);

        SyncStat statAttacker = mAttacker.getStat(StatType.ATTACK);
        SyncStat statDefender = mDefender.getStat(StatType.DEFENCE);
        for (int i = 0; i < 6; i++) {

            statAttacker.increaseLevel();
            statDefender.increaseLevel();

            mAttacker.afterStatChange();
            mDefender.afterStatChange();

            percentageOfWins = runAttack(mAttacker, mDefender);

            assertEquals(0.5f, percentageOfWins, 0.1f);
        }
    }

    /** This is a small test when buildings are at different levels */
    @Test
    public void differentLevels() {
        SyncStat statAttacker = mAttacker.getStat(StatType.ATTACK);
        SyncStat statDefender = mDefender.getStat(StatType.DEFENCE);

        statAttacker.increaseLevel();
        statAttacker.increaseLevel();

        mAttacker.afterStatChange();

        float percentageOfWins = runAttack(mAttacker, mDefender);

        assertEquals(0.6f, percentageOfWins, 0.1f);

        statDefender.increaseLevel();
        statDefender.increaseLevel();
        statDefender.increaseLevel();

        mDefender.afterStatChange();

        percentageOfWins = runAttack(mAttacker, mDefender);

        assertEquals(0.4f, percentageOfWins, 0.1f);
    }
}
