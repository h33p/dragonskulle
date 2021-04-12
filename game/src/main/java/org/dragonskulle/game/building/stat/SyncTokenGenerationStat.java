/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import org.dragonskulle.game.building.Building;

/**
 * Contains the value used to generate a set number of tokens.
 *
 * @author Craig Wilbourne
 */
public class SyncTokenGenerationStat extends SyncStat<Integer> {

    public SyncTokenGenerationStat(Building building) {
        super(building);
    }

    @Override
    public Integer getValue() {
        return get();
    }
}
