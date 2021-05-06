# AI Tutorial

This guide will teach you how to add a probablistic AI into the game

## Steps

1) Create a new class within the aiPlayer package and give it a name ProbabilisticAiPlayer(Whatever).java.
2) Make this new class extend `ProbabilisticAiPlayer`.
3) Set the values of `mBuildProbability`, `mAttackProbability`, `mUpgradeProbability` and `mSellProbability` in `onStart`. Make sure they all sum to 1.
4) If you would like to change how often the AiPlayer works change `mLowerBoundTime` and `mUpperBoundTime` as well within `onStart()`.
5) At the end of `onStart()` run the command `super.onStart()`.  
6) Then you need to create a template.  This needs to be done in the blender GLTF thingy. Basically open network_templates.blend and create a new thing in Collection.  Give it a name.  Then add 2 components.  The Player Class and your AI Player.  If you're stuck tell Nat, I will don't mind helping!
7) Voila you have created a new AI player.  This can be used by the player to give them a different challenge
8) If it does not work tell Nat

## Variable Meanings
- `mBuildProbablity` - This is the chance of that the AI player will build a building.  The larger this is the less chance the AI player will try and upgrade, attack or sell.
- `mAttackProbability` - The chance the player will attack if it does not build
- `mUpgradeProbability` - The chance the player will upgrade one of their stats
- `mSellProbability` - The chance the player will sell one of their buildings
- `mLowerBoundTime` - The shortest amount of time the player has to wait between each of their moves
- `mUpperBoundTime` - The longest amount of time the player has to wait between each of their moves.  

## How AI works
This AI firstly needs to check whether they can play game.  This is done by creating a random time and then getting it in between `mLowerBoundTime` and `mUpperBoundTime` inclusive.  This is updated after every time the player plays.  After checking it then needs to decide what action to do.  Firstly it checks how many buildings it has.  If only one it creates a new Building (Why?  Because I chose to.).  If it has built it then needs to decide whether to build or to do something with a building.  If building it chooses a legal tile and tries to build.  If something with a building it then needs to decide whether to attack, upgrade a Stat or Sell a building and then from their tries that action.

Any questions please ask Nat 

## Current Numbers
Here are what all the variables which you need to worry about are set at 

- **BaseAI**
	- `mLowerBoundTime` = 1
	- `mUpperBoundTime` = 2
- **ProbabilisticAiPlayer** 
	- `mBuildingProbability` = 0.65
	- `mAttackProbability` = 0.15
	- `mUpgradeProbability` = 0.19
	- `mSellProbability` = 0.01