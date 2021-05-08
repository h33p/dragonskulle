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

    public static final int BASE_COST = 10;

    @Getter public final int mAttack;
    @Getter public final int mDefence;
    @Getter public final int mTokenGenerationLevel;
    @Getter public final int mCost;
    @Getter public final int mSellPrice;
    @Getter public final String mIconPath;
    @Getter public final String mName;

    /**
     * Instantiates a new Building descriptor.
     *
     * @param mAttack the attack value
     * @param mDefence the defence value
     * @param mTokenGeneration the token generation value
     * @param mCost the cost value
     * @param mSellPrice the sell price value
     * @param mIconPath the icon path to be displayed in the menu
     * @param mName the buildings name
     */
    public BuildingDescriptor(
            int mAttack,
            int mDefence,
            int mTokenGeneration,
            int mCost,
            int mSellPrice,
            String mIconPath,
            String mName) {
        this.mAttack = mAttack;
        this.mDefence = mDefence;
        this.mTokenGenerationLevel = mTokenGeneration;
        this.mCost = mCost;
        this.mSellPrice = mSellPrice;
        this.mIconPath = mIconPath;
        this.mName = mName;
    }

    /**
     * Get building cost, adjusting for inflation.
     *
     * @param player player to calculate the price for.
     * @return cost that accounts for inflation.
     */
    public int getTotalCost(Player player) {
        float globalInflation = player != null ? player.getGlobalInflation() : 1;
        float playerInflation = player != null ? player.getInflation() : 1;
        return Math.round((mCost - BASE_COST) * globalInflation + BASE_COST * playerInflation);
    }

    /**
     * Get building sell price, adjusting for current inflation.
     *
     * @param player player to calculate the price for.
     * @return price that accounts for inflation.
     */
    public int getSellPrice(Player player) {
        float globalInflation = player != null ? player.getGlobalInflation() : 1;
        float playerInflation = player != null ? player.getInflation() : 1;
        return Math.round(
                (mSellPrice - (BASE_COST / 2)) * globalInflation
                        + (BASE_COST / 2) * playerInflation);
    }

    @Override
    public String toString() {
        return String.format(
                "BuildingDescriptor{(%d:%d:%d:%d:%d)",
                mAttack, mDefence, mTokenGenerationLevel, mCost, mSellPrice);
    }
}
