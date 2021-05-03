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