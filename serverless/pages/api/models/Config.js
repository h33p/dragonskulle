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
    }
  }
}, { timestamps: false }
);

module.exports = mongoose.models.Config || mongoose.model('Config', ConfigSchema);
