/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;

/** @author Oscar L */
@Accessors(prefix = "m")
public class BuildingDescriptor {
    @Getter public final int mAttack;
    @Getter public final int mDefence;
    @Getter public final int mTokenGeneration;
    @Getter public final int mViewDistance;
    @Getter public final int mAttackDistance;
    @Getter public final int mCost;
    @Getter public final int mSellPrice;

    public BuildingDescriptor(
            int mAttack,
            int mDefence,
            int mTokenGeneration,
            int mViewDistance,
            int mAttackDistance,
            int mCost,
            int mSellPrice) {
        this.mAttack = mAttack;
        this.mDefence = mDefence;
        this.mTokenGeneration = mTokenGeneration;
        this.mViewDistance = mViewDistance;
        this.mAttackDistance = mAttackDistance;
        this.mCost = mCost;
        this.mSellPrice = mSellPrice;
    }

    @Override
    public String toString() {
        return "BuildingDescriptor{"
                + "("
                + mAttack
                + ":"
                + mDefence
                + ":"
                + mTokenGeneration
                + ":"
                + mViewDistance
                + ":"
                + mAttackDistance
                + ":"
                + mCost
                + ":"
                + mSellPrice
                + ")";
    }
}
