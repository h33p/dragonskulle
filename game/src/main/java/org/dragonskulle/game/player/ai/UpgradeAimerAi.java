/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import org.dragonskulle.components.IOnStart;

/**
 * An {@link AimerAi} which is more likely to Upgrade when not playing
 *
 * @author DragonSkulle
 */
public class UpgradeAimerAi extends AimerAi implements IOnStart {

    @Override
    public void onStart() {
        /*mAttackProbability = 0.80f;
        mBuildProbability = 0.1f;
        mSellProbability = 0.01f;
        mUpgradeProbability = 0.09f;*/

        super.onStart();
    }
}
