#Serverless API
This is for keeping track of available non LAN hosts

##Endpoints

GET /api/hosts - gets all hosts

PUSH /api/hosts - creates a new entry, must be in format 
```json
{
    "address": "255.255.255.255",
    "port": 1337
}
```

DELETE /api/hosts/{id} - deletes entry

GET /api/hosts/{id} - retrieves a specific entry

GET /api/hosts/code/{code} - gets host by code

DELETE /api/hosts/code/{code} - deletes host by code

GET /api/hosts/config - gets the current config values

POST /api/config - sets the values
You must also provide `token` in the headers with the API Key.
```json
{
  "SyncStat": {
    "LEVEL_MIN": 1,
    "LEVEL_MAX": 10
  },
  "Player": {
    "ATTACK_COOLDOWN": 2,
    "TOKEN_RATE": 5,
    "TOKEN_TIME": 1
  },
  "ProbabilisticAiPlayer": {
    "mBuildProbability": 0.65,
    "mUpgradeProbability": 0.15,
    "mAttackProbability": 0.15,
    "mSellProbability": 0.05
  },
  "AiAimer": {
    "PLAY_A_STAR": 0.9,
    "AIM_AT_CAPITAL": 0.01,
    "TRIES": 10
  }
}
```
DELETE /api/hosts/code/{code} - deletes host by code
