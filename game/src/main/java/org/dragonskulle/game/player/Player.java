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

    // List of Buildings -- stored & synced in HexagonMap
    //    @Getter
    private final Map<HexagonTile, Reference<Building>> mOwnedBuildings = new HashMap<>();

    private final Map<Integer, Reference<Player>> mPlayersOnline = new TreeMap<>();

    @Getter public SyncInt mTokens = new SyncInt(0);
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

    private final int TOKEN_RATE = 5;
    private final float UPDATE_TIME = 1;
    private float mLastTokenUpdate = 0;

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
        mClientSellRequest = new ClientRequest<>(new SellData(), this::handleEvent);
        mClientAttackRequest = new ClientRequest<>(new AttackData(), this::handleEvent);
        mClientBuildRequest = new ClientRequest<>(new BuildData(), this::handleEvent);
        mClientStatRequest = new ClientRequest<>(new StatData(), this::handleEvent);

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
        updateTokens(UPDATE_TIME);
    }

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
     * This method will update the amount of tokens the user has per UPDATE_TIME. Goes through all
     * owned buildings to check if need to update tokens. Should only be ran on the server
     *
     * @param time The time since the last update
     */
    public void updateTokens(float time) {
        // Checks if server
        if (getNetworkObject().isServer()) {
            mLastTokenUpdate += time;
            // Checks to see how long its been since lastTokenUpdate
            if (mLastTokenUpdate >= UPDATE_TIME) {

                // Add tokens for each building
                mOwnedBuildings.values().stream()
                        .filter(Reference::isValid)
                        .map(Reference::get)
                        .forEach(
                                b ->
                                        mTokens.set(
                                                mTokens.get() + b.getTokenGeneration().getValue()));

                mTokens.set(mTokens.get() + TOKEN_RATE);
                mLastTokenUpdate = 0;
                log.info("Tokens at: " + mTokens.get());
            }
        }
    }


    @Override
    protected void onDestroy() {}

    // Selling of buildings is handled below
    

    /**
     * How this component will react to an sell event.
     *
     * @param data sell event being executed on the server.
     */
    public void handleEvent(SellData data) {
        // TODO implement
        // get building
        // verify the sender owns the building
        // remove from owned buildings
        // remove from map
        // reimburse player with tokens

        // TODO: Remove.
        Building building = data.getBuilding(getMap());
        log.info("Removing building.");
        if (building != null) {
            building.remove();
        }
    }

    // attacking of buildings is handled below
    

    /**
     * How this component will react to an attack event.
     *
     * @param data attack event being executed on the server.
     */
    public void handleEvent(AttackData data) {

        /* int COST = 5; // 	TODO MOVE TO BUILDING OR ATTACK.  BASICALLY A BETTER PLACE THAN THIS

        // Checks if there is enough tokens for this
        if (mTokens.get() < COST) {
            log.info("Do not have enough for attack");
            return;
        }

        // Get the hexagon tiles
        Building attackingBuilding = data.getAttackingFrom(null);
        Building defenderBuilding = data.getAttackingTo(null);

        if (attackingBuilding == null
                || defenderBuilding == null
                || attackingBuilding.getNetworkObject().getOwnerId()
                        != getNetworkObject().getOwnerId()) {
            log.info("Invalid building selection!");
            return;
        }

        if (!attackingBuilding.isBuildingAttackable(defenderBuilding)) {
            log.info("Player passed a non-attackable building!");
            return;
        }

        // Checks building is correct
        if (defenderBuilding.getOwnerID() == attackingBuilding.getOwnerID()) {
            log.info("ITS YOUR BUILDING DUMMY");
            return;
        }

        // ATTACK!!! (Sorry...)
        boolean won = attackingBuilding.attack(defenderBuilding);
        log.info("Attack is: " + won);
        mTokens.set(mTokens.get() - COST);

        // If you've won attack
        if (won) {
            Reference<Player> player = mPlayersOnline.get(defenderBuilding.getOwnerID());

            if (player != null && player.isValid()) {
                player.get().mOwnedBuildings.remove(defenderBuilding.getTile());
            } else {
                log.warning("Player not found!");
            }

            mOwnedBuildings.put(
                    defenderBuilding.getTile(), defenderBuilding.getReference(Building.class));
            defenderBuilding.getNetworkObject().setOwnerId(attackingBuilding.getOwnerID());
        }
        log.info("Done");

        return;*/
    }

    // Building is handled below
    

    /**
     * How this component will react to a Build event.
     *
     * @param data attack event being executed on the server.
     */
    public void handleEvent(BuildData data) {
        // TODO implement
        // get Hexagon to build on
        // Add to the HexagonMap
        // Take tokens off

        // TODO: Move to Building.
        int COST = 5;

        if (mTokens.get() < COST) {
            log.info("Not enough tokens for building");
            return;
        }

        // Gets the actual tile
        HexagonMap map = getMap();
        if(map == null) {
        	log.warning("Map is null.");
        	return;
        }
        
        HexagonTile tile = data.getTile(map);
        if(tile == null) {
        	log.warning("Tile from BuildData is null.");
        	return;
        }
        
        if(tile.isClaimed()) {
        	log.info("Tile already claimed.");
            return;
        }
        
        if(tile.hasBuilding()) {
        	log.info("Building already on tile.");
            return;
        }
        
        // Ensure the building is placed within a set radius of an owned building.
        final int radius = 3;
        ArrayList<HexagonTile> tiles = map.getTilesInRadius(tile, radius);
        
        if (containsOwnedBuilding(tiles) == false) {
            log.info("Building is placed too far away from preexisting buildings.");
            return;
        }

        Building addedNewBuilding = createBuilding(tile.getQ(), tile.getR());

        if (addedNewBuilding != null) {
            mTokens.set(mTokens.get() - COST);
            log.info("Building added");
        }
    }

    // Upgrading Stats is handled below
    

    /**
     * How this component will react to an upgrade event.
     *
     * @param data attack event being executed on the server.
     */
    public void handleEvent(StatData data) {
        // TODO implement
        // Get Building
        // Get Stat
        // Upgrade

        // TODO: Replace with actual logic.
        // Used for testing:
        HexagonMap map = getMap();
        Building building = data.getBuilding(map);
        if (building.getAttack().get() + 1 > SyncStat.LEVEL_MAX) {
            building.getAttack().setLevel(0);
        } else {
            building.getAttack().increaseLevel();
        }

        // Update the building on the server.
        building.afterStatChange();
    }

    @Override
    public void fixedUpdate(float deltaTime) {

        updateTokens(deltaTime);
    }
}
