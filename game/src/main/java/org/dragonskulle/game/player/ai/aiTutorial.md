# AI Tutorial

This guide will teach you how to add a probablistic AI into the game

## Steps

1) Create a new class within the aiPlayer package
2) Make this new class extend ``ProbablisticAiPlayer``
3) In your class set the class variable ``mTileProbability`` between any value between 0 & 1.  (Cast it to a float as well) -- This needs to be done in ``public void onStart()``
4) Set the values of ``mAttackBuilding``, ``mUpgradeBuilding`` & ``mSellBuilding``.  Make sure they all sum to 1. (They all need to be floats).  Do this isn ``public void onStart()``

5) If you would like to change how often the AiPlayer works change ``mLowerBoundTime`` and ``mUpperBoundTime`` as well withing ``onStart()``.
6) At the end of ``onStart()`` run the command ``super.onStart``.  

7) If it does not work tell Nat