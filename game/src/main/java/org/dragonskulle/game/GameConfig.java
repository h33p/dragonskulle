/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.network.components.sync.INetSerializable;

/** Configurable game properties. */
@Accessors(prefix = "m")
@Getter
@Setter
public class GameConfig implements INetSerializable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public GameConfig() {}

    public static GameConfig fromJson(String jsonData) {
        try {
            return MAPPER.readValue(jsonData, GameConfig.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class GlobalConfig implements INetSerializable {
        /** Global price inflation rate (per second). */
        private float mInflation = 1.002f;
        /** Map to size spawn. */
        private int mMapSize = 51;

        public GlobalConfig(float inflation, int mapSize) {
            mInflation = inflation;
            mMapSize = mapSize;
        }

        public GlobalConfig() {
            this(1.002f, 51);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mInflation);
            stream.writeInt(mMapSize);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mInflation = stream.readFloat();
            mMapSize = stream.readInt();
        }
    }

    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class PlayerConfig implements INetSerializable {
        private float mAttackCooldown;
        private float mTokenRate;
        private float mTokenTime;
        private float mInflationPerBuilding;
        private float mAttackHeightMul;

        public PlayerConfig(
                float attackCooldown,
                float tokenRate,
                float tokenTime,
                float inflationPerBuilding,
                float attackHeightMul) {
            mAttackCooldown = attackCooldown;
            mTokenRate = tokenRate;
            mTokenTime = tokenTime;
            mInflationPerBuilding = inflationPerBuilding;
            mAttackHeightMul = attackHeightMul;
        }

        public PlayerConfig() {
            this(2, 5, 1, 1.05f, 1);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mAttackCooldown);
            stream.writeFloat(mTokenRate);
            stream.writeFloat(mTokenTime);
            stream.writeFloat(mInflationPerBuilding);
            stream.writeFloat(mAttackHeightMul);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mAttackCooldown = stream.readFloat();
            mTokenRate = stream.readFloat();
            mTokenTime = stream.readFloat();
            mInflationPerBuilding = stream.readFloat();
            mAttackHeightMul = stream.readFloat();
        }
    }

    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class AiConfig implements INetSerializable {
        private float mLowerBoundTime;
        private float mUpperBoundTime;

        public AiConfig(float lowerBoundTime, float upperBoundTime) {
            mLowerBoundTime = lowerBoundTime;
            mUpperBoundTime = upperBoundTime;
        }

        public AiConfig() {
            this(1, 2);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mLowerBoundTime);
            stream.writeFloat(mUpperBoundTime);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mLowerBoundTime = stream.readFloat();
            mUpperBoundTime = stream.readFloat();
        }
    }

    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class ProbabilisticAiConfig implements INetSerializable {
        private float mBuildProbability;
        private float mUpgradeProbability;
        private float mAttackProbability;
        private float mSellProbability;

        public ProbabilisticAiConfig(
                float buildProbability,
                float upgradeProbability,
                float attackProbability,
                float sellProbability) {
            mBuildProbability = buildProbability;
            mUpgradeProbability = upgradeProbability;
            mAttackProbability = attackProbability;
            mSellProbability = sellProbability;
        }

        public ProbabilisticAiConfig() {
            this(0.65f, 0.15f, 0.15f, 0.05f);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mBuildProbability);
            stream.writeFloat(mUpgradeProbability);
            stream.writeFloat(mAttackProbability);
            stream.writeFloat(mSellProbability);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mBuildProbability = stream.readFloat();
            mUpgradeProbability = stream.readFloat();
            mAttackProbability = stream.readFloat();
            mSellProbability = stream.readFloat();
        }
    }

    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class AiAimerConfig implements INetSerializable {
        private float mPlayAStar;
        private float mAimAtCapital;
        private int mTries;

        public AiAimerConfig(float playAStar, float aimAtCapital, int tries) {
            mPlayAStar = playAStar;
            mAimAtCapital = aimAtCapital;
            mTries = tries;
        }

        public AiAimerConfig() {
            this(0.9f, 0.01f, 10);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mPlayAStar);
            stream.writeFloat(mAimAtCapital);
            stream.writeInt(mTries);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mPlayAStar = stream.readFloat();
            mAimAtCapital = stream.readFloat();
            mTries = stream.readInt();
        }
    }

    /**
     * Configures bonus claimed terrain features provide for the building.
     *
     * <p>Bonus is calculated by counting the number of matching tiles, and multiplying it by the
     * multiplier.
     */
    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class StatBonusConfig implements INetSerializable {
        private TileType mBonusTile;
        private float mMultiplier;

        public StatBonusConfig(TileType bonusTile, float multiplier) {
            mBonusTile = bonusTile;
            mMultiplier = multiplier;
        }

        public StatBonusConfig() {
            this(null, 0);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeByte(mBonusTile == null ? -1 : mBonusTile.getValue());
            stream.writeFloat(mMultiplier);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mBonusTile = TileType.getTile(stream.readByte());
            mMultiplier = stream.readFloat();
        }
    }

    /**
     * Configures stat values based on levels.
     *
     * <p>Stats are calculated like so:
     *
     * <p>{@code clamp(mBaseValue + level * mMulLevel, mMinValue, mMaxValue) + bonus()}
     */
    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class StatValueConfig implements INetSerializable {
        private float mBaseValue;
        private float mMulLevel;
        private float mMinValue;
        private float mMaxValue;
        private StatBonusConfig mBonus;

        public StatValueConfig(
                float baseValue,
                float mulLevel,
                float minValue,
                float maxValue,
                TileType bonusTile,
                float bonusMultiplier) {
            mBaseValue = baseValue;
            mMulLevel = mulLevel;
            mMinValue = minValue;
            mMaxValue = maxValue;
            mBonus = new StatBonusConfig(bonusTile, bonusMultiplier);
        }

        public StatValueConfig() {
            this(0, 1, 0, 100, null, 0);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mBaseValue);
            stream.writeFloat(mMulLevel);
            stream.writeFloat(mMinValue);
            stream.writeFloat(mMaxValue);
            mBonus.serialize(stream, clientId);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mBaseValue = stream.readFloat();
            mMulLevel = stream.readFloat();
            mMinValue = stream.readFloat();
            mMaxValue = stream.readFloat();
            mBonus.deserialize(stream);
        }
    }

    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class StatCostConfig implements INetSerializable {
        private float mSelfLevelMultiplier;
        private float mCombinedLevelMultiplier;

        public StatCostConfig(float selfLevelMultiplier, float combinedLevelMultiplier) {
            mSelfLevelMultiplier = selfLevelMultiplier;
            mCombinedLevelMultiplier = combinedLevelMultiplier;
        }

        public StatCostConfig() {
            this(0.5f, 0.5f);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mSelfLevelMultiplier);
            stream.writeFloat(mCombinedLevelMultiplier);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mSelfLevelMultiplier = stream.readFloat();
            mCombinedLevelMultiplier = stream.readFloat();
        }
    }

    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class StatConfig implements INetSerializable {
        private StatValueConfig mValue;
        private StatCostConfig mCost;

        public StatConfig(
                float baseValue,
                float mulLevel,
                float minValue,
                float maxValue,
                TileType bonusTile,
                float bonusMultiplier,
                float selfLevelMultiplier,
                float combinedLevelMultiplier) {
            mValue =
                    new StatValueConfig(
                            baseValue, mulLevel, minValue, maxValue, bonusTile, bonusMultiplier);
            mCost = new StatCostConfig(selfLevelMultiplier, combinedLevelMultiplier);
        }

        public StatConfig(
                float baseValue,
                float mulLevel,
                float minValue,
                float maxValue,
                TileType bonusTile,
                float bonusMultiplier) {
            mValue =
                    new StatValueConfig(
                            baseValue, mulLevel, minValue, maxValue, bonusTile, bonusMultiplier);
            mCost = new StatCostConfig();
        }

        public StatConfig() {
            mValue = new StatValueConfig();
            mCost = new StatCostConfig();
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            mValue.serialize(stream, clientId);
            mCost.serialize(stream, clientId);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mValue.deserialize(stream);
            mCost.deserialize(stream);
        }
    }

    private GlobalConfig mGlobal = new GlobalConfig();
    private PlayerConfig mPlayer = new PlayerConfig();

    private AiConfig mAi = new AiConfig();
    private ProbabilisticAiConfig mProbabilisticAi = new ProbabilisticAiConfig();
    private AiAimerConfig mAiAimer = new AiAimerConfig();

    private StatConfig mAttackStat = new StatConfig(0, 1, 0, 100, null, 0);
    private StatConfig mBuildDistanceStat = new StatConfig(2, 0, 0, 100, null, 0);
    private StatConfig mClaimDistanceStat = new StatConfig(1, 0, 0, 100, null, 0);
    private StatConfig mDefenceStat = new StatConfig(-1, 1, 0, 100, TileType.MOUNTAIN, 0.5f);
    private StatConfig mGenerationStat = new StatConfig(-1, 1, 0, 100, TileType.WATER, 1f);
    private StatConfig mViewDistanceStat = new StatConfig(3, 0, 0, 3, null, 0);

    @Override
    public void serialize(DataOutput stream, int clientId) throws IOException {
        mGlobal.serialize(stream, clientId);
        mPlayer.serialize(stream, clientId);

        mAi.serialize(stream, clientId);
        mProbabilisticAi.serialize(stream, clientId);
        mAiAimer.serialize(stream, clientId);

        mAttackStat.serialize(stream, clientId);
        mBuildDistanceStat.serialize(stream, clientId);
        mClaimDistanceStat.serialize(stream, clientId);
        mDefenceStat.serialize(stream, clientId);
        mGenerationStat.serialize(stream, clientId);
        mViewDistanceStat.serialize(stream, clientId);
    }

    @Override
    public void deserialize(DataInput stream) throws IOException {
        mGlobal.deserialize(stream);
        mPlayer.deserialize(stream);

        mAi.deserialize(stream);
        mProbabilisticAi.deserialize(stream);
        mAiAimer.deserialize(stream);

        mAttackStat.deserialize(stream);
        mBuildDistanceStat.deserialize(stream);
        mClaimDistanceStat.deserialize(stream);
        mDefenceStat.deserialize(stream);
        mGenerationStat.deserialize(stream);
        mViewDistanceStat.deserialize(stream);
    }
}
