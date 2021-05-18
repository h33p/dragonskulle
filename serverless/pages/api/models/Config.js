const mongoose = require('mongoose');
var Float = require('mongoose-float').loadType(mongoose, 3);
const ConfigSchema = new mongoose.Schema(
    {
        global: {
            inflation: Float,
            mapSize: Number
        },
        attackStat: {
            value: {
                baseValue: Float,
                mulLevel: Float,
                minValue: Float,
                maxValue: Float,
                bonus: {
                    multiplier: Float,
                    bonusTile: String
                },
            },
            cost: {
                selfLevelMultiplier: Float,
            }
        },
        buildDistanceStat: {
            value: {
                baseValue: Float,
                mulLevel: Float,
                minValue: Float,
                maxValue: Float,
                bonus: {
                    multiplier: Float,
                    bonusTile: String
                },
            },
            cost: {
                selfLevelMultiplier: Float,
            }
        },
        claimDistanceStat: {
            value: {
                baseValue: Float,
                mulLevel: Float,
                minValue: Float,
                maxValue: Float,
                bonus: {
                    multiplier: Float,
                    bonusTile: String
                },
            },
            cost: {
                selfLevelMultiplier: Float,
            }
        },
        defenceStat: {
            value: {
                baseValue: Float,
                mulLevel: Float,
                minValue: Float,
                maxValue: Float,
                bonus: {
                    multiplier: Float,
                    bonusTile: String
                },
            },
            cost: {
                selfLevelMultiplier: Float,
            }
        },
        generationStat: {
            value: {
                baseValue: Float,
                mulLevel: Float,
                minValue: Float,
                maxValue: Float,
                bonus: {
                    multiplier: Float,
                    bonusTile: String
                },
            },
            cost: {
                selfLevelMultiplier: Float,
            }
        },
        viewDistanceStat: {
            value: {
                baseValue: Float,
                mulLevel: Float,
                minValue: Float,
                maxValue: Float,
                bonus: {
                    multiplier: Float,
                    bonusTile: String
                },
            },
            cost: {
                selfLevelMultiplier: Float,
            }
        },
        player: {
            attackCooldown: Float,
            tokenRate: Float,
            tokenTime: Float,
            inflationPerBuilding: Float,
            attackHeightMul: Float,
            capitalInflationBonus: Float,
            buildingInflationBonus: Float,
            buildingLostInflationBonus: Float
        },
        ai: [{
            lowerBoundTime: Float,
            upperBoundTime: Float,
            probabilisticAi: {
                upgradeProbability: Float,
                attackProbability: Float,
                sellProbability: Float,
                buildProbability: Float
            },
            aiAimer: {
                playAStar: Float,
                maxAttempts: Number,
                tries: Number
            },
        },
            {
                lowerBoundTime: Float,
                upperBoundTime: Float,
                probabilisticAi: {
                    upgradeProbability: Float,
                    attackProbability: Float,
                    sellProbability: Float,
                    buildProbability: Float
                },
                aiAimer: {
                    playAStar: Float,
                    maxAttempts: Number,
                    tries: Number
                },
            },
            {
                lowerBoundTime: Float,
                upperBoundTime: Float,
                probabilisticAi: {
                    upgradeProbability: Float,
                    attackProbability: Float,
                    sellProbability: Float,
                    buildProbability: Float
                },
                aiAimer: {
                    playAStar: Float,
                    maxAttempts: Number,
                    tries: Number
                },
            },
            {
                lowerBoundTime: Float,
                upperBoundTime: Float,
                probabilisticAi: {
                    upgradeProbability: Float,
                    attackProbability: Float,
                    sellProbability: Float,
                    buildProbability: Float
                },
                aiAimer: {
                    playAStar: Float,
                    maxAttempts: Number,
                    tries: Number
                },
            }
        ]
    }, {timestamps: false}
);

module.exports = mongoose.models.Config || mongoose.model('Config', ConfigSchema);
