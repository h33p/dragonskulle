const mongoose = require('mongoose');
var Float = require('mongoose-float').loadType(mongoose, 3);
const ConfigSchema = new mongoose.Schema({
    SyncStat: {
        LEVEL_MIN: Number,
        LEVEL_MAX: Number
    },
    Player: {
        ATTACK_COOLDOWN: Float,
        TOKEN_RATE: Float,
        TOKEN_TIME: Number
    },
    ProbabilisticAiPlayer: {
        mBuildProbability: Float,
        mUpgradeProbability: Float,
        mAttackProbability: Float,
        mSellProbability: Float
    },
    AiAimer: {
        PLAY_A_STAR: Float,
        AIM_AT_CAPITAL: Float,
        TRIES: Number
    }
},
    { timestamps: true }
);

module.exports = mongoose.models.Config || mongoose.model('Config', ConfigSchema);