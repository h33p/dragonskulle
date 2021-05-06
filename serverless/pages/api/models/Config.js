const mongoose = require('mongoose');
var Float = require('mongoose-float').loadType(mongoose, 3);
const ConfigSchema = new mongoose.Schema(
{
  global : {
    inflation : Float,
    mapSize : NUMBER
  },
  player : {
    attackCooldown : Float,
    tokenRate : Float,
    tokenTime : Float,
    inflationPerBuilding : Float,
    attackHeightMul : Float
  },
  ai : {
    lowerBoundTime : Float,
    upperBoundTime : Float
  },
  probabilisticAi : {
    buildProbability : Float,
    upgradeProbability : Float,
    sellProbability : Float,
    attackProbability : Float
  },
  aiAimer : {
    playAStar : Float,
    aimAtCapital : Float,
    tries : NUMBER
  },
  attackStat : {
    value : {
      bonus : {
        multiplier : Float,
        bonusTile : String
      },
      baseValue : Float,
      mulLevel : Float,
      minValue : Float,
      maxValue : Float
    },
    cost : {
      selfLevelMultiplier : Float,
      combinedLevelMultiplier : Float
    }
  },
  buildDistanceStat : {
    value : {
      bonus : {
        multiplier : Float,
        bonusTile : String
      },
      baseValue : Float,
      mulLevel : Float,
      minValue : Float,
      maxValue : Float
    },
    cost : {
      selfLevelMultiplier : Float,
      combinedLevelMultiplier : Float
    }
  },
  claimDistanceStat : {
    value : {
      bonus : {
        multiplier : Float,
        bonusTile : String
      },
      baseValue : Float,
      mulLevel : Float,
      minValue : Float,
      maxValue : Float
    },
    cost : {
      selfLevelMultiplier : Float,
      combinedLevelMultiplier : Float
    }
  },
  defenceStat : {
    value : {
      bonus : {
        multiplier : Float,
        bonusTile : String
      },
      baseValue : Float,
      mulLevel : Float,
      minValue : Float,
      maxValue : Float
    },
    cost : {
      selfLevelMultiplier : Float,
      combinedLevelMultiplier : Float
    }
  },
  generationStat : {
    value : {
      bonus : {
        multiplier : Float,
        bonusTile : String
      },
      baseValue : Float,
      mulLevel : Float,
      minValue : Float,
      maxValue : Float
    },
    cost : {
      selfLevelMultiplier : Float,
      combinedLevelMultiplier : Float
    }
  },
  viewDistanceStat : {
    value : {
      bonus : {
        multiplier : Float,
        bonusTile : String
      },
      baseValue : Float,
      mulLevel : Float,
      minValue : Float,
      maxValue : Float
    },
    cost : {
      selfLevelMultiplier : Float,
      combinedLevelMultiplier : Float
    }
  }
}, { timestamps: false }
);

module.exports = mongoose.models.Config || mongoose.model('Config', ConfigSchema);
