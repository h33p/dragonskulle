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
                                        pout.println(sDefaultConfig.toJson());
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

        AiAimerConfig normalAimer = new AiAimerConfig();

        ProbabilisticAiConfig builder = new ProbabilisticAiConfig(0.90f, 0.04f, 0.05f, 0.01f);
        mAi.add(new AiConfig(1, 2, builder, normalAimer));

        ProbabilisticAiConfig upgrader = new ProbabilisticAiConfig(0.24f, 0.5f, 0.25f, 0.01f);
        mAi.add(new AiConfig(1, 2, upgrader, normalAimer));

        ProbabilisticAiConfig attacker = new ProbabilisticAiConfig(0.1f, 0.09f, 0.8f, 0.01f);
        mAi.add(new AiConfig(1, 2, attacker, normalAimer));
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

    /**
     * Load {@link GameConfig} from a valid JSON file.
     *
     * @param path the path to the file
     * @return the generated game config, or {@code null} if invalid.
     */
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

        /**
         * Constructor for {@link GlobalConfig}.
         *
         * @param inflation inflation to set.
         * @param mapSize map size to spawn.
         */
        public GlobalConfig(float inflation, int mapSize) {
            mInflation = inflation;
            mMapSize = mapSize;
        }

        /** Default constructor for {@link GlobalConfig}. */
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
        /** Attack cooldown to set. */
        private float mAttackCooldown;
        /** Token generation rate to set. */
        private float mTokenRate;
        /** Token generation time to set. */
        private float mTokenTime;
        /** How much inflation a building adds. */
        private float mInflationPerBuilding;
        /** How much advantage a unit of height adds. */
        private float mAttackHeightMul;
        /** How much taking over an enemy capital helps with inflation. */
        private float mCapitalInflationBonus;
        /** How much taking over an enemy building helps with inflation. */
        private float mBuildingInflationBonus;
        /** How much extra losing a building helps with inflation. */
        private float mBuildingLostInflationBonus;

        /**
         * Constructor for {@link PlayerConfig}.
         *
         * @param attackCooldown attack cooldown to set.
         * @param tokenRate token generation rate to set.
         * @param tokenTime how fast the tokens should be generated.
         * @param inflationPerBuilding how much inflation a building adds.
         * @param attackHeightMul how much advantage a unit of height adds.
         * @param capitalInflationBonus how much taking over a capital helps with inflation.
         * @param buildingInflationBonus how much taking over an enemy building helps with
         *     inflation.
         * @param buildingLostInflationBonus how much extra losing a building helps with inflation.
         */
        public PlayerConfig(
                float attackCooldown,
                float tokenRate,
                float tokenTime,
                float inflationPerBuilding,
                float attackHeightMul,
                float capitalInflationBonus,
                float buildingInflationBonus,
                float buildingLostInflationBonus) {
            mAttackCooldown = attackCooldown;
            mTokenRate = tokenRate;
            mTokenTime = tokenTime;
            mInflationPerBuilding = inflationPerBuilding;
            mAttackHeightMul = attackHeightMul;
            mCapitalInflationBonus = capitalInflationBonus;
            mBuildingInflationBonus = buildingInflationBonus;
            mBuildingLostInflationBonus = buildingLostInflationBonus;
        }

        /** Default constructor for {@link PlayerConfig}. */
        public PlayerConfig() {
            this(2, 5, 1, 1.04f, 1, -5, -0.1f, 0.1f);
        }

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeFloat(mAttackCooldown);
            stream.writeFloat(mTokenRate);
            stream.writeFloat(mTokenTime);
            stream.writeFloat(mInflationPerBuilding);
            stream.writeFloat(mAttackHeightMul);
            stream.writeFloat(mCapitalInflationBonus);
            stream.writeFloat(mBuildingInflationBonus);
            stream.writeFloat(mBuildingLostInflationBonus);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mAttackCooldown = stream.readFloat();
            mTokenRate = stream.readFloat();
            mTokenTime = stream.readFloat();
            mInflationPerBuilding = stream.readFloat();
            mAttackHeightMul = stream.readFloat();
            mCapitalInflationBonus = stream.readFloat();
            mBuildingInflationBonus = stream.readFloat();
            mBuildingLostInflationBonus = stream.readFloat();
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

        /**
         * Construtor for {@link ProbabilisticAiConfig}.
         *
         * @param buildProbability probability of placing a building.
         * @param upgradeProbability probability of upgrading a building.
         * @param attackProbability probability of attacking.
         * @param sellProbability probability of selling a building.
         */
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

        /** Default contructor for {@link ProbabilisticAiConfig}. */
        public ProbabilisticAiConfig() {
            // Probabilistic
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
        /** The number of attempts before it will always aim for a Capital. */
        private int mMaxAttempts;
        /** This is the number of tries we should do before resetting. */
        private int mTries;

        /**
         * Constructor for {@link AiAimerConfig}.
         *
         * @param playAStar probability of using AStar algorithm.
         * @param maxAttempts maximum attempts of aiming for enemy capital.
         * @param tries number of tries before resetting attempts.
         */
        public AiAimerConfig(float playAStar, int maxAttempts, int tries) {
            mPlayAStar = playAStar;
            mMaxAttempts = maxAttempts;
            mTries = tries;
        }

        /** Default constructor for {@link AiAimerConfig}. */
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
        /** Lower bound for delaying moves. */
        private float mLowerBoundTime;
        /** Upper bound for delaying moves. */
        private float mUpperBoundTime;

        /** Properties for probabilistic AI. */
        private ProbabilisticAiConfig mProbabilisticAi;
        /** Properties for aimer AI. */
        private AiAimerConfig mAiAimer;

        /**
         * Constructor for {@link AiConfig}.
         *
         * @param lowerBoundTime lower bound for delaying moves.
         * @param upperBoundTime upper bound for delaying moves.
         * @param probabilisticAi {@link ProbabilisticAiConfig} instance.
         * @param aimerAi {@link AiAimerConfig} instance.
         */
        public AiConfig(
                float lowerBoundTime,
                float upperBoundTime,
                ProbabilisticAiConfig probabilisticAi,
                AiAimerConfig aimerAi) {
            mLowerBoundTime = lowerBoundTime;
            mUpperBoundTime = upperBoundTime;
            mProbabilisticAi = probabilisticAi;
            mAiAimer = aimerAi;
        }

        /** Default constructor for {@link AiConfig}. */
        public AiConfig() {
            this(1, 2, new ProbabilisticAiConfig(), new AiAimerConfig());
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
        /** Tile that adds bonus for the stat. */
        private TileType mBonusTile;
        /** How much bonus value a tile adds to stat. */
        private float mMultiplier;

        /**
         * Constructor for {@link StatBonusConfig}.
         *
         * @param bonusTile bonus tile to set.
         * @param multiplier how much bonus a tile adds.
         */
        public StatBonusConfig(TileType bonusTile, float multiplier) {
            mBonusTile = bonusTile;
            mMultiplier = multiplier;
        }

        /** Default constructor for {@link StatBonusConfig}. */
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
        /** Base value of the stat. */
        private float mBaseValue;
        /** How much each level adds to the stat. */
        private float mMulLevel;
        /** Minimum value of the stat. */
        private float mMinValue;
        /** Maximum value of the stat. */
        private float mMaxValue;
        /** Bonus of the stat (applied after minmax bounds). */
        private StatBonusConfig mBonus;

        /**
         * Constructor for {@link StatValueConfig}.
         *
         * @param baseValue base value for the stat.
         * @param mulLevel how much each level adds to the stat.
         * @param minValue minimum value of the stat.
         * @param maxValue maximum value of the stat.
         * @param bonusTile bonus tile to use.
         * @param bonusMultiplier bonus multiplier for the tile.
         */
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

        /** Default constructor for {@link StatValueConfig}. */
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
        /** How much each level of stat costs more. */
        private float mSelfLevelMultiplier;

        /**
         * Constructor for {@link StatCostConfig}.
         *
         * @param selfLevelMultiplier how much each level of stat costs more.
         */
        public StatCostConfig(float selfLevelMultiplier) {
            mSelfLevelMultiplier = selfLevelMultiplier;
        }

        /** Default constructor for {@link StatCostConfig}. */
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
        /** Value value configuration for the stat. */
        private StatValueConfig mValue;
        /** Cost configuration for the stat. */
        private StatCostConfig mCost;

        /**
         * Constructor for {@link StatConfig}.
         *
         * @param baseValue base value for the stat.
         * @param mulLevel how much each level adds to the stat.
         * @param minValue minimum value of the stat.
         * @param maxValue maximum value of the stat.
         * @param bonusTile bonus tile to use.
         * @param bonusMultiplier bonus multiplier for the tile.
         * @param selfLevelMultiplier how much each level of stat costs more.
         */
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

        /**
         * Constructor for {@link StatConfig}.
         *
         * @param baseValue base value for the stat.
         * @param mulLevel how much each level adds to the stat.
         * @param minValue minimum value of the stat.
         * @param maxValue maximum value of the stat.
         * @param bonusTile bonus tile to use.
         * @param bonusMultiplier bonus multiplier for the tile.
         */
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

        /** Default constructor for {@link StatConfig}. */
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
