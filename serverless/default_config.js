{
  "global" : {
    "inflation" : 1.002,
    "mapSize" : 51
  },
  "player" : {
    "attackCooldown" : 2.0,
    "tokenRate" : 5.0,
    "tokenTime" : 1.0,
    "inflationPerBuilding" : 1.05,
    "attackHeightMul" : 1.0
  },
  "ai" : {
    "lowerBoundTime" : 1.0,
    "upperBoundTime" : 2.0
  },
  "probabilisticAi" : {
    "buildProbability" : 0.65,
    "upgradeProbability" : 0.15,
    "sellProbability" : 0.05,
    "attackProbability" : 0.15
  },
  "aiAimer" : {
    "playAStar" : 0.9,
    "aimAtCapital" : 0.01,
    "tries" : 10
  },
  "attackStat" : {
    "value" : {
      "bonus" : {
        "multiplier" : 0.0,
        "bonusTile" : null
      },
      "baseValue" : 0.0,
      "mulLevel" : 1.0,
      "minValue" : 0.0,
      "maxValue" : 100.0
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "buildDistanceStat" : {
    "value" : {
      "bonus" : {
        "multiplier" : 0.0,
        "bonusTile" : null
      },
      "baseValue" : 2.0,
      "mulLevel" : 0.0,
      "minValue" : 0.0,
      "maxValue" : 100.0
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "claimDistanceStat" : {
    "value" : {
      "bonus" : {
        "multiplier" : 0.0,
        "bonusTile" : null
      },
      "baseValue" : 1.0,
      "mulLevel" : 0.0,
      "minValue" : 0.0,
      "maxValue" : 100.0
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "defenceStat" : {
    "value" : {
      "bonus" : {
        "multiplier" : 0.5,
        "bonusTile" : "MOUNTAIN"
      },
      "baseValue" : -1.0,
      "mulLevel" : 1.0,
      "minValue" : 0.0,
      "maxValue" : 100.0
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "generationStat" : {
    "value" : {
      "bonus" : {
        "multiplier" : 1.0,
        "bonusTile" : "WATER"
      },
      "baseValue" : -1.0,
      "mulLevel" : 1.0,
      "minValue" : 0.0,
      "maxValue" : 100.0
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "viewDistanceStat" : {
    "value" : {
      "bonus" : {
        "multiplier" : 0.0,
        "bonusTile" : null
      },
      "baseValue" : 3.0,
      "mulLevel" : 0.0,
      "minValue" : 0.0,
      "maxValue" : 3.0
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  }
}