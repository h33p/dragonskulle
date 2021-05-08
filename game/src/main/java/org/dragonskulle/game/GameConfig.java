/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.futures.AwaitFuture;
import org.dragonskulle.core.futures.Future;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.lobby.GameAPI;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.network.components.sync.INetSerializable;
import org.dragonskulle.network.components.sync.ISyncVar;

/**
 * Configurable game properties.
 *
 * @author Aurimas Blažulionis
 *     <p>This class allows to customize game configuration very easily. It will be synced by {@link
 *     GameState} upon clients connecting to the game.
 */
@Accessors(prefix = "m")
@Getter
@Setter
@JsonIgnoreProperties({"_id", "__v"})
public class GameConfig implements ISyncVar {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String OVERRIDE_CONFIG_PATH = "game_config_override.json";
    private static final String CACHED_CONFIG_PATH = "game_config_cache.json";

    @Accessors(prefix = "s")
    @Getter
    private static GameConfig sDefaultConfig = new GameConfig();

    /**
     * Queue an async refresh of the default config.
     *
     * @return a future that blocks until async web request finishes and game config is fully
     *     updated.
     */
    public static Future refreshConfig() {
        boolean[] finished = {false};

        GameConfig fileConfig = fromFile(OVERRIDE_CONFIG_PATH);

        if (fileConfig != null) {
            finished[0] = true;
            sDefaultConfig = fileConfig;
        } else {
            fileConfig = fromFile(CACHED_CONFIG_PATH);

            if (fileConfig != null) {
                sDefaultConfig = fileConfig;
            }

            GameAPI.getCurrentConfigAsync(
                    (data, success) -> {
                        if (success) {
                            GameConfig c = fromJson(data);

                            if (c != null) {
                                sDefaultConfig = c;
                                try (FileOutputStream out =
                                        new FileOutputStream(CACHED_CONFIG_PATH)) {
                                    try (PrintStream pout = new PrintStream(out)) {
                                        pout.print(data);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        finished[0] = true;
                    });
        }

        return new AwaitFuture((__) -> finished[0]);
    }

    /**
     * Convert this config to JSON.
     *
     * @return JSON representation of the config.
     */
    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Constructor for {@link GameConfig}. */
    public GameConfig() {
        mAi.add(new AiConfig());
    }

    /**
     * Build game config from JSON.
     *
     * @param jsonData json string representing the config data.
     * @return new instance of {@link GameConfig}, or {@code null}, if parsing fails.
     */
    public static GameConfig fromJson(String jsonData) {
        try {
            return MAPPER.readValue(jsonData, GameConfig.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static GameConfig fromFile(String path) {
        File file = new File(path);

        try {
            return MAPPER.readValue(file, GameConfig.class);
        } catch (IOException e) {
            return null;
        }
    }

    /** Global game properties. */
    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class GlobalConfig implements INetSerializable {
        /** Global price inflation rate (per second). */
        private float mInflation;
        /** Map to size spawn. */
        private int mMapSize;

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

    /** Player specific properties. */
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
            this(2, 5, 1, 1.04f, 1);
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

    /** Probabilistic AI properties. */
    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class ProbabilisticAiConfig implements INetSerializable {
        /** Probability of placing a new {@link Building}. */
        private float mBuildProbability;
        /** Probability of upgrading an owned {@link Building}. */
        private float mUpgradeProbability;
        /** Probability of attacking an opponent {@link Building}. */
        private float mAttackProbability;
        /** Probability of selling an owned {@link Building}. */
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
            this(0.65f, 0.155f, 0.19f, 0.005f);
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

    /** Aimer AI properties. */
    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class AiAimerConfig implements INetSerializable {
        /** Whether to use the A* route. */
        private float mPlayAStar;
        /** The number of attempts before it will always aim for a Capital */
        private int mMaxAttempts;
        /** This is the number of tries we should do before resetting. */
        private int mTries;

        public AiAimerConfig(float playAStar, int maxAttempts, int tries) {
            mPlayAStar = playAStar;
            mMaxAttempts = maxAttempts;
            mTries = tries;
        }

        public AiAimerConfig() {
            this(0.75f, 50, 5);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mPlayAStar);
            stream.writeInt(mMaxAttempts);
            stream.writeInt(mTries);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mPlayAStar = stream.readFloat();
            mMaxAttempts = stream.readInt();
            mTries = stream.readInt();
        }
    }

    /** AI properties. */
    @Getter
    @Setter
    @Accessors(prefix = "m")
    @JsonIgnoreProperties({"_id"})
    public static class AiConfig implements INetSerializable {
        private float mLowerBoundTime;
        private float mUpperBoundTime;

        /** Properties for probabilistic AI. */
        private ProbabilisticAiConfig mProbabilisticAi;
        /** Properties for aimer AI. */
        private AiAimerConfig mAiAimer;

        public AiConfig(float lowerBoundTime, float upperBoundTime) {
            mLowerBoundTime = lowerBoundTime;
            mUpperBoundTime = upperBoundTime;
            mProbabilisticAi = new ProbabilisticAiConfig();
            mAiAimer = new AiAimerConfig();
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

    /** Stat cost properties. */
    @Getter
    @Setter
    @Accessors(prefix = "m")
    public static class StatCostConfig implements INetSerializable {
        private float mSelfLevelMultiplier;

        public StatCostConfig(float selfLevelMultiplier) {
            mSelfLevelMultiplier = selfLevelMultiplier;
        }

        public StatCostConfig() {
            this(3f);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mSelfLevelMultiplier);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mSelfLevelMultiplier = stream.readFloat();
        }
    }

    /** Combined stat properties. */
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
                float selfLevelMultiplier) {
            mValue =
                    new StatValueConfig(
                            baseValue, mulLevel, minValue, maxValue, bonusTile, bonusMultiplier);
            mCost = new StatCostConfig(selfLevelMultiplier);
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

    /** Global game properties. */
    private GlobalConfig mGlobal = new GlobalConfig();
    /** Properties for players. */
    private PlayerConfig mPlayer = new PlayerConfig();

    /** Properties for AI. */
    private List<AiConfig> mAi = new ArrayList<AiConfig>();

    // The attack value is identical to the current level number.
    /** Attack stat configuration. */
    private StatConfig mAttackStat = new StatConfig(0, 1, 0, 100, null, 0);
    // Regardless of the level, the build distance will always be the same.
    /** Build distance stat configuration. */
    private StatConfig mBuildDistanceStat = new StatConfig(2, 0, 0, 100, null, 0);
    // Regardless of the level, the claim distance will always be the same.
    /** Claim distance stat properties. */
    private StatConfig mClaimDistanceStat = new StatConfig(1, 0, 0, 100, null, 0);
    // The defence value is identical to the current level.
    /** Defence stat properties. */
    private StatConfig mDefenceStat = new StatConfig(0, 1, 0, 100, TileType.MOUNTAIN, 0.5f);
    /** Token generation stat properties. */
    private StatConfig mGenerationStat = new StatConfig(-1, 1, 0, 100, TileType.WATER, 0.3f);
    // Regardless of the level, the view distance will always be the same.
    /** View distance stat properties. */
    private StatConfig mViewDistanceStat = new StatConfig(3, 0, 0, 3, null, 0);

    @Override
    public void serialize(DataOutput stream, int clientId) throws IOException {
        mGlobal.serialize(stream, clientId);
        mPlayer.serialize(stream, clientId);

        stream.writeInt(mAi.size());

        for (AiConfig c : mAi) {
            c.serialize(stream, clientId);
        }

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

        int size = stream.readInt();

        mAi.clear();

        for (int i = 0; i < size; i++) {
            AiConfig cfg = new AiConfig();
            cfg.deserialize(stream);
            mAi.add(cfg);
        }

        mAttackStat.deserialize(stream);
        mBuildDistanceStat.deserialize(stream);
        mClaimDistanceStat.deserialize(stream);
        mDefenceStat.deserialize(stream);
        mGenerationStat.deserialize(stream);
        mViewDistanceStat.deserialize(stream);
    }

    @Override
    public boolean isDirty(int clientId) {
        // Always false, because config is only meant to be synced once.
        return false;
    }
}
