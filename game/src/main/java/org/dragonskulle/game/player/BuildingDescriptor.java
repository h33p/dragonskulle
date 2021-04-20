/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * BuildingDescriptor is used to describe a Building Preset. For example it can define a building
 * using only its values.
 *
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class BuildingDescriptor {
    @Getter
    public final int mAttack;
    @Getter
    public final int mDefence;
    @Getter
    public final int mTokenGeneration;
    @Getter
    public final int mViewDistance;
    @Getter
    public final int mAttackDistance;
    @Getter
    public final int mCost;
    @Getter
    public final int mSellPrice;
    @Getter
    public final String mIconPath;
    @Getter
    public final String mName;

    /**
     * Instantiates a new Building descriptor.
     *  @param mAttack          the attack value
     * @param mDefence         the defence value
     * @param mTokenGeneration the token generation value
     * @param mViewDistance    the view distance value
     * @param mAttackDistance  the attack distance value
     * @param mCost            the cost value
     * @param mSellPrice       the sell price value
     * @param mIconPath        the icon path to be displayed in the menu
     * @param mName the buildings name
     */
    public BuildingDescriptor(
            int mAttack,
            int mDefence,
            int mTokenGeneration,
            int mViewDistance,
            int mAttackDistance,
            int mCost,
            int mSellPrice,
            String mIconPath, String mName) {
        this.mAttack = mAttack;
        this.mDefence = mDefence;
        this.mTokenGeneration = mTokenGeneration;
        this.mViewDistance = mViewDistance;
        this.mAttackDistance = mAttackDistance;
        this.mCost = mCost;
        this.mSellPrice = mSellPrice;
        this.mIconPath = mIconPath;
        this.mName = mName;
    }

    @Override
    public String toString() {
        return String.format(
                "BuildingDescriptor{(%d:%d:%d:%d:%d:%d:%d)",
                mAttack,
                mDefence,
                mTokenGeneration,
                mViewDistance,
                mAttackDistance,
                mCost,
                mSellPrice);
    }
}
