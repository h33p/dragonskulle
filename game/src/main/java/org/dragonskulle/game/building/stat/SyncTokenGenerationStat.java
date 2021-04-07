/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import org.dragonskulle.game.building.Building;

/**
 * Contains the value used to generate a set number of tokens.
 *
 * @author Craig Wilbourne
 */
public class SyncTokenGenerationStat extends SyncStat {

    public SyncTokenGenerationStat(Building building) {
        super(building);
    }

    @Override
    public int getValue() {
        // The number of tokens to generate is identical to the current level number.
        return getLevel();
    }
}
