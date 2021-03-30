/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.map.MapEffects.HighlightSelection;
import org.dragonskulle.game.player.networkData.AttackData;
import org.dragonskulle.game.player.networkData.BuildData;
import org.dragonskulle.game.player.networkData.SellData;
import org.dragonskulle.game.player.networkData.StatData;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.sync.SyncInt;
import org.dragonskulle.network.components.sync.SyncVector3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * This is the class which contains all the needed data to play a game
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class Player extends NetworkableComponent implements IOnStart, IFixedUpdate {

    /** A list of {@link Building}s owned by the player. */
    private final Map<HexagonTile, Reference<Building>> mOwnedBuildings = new HashMap<>();

    private final Map<Integer, Reference<Player>> mPlayersOnline = new TreeMap<>();

    /** The number of tokens the player has, synchronised from server to client. */
    @Getter public SyncInt mTokens = new SyncInt(0);
    
    /** The colour of the player. */
    @Getter public final SyncVector3 mPlayerColour = new SyncVector3();
    @Getter private HighlightSelection mPlayerHighlightSelection;

    private static final Vector3f[] COLOURS = {
        new Vector3f(0.5f, 1f, 0.05f),
        new Vector3f(0.05f, 1f, 0.86f),
        new Vector3f(0.89f, 0.05f, 1f),
        new Vector3f(0.1f, 0.56f, 0.05f),
        new Vector3f(0.05f, 1f, 0.34f),
        new Vector3f(0.05f, 1f, 0.34f),
        new Vector3f(0.1f, 0.05f, 0.56f),
        new Vector3f(0f, 0f, 0f)
    };

    /** The base rate of tokens which will always be added. */
    private final int TOKEN_RATE = 5;
    /** How frequently the tokens should be added. */
    private final float TOKEN_TIME = 1f;
    /** The total amount of time passed since the last time tokens where added. */
    private float mCumulativeTokenTime = 0f;

    // TODO this needs to be set dynamically -- specifies how many players will play this game
    private final int playersToPlay = 6;
    
    /** Used by the client to request that a building be placed by the server. */
    @Getter private transient ClientRequest<BuildData> mClientBuildRequest;
    /** Used by the client to request that a building attack another building. */
    @Getter private transient ClientRequest<AttackData> mClientAttackRequest;
    /** Used by the client to request that a building's stats be increased. */
    @Getter private transient ClientRequest<StatData> mClientStatRequest;
    /** Used by the client to request that a building be sold. */
    @Getter private transient ClientRequest<SellData> mClientSellRequest;
    
    /** The base constructor for player */
    public Player() {}

    @Override
    protected void onConnectedSyncvars() {
        if (getNetworkObject().isServer()) {
            int id = getNetworkObject().getOwnerId() % COLOURS.length;
            if (id < 0) id += COLOURS.length;
            mPlayerColour.set(COLOURS[id]);
        }
    }

    /** We need to initialise client requests here, since java does not like to serialise lambdas. */
    @Override
    protected void onNetworkInitialize() {
    	mClientBuildRequest = new ClientRequest<>(new BuildData(), this::buildEvent);
    	mClientAttackRequest = new ClientRequest<>(new AttackData(), this::attackEvent);
    	mClientStatRequest = new ClientRequest<>(new StatData(), this::statEvent);
    	mClientSellRequest = new ClientRequest<>(new SellData(), this::sellEvent);
        
        if (getNetworkObject().isMine()) Scene.getActiveScene().registerSingleton(this);
    }
    
    @Override
    public void onStart() {

    	if(getNetworkObject() == null) {
    		log.severe("Player has no NetworkObject.");
    	}
    	
    	if(getNetworkManager() == null) {
    		log.severe("Player has no NetworkManager.");
    	}
    	
    	if(getMap() == null) {
    		log.severe("Player has no HexagonMap.");
    	}
    	
        if (getNetworkObject().isServer()) {
            distributeCoordinates();
        }

        Vector3fc col = mPlayerColour.get();
        mPlayerHighlightSelection =
                MapEffects.highlightSelectionFromColour(col.x(), col.y(), col.z());

        // mOwnedBuildings.add(capital);
        // TODO Get all Players & add to list
        updateTokens(TOKEN_TIME);
    }

    @Override
    public void fixedUpdate(float deltaTime) {
    	// Update the token count.
        updateTokens(deltaTime);
    }
    
    @Override
    protected void onDestroy() {}
    
    /**
     * This will randomly place a capital using an angle so each person is within their own slice
     */
    private void distributeCoordinates() {

        float angleOfCircle = (float) 360 / (float) playersToPlay;

        // This says how many people have joined the server so far
        int playersOnlineNow = mPlayersOnline.size();

        // This gives us the angle to find our coordinates
        float angleToStart = playersOnlineNow * angleOfCircle;
        float angleToEnd = (playersOnlineNow + 1) * angleOfCircle;

        /* TODO NOTES
        Create a line from each of these angles.  Use Maths
        Then choose a pair of coordinates from between those lines
        Change to Axial
        Done????
        */
        // boolean finished;

        int min = -10;
        int max = 10;

        int posX = min + (int) (Math.random() * ((max - min) + 1));
        int posY = min + (int) (Math.random() * ((max - min) + 1));
        // HexagonTile toBuild = mMapComponent.get().getTile(posX, posY);
        Building buildingToBecomeCapital = createBuilding(posX, posY);
        if (buildingToBecomeCapital == null) {
            log.severe("Unable to place an initial capital building.");
            return;
        }
        buildingToBecomeCapital.setCapital(true);
    }

    /**
     * This method will update the amount of tokens the user has per {@link #TOKEN_TIME}. Goes through all
     * owned {@link Building}s to check if need to update tokens. Should only be ran on the server.
     *
     * @param time The time since the last update.
     */
    private void updateTokens(float time) {
        // Only the server should add tokens.
        if (getNetworkObject().isServer()) {
            // Increase the total amount of time since tokens where last added.
        	mCumulativeTokenTime += time;
            
            // Check to see if enough time has passed.
            if (mCumulativeTokenTime >= TOKEN_TIME) {

            	// Add tokens for each building.
            	getOwnedBuildingsAsStream()
                        .filter(Reference::isValid)
                        .map(Reference::get)
                        .forEach(
                                building ->
                                        mTokens.add(building.getTokenGeneration().getValue()));

            	// Add a base amount of tokens.
                mTokens.add(TOKEN_RATE);
                
                // Reduce the cumulative time by the TOKEN_TIME.
                mCumulativeTokenTime -= TOKEN_TIME;
                
                log.info("Tokens at: " + mTokens.get());
            }
        }
    }
    
    /**
     * This will create a new {@link GameObject} with a {@link Building} component. 
     *
     * @param qPos The q position of the building.
     * @param rPos The r position of the building.
     * @return {@code true} a new building is created, otherwise {@code false}.
     */
    private Building createBuilding(int qPos, int rPos) {

        if (getNetworkManager().getServerManager() == null) {
            log.warning("Unable to create building: Server manager is null.");
            return null;
        }

        HexagonMap map = getMap();
        if(map == null) {
        	log.warning("Unable to create building: no HexagonMap.");
            return null;
        }
        
        // Get the HexagonTile.
        HexagonTile tile = map.getTile(qPos, rPos);
        if (tile == null) {
            log.warning("Unable to create building: Tile does not exist.");
            return null;
        }
        
        if(tile.isClaimed()) {
        	log.warning("Unable to create building: Tile is already claimed by a Building.");
            return null;
        }
        
        if (tile.hasBuilding()) {
            log.warning("Unable to create building: Tile already has Building.");
            return null;
        }

        int playerId = getNetworkObject().getOwnerId(); //TODO: Change to getID?
        int template = getNetworkManager().findTemplateByName("building");
        Reference<NetworkObject> networkObject = getNetworkManager().getServerManager().spawnNetworkObject(playerId, template);

        if (networkObject == null || networkObject.isValid() == false) {
            log.warning("Unable to create building: Could not create a Network Object.");
            return null;
        }

        GameObject gameObject = networkObject.get().getGameObject();
        gameObject.getTransform(TransformHex.class).setPosition(qPos, rPos);
        Reference<Building> building = gameObject.getComponent(Building.class);

        if (building == null || building.isValid() == false) {
            log.warning("Unable to create building: Reference to Building component is invalid.");
            return null;
        }

        return building.get();
    }

    /**
     * Add a {@link Building} the the list of owned buildings.
     *
     * @param building The building to add to {@link #mOwnedBuildings}.
     */
    public void addOwnership(Building building) {
    	if (building == null) return;

        // Get the tile the building is on.
        HexagonTile tile = building.getTile();

        if(tile == null) {
        	log.warning("Unable to add Building to list of owned tiles as the Building's HexagonTile is null.");
        	return;
        }
        
        // Add the building at the relevant position.
        mOwnedBuildings.put(tile, building.getReference(Building.class));
    }
    
    /**
     * Remove a {@link Building} from the {@link List} of owned buildings.
     * 
     * @param building The {@link Building} to remove.
     * @return {@code true} on success, otherwise {@code false}.
     */
    public boolean removeOwnership(Building building) {
        if(building == null) return false;
    	
        HexagonTile tile = building.getTile();
        if(tile == null) return false;
    	
        Reference<Building> removed = mOwnedBuildings.remove(tile);
        return (removed != null); 
    }
    
    /**
     * Get the {@link Building}, that is on the specified {@link HexagonTile}, from {@link #mOwnedBuildings}
     *
     * @param tile The tile to get the building from.
     * @return The reference to the building, if it is in your {@link #mOwnedBuildings}, otherwise {@code null}.
     */
    public Reference<Building> getOwnedBuilding(HexagonTile tile) {
        return mOwnedBuildings.get(tile);
    }

    /**
     * This will return a {@link Stream} of {@link Reference}s to {@link Building}s which are owned by the player.
     *
     * @return A stream containing references to the buildings the player owns.
     */
    public Stream<Reference<Building>> getOwnedBuildingsAsStream() {
        return mOwnedBuildings.values().stream();
    }
    
    /**
     * The number of {@link Building}s the player owns (the number of buildings in {@link #mOwnedBuildings}).
     *
     * @return The number of buildings the player currently owns.
     */
    public int numberOfOwnedBuildings() {
        return mOwnedBuildings.size();
    }
    
    /**
     * Check if the {@link Building}'s owner is the player.
     * 
     * @param building The building to check.
     * @return {@code true} if the player owns the building, otherwise {@code false}.
     */
    public boolean checkBuildingOwnership(Building building) {
    	return building.getOwnerID() == getNetworkObject().getOwnerId();
    }
    
    /**
     * Gets the {@link HexagonMap} by iterating though all active scenes.
     *
     * @return The HexagonMap being used, otherwise {@code null}.
     */
    public HexagonMap getMap() {        
        // The currently active scene may not contain the hexagon map, so check all active scenes.
        ArrayList<Scene> scenes = Engine.getInstance().getActiveScenes();
        for (Scene scene : scenes) {
			HexagonMap map = scene.getSingleton(HexagonMap.class);
			if(map != null) {
				return map;
			}
		}
        
        return null;
    }
    
    /**
     * Check whether a list of {@link HexagonTile}s contains a {@link Building} owned by the player.
     * 
     * @param tiles The tiles to check.
     * @return {@code true} if the list of tiles contains at least one building that the player owns; otherwise {@code false}.
     */
    public boolean containsOwnedBuilding(ArrayList<HexagonTile> tiles) {
    	for (HexagonTile tile : tiles) {
			if(tile.hasBuilding() && checkBuildingOwnership(tile.getBuilding())) {
				return true;
			}
		}
    	return false;
    }
    
    /**
     * Process and parse an event in which the <b>client</b> player wishes to place a {@link Building}.
     * <p>
     * Players that run on the <b>server</b> do not need to do this- they can simply run {@link #buildAttempt(HexagonTile)}.
     *
     * @param data The {@link BuildData} sent by the client.
     */
    void buildEvent(BuildData data) {
    	
    	HexagonMap map = getMap();
        if(map == null) {
        	log.warning("Unable to parse BuildData: Map is null.");
        	return;
        }
    	
    	HexagonTile tile = data.getTile(map);
        if(tile == null) {
        	log.warning("Unable to parse BuildData: Tile from BuildData is null.");
        	return;
        }
        
    	// Try to place the building on the tile.
    	buildAttempt(tile);
    }
    
    /**
     * Attempt to place a building on a specific tile.
     * <p>
     * This first checks the tile to make sure it is fully eligible before any placement happens.
     * 
     * @param tile The tile to place a building on.
     * @return Whether the attempt to build was successful.
     */
    public boolean buildAttempt(HexagonTile tile) {
    	if(buildCheck(tile) == false) {
    		log.info("Unable to pass build check.");
    		return false;
    	}
    	
    	Building building = createBuilding(tile.getQ(), tile.getR());

    	if(building == null) {
    		log.info("Unable to add building.");
    		return false;
    	}
    	
    	// Subtract the cost.
    	mTokens.add(-Building.BUY_PRICE);

    	log.info("Added building.");
    	return true;
    }
    
    /**
     * Ensure that the {@link HexagonTile} is eligible to have a {@link Building} placed on it.
     * 
     * @param tile The tile to put a building on.
     * @return {@code true} if the tile is eligible, otherwise {@code false}.
     */
    public boolean buildCheck(HexagonTile tile) {

    	if(tile == null) {
        	log.warning("Tile is null.");
        	return false;
        }
    	
        if (mTokens.get() < Building.BUY_PRICE) {
            log.info("Not enough tokens to buy building.");
            return false;
        }
        
        HexagonMap map = getMap();
        if(map == null) {
        	log.warning("Map is null.");
        	return false;
        }
        
        if(tile.isClaimed()) {
        	log.info("Tile already claimed.");
            return false;
        }
        
        if(tile.hasBuilding()) {
        	log.info("Building already on tile.");
            return false;
        }
        
        // Ensure the building is placed within a set radius of an owned building.
        final int radius = 3;
        ArrayList<HexagonTile> tiles = map.getTilesInRadius(tile, radius);
        
        if (containsOwnedBuilding(tiles) == false) {
            log.info("Building is placed too far away from preexisting buildings.");
            return false;
        }
        
    	return true;
    }
    
    /**
     * Process and parse an event in which the <b>client</b> player wishes to attack a {@link Building} from another Building.
     * <p>
     * Players that run on the <b>server</b> do not need to do this- they can simply run {@link #attackAttempt(Building, Building)}.
     *
     * @param data The {@link AttackData} sent by the client.
     */
    void attackEvent(AttackData data) {
    	
    	HexagonMap map = getMap();
        if(map == null) {
        	log.warning("Unable to parse AttackData: Map is null.");
        	return;
        }
    	
    	Building attacker = data.getAttacker(map);        
    	if(attacker == null) {
        	log.warning("Unable to parse AttackData: attacking building is null.");
        	return;
        }
    	
    	Building defender = data.getDefender(map);
    	if(defender == null) {
        	log.warning("Unable to parse AttackData: defending building is null.");
        	return;
        }
    	
    	// Try to run an attack.
    	attackAttempt(attacker, defender);
    }
    
    /**
     * Attempt to attack an opponent {@link Building} from a player owned Building.
     * <p>
     * This first checks the buildings can attack each other before any attack happens.
     * 
     * @param attacker The attacking building.
     * @param defender The defending building.
     * @return Whether the attempt to attack was successful.
     */
    public boolean attackAttempt(Building attacker, Building defender) {
    	if(attackCheck(attacker, defender) == false) {
    		log.info("Unable to pass attack check.");
    		return false;
    	}

    	// TODO: Write actual attack logic.
    	log.info("ATTACK HERE.");
    	
    	return true;
    }
    
    /**
     * Ensure that the attacker and defender are valid and can actually attack each other.
     * 
     * @param attacker The attacking building.
     * @param defender The defending building.
     * @return {@code true} if the attack is eligible, otherwise {@code false}.
     */
    public boolean attackCheck(Building attacker, Building defender) {

    	if(attacker == null) {
    		log.info("Attacker is null.");
    		return false;
    	}
    	
    	if(defender == null) {
    		log.info("Defender is null.");
    		return false;
    	}
    	
    	// TODO: Write all checks.
    	
    	return true;
    }
    
    /**
     * Process and parse an event in which the <b>client</b> player wishes to sell a {@link Building}.
     * <p>
     * Players that run on the <b>server</b> do not need to do this- they can simply run {@link #sellAttempt(Building)}.
     *
     * @param data The {@link SellData} sent by the client.
     */
    void sellEvent(SellData data) {
    	
    	HexagonMap map = getMap();
        if(map == null) {
        	log.warning("Unable to parse StatData: Map is null.");
        	return;
        }
    	
        Building building = data.getBuilding(map);
        if(building == null) {
        	log.warning("Unable to parse StatData: Building from StatData is null.");
        	return;
        }
        
    	// Try to sell the building on the tile.
    	sellAttempt(building);
    }
    
    /**
     * Attempt to sell a specified building.
     * <p>
     * This first checks the building is fully eligible before any selling happens.
     * 
     * @param building The building to sell.
     * @return Whether the attempt to sell the building was successful.
     */
    public boolean sellAttempt(Building building) {
    	if(sellCheck(building) == false) {
    		log.info("Unable to pass sell check.");
    		return false;
    	}
    	
    	// TODO: Add sell logic.
    	log.info("SELL HERE.");
    	return true;
    }
    
    /**
     * Ensure that the {@link Building} is eligible to be sold.
     * 
     * @param building The building to sell.
     * @return {@code true} if the building can be sold, otherwise {@code false}.
     */
    public boolean sellCheck(Building building) {

    	if(building == null) {
        	log.warning("building is null.");
        	return false;
        }
    	
        // TODO: Write all checks.
        
    	return true;
    }    

    /**
     * Process and parse an event in which the <b>client</b> player wishes to increase the stats of a {@link Building}.
     * <p>
     * Players that run on the <b>server</b> do not need to do this- they can simply run {@link #statAttempt(Building)}.
     *
     * @param data The {@link StatData} sent by the client.
     */
    void statEvent(StatData data) {
    	
    	HexagonMap map = getMap();
        if(map == null) {
        	log.warning("Unable to parse StatData: Map is null.");
        	return;
        }
    	
        Building building = data.getBuilding(map);
        if(building == null) {
        	log.warning("Unable to parse StatData: Building from StatData is null.");
        	return;
        }
        
    	// Try to change the stats of the building.
        statAttempt(building, null); // TODO: Pass through the stat to be changed.
    }
    
    /**
     * Attempt to increase a specific stat of a {@link Building}.
     * <p>
     * This first checks the building and stat to make sure that they are fully eligible before any stat changes happen.
     * 
     * @param building The building whose stats will be changed.
     * @param stat The stat to increase.
     * @return Whether the attempt to change the stats where successful.
     */
    public boolean statAttempt(Building building, SyncStat<?> stat) {
    	if(statCheck(building) == false) {
    		log.info("Unable to pass stat check.");
    		return false;
    	}
    	
    	// TODO: Add stat increase logic.
    	log.info("INCREASE SPECIFIC STAT HERE.");
    	
    	// Update the building on the server.
        building.afterStatChange();
    	
    	return true;
    }
    
    /**
     * Ensure that the {@link HexagonTile} is eligible to have a {@link Building} placed on it.
     * 
     * @param building The building to have its stats increased.
     * @return {@code true} if the tile is eligible, otherwise {@code false}.
     */
    public boolean statCheck(Building building) {
    	
    	if(building == null) {
        	log.warning("Building is null.");
        	return false;
        }
    	
    	// TODO: Write all checks.
    	// TODO: Also check the desired stat.
        
    	return true;
    }
    
}
