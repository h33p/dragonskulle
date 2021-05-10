/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import org.dragonskulle.components.IOnStart;

/**
 * An {@link AimerAi} which is more likely to Build when not playing
 *
 * @author DragonSkulle
 */
public class BuildingAimerAi extends AimerAi implements IOnStart {

    @Override
    public void onStart() {
        /*mAttackProbability = 0.05f;
        mBuildProbability = 0.90f;
        mSellProbability = 0.01f;
        mUpgradeProbability = 0.04f;*/

        super.onStart();
    }
}
