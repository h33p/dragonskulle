{
  "global" : {
    "inflation" : 1.002,
    "mapSize" : 51
  },
  "player" : {
    "inflationPerBuilding" : 1.04,
    "attackCooldown" : 2.0,
    "tokenRate" : 5.0,
    "tokenTime" : 1.0,
    "attackHeightMul" : 1.0
  },
  "ai" : [ {
    "lowerBoundTime" : 1.0,
    "upperBoundTime" : 2.0,
    "probabilisticAi" : {
      "buildProbability" : 0.65,
      "upgradeProbability" : 0.155,
      "attackProbability" : 0.19,
      "sellProbability" : 0.005
    },
    "aiAimer" : {
      "playAStar" : 0.75,
      "maxAttempts" : 50,
      "tries" : 5
    }
  } ],
  "attackStat" : {
    "value" : {
      "baseValue" : 0.0,
      "mulLevel" : 1.0,
      "minValue" : 0.0,
      "maxValue" : 100.0,
      "bonus" : {
        "multiplier" : 0.0,
        "bonusTile" : null
      }
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "buildDistanceStat" : {
    "value" : {
      "baseValue" : 2.0,
      "mulLevel" : 0.0,
      "minValue" : 0.0,
      "maxValue" : 100.0,
      "bonus" : {
        "multiplier" : 0.0,
        "bonusTile" : null
      }
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "claimDistanceStat" : {
    "value" : {
      "baseValue" : 1.0,
      "mulLevel" : 0.0,
      "minValue" : 0.0,
      "maxValue" : 100.0,
      "bonus" : {
        "multiplier" : 0.0,
        "bonusTile" : null
      }
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "defenceStat" : {
    "value" : {
      "baseValue" : 0.0,
      "mulLevel" : 1.0,
      "minValue" : 0.0,
      "maxValue" : 100.0,
      "bonus" : {
        "multiplier" : 0.5,
        "bonusTile" : "MOUNTAIN"
      }
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "generationStat" : {
    "value" : {
      "baseValue" : -1.0,
      "mulLevel" : 1.0,
      "minValue" : 0.0,
      "maxValue" : 100.0,
      "bonus" : {
        "multiplier" : 0.3,
        "bonusTile" : "WATER"
      }
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  },
  "viewDistanceStat" : {
    "value" : {
      "baseValue" : 3.0,
      "mulLevel" : 0.0,
      "minValue" : 0.0,
      "maxValue" : 3.0,
      "bonus" : {
        "multiplier" : 0.0,
        "bonusTile" : null
      }
    },
    "cost" : {
      "selfLevelMultiplier" : 3.0
    }
  }
}
