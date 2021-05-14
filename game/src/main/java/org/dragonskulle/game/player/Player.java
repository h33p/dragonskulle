/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import static org.dragonskulle.game.GameUIAppearance.AudioFiles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.futures.AwaitFuture;
import org.dragonskulle.game.GameConfig;
import org.dragonskulle.game.GameConfig.PlayerConfig;
import org.dragonskulle.game.GameState;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.map.MapEffects.StandardHighlightType;
import org.dragonskulle.game.player.network_data.AttackData;
import org.dragonskulle.game.player.network_data.BuildData;
import org.dragonskulle.game.player.network_data.SellData;
import org.dragonskulle.game.player.network_data.StatData;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.requests.ServerEvent;
import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncFloat;
import org.dragonskulle.network.components.sync.SyncInt;
import org.dragonskulle.network.components.sync.SyncVector3;
import org.dragonskulle.utils.MathUtils;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/**
 * This is the class which contains all the needed data to play a game.
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class Player extends NetworkableComponent implements IOnStart, IFixedUpdate {

    /** A list of {@link Building}s owned by the player. */
    private final Map<HexagonTile, Reference<Building>> mOwnedBuildings = new HashMap<>();

    /** Extra deflation bonus for when capitals are captured. */
    private SyncFloat mBuildingInflationBonus = new SyncFloat(0);

    /**
     * A set tiles around the player.
     *
     * <p>Positive values indicate the tile is viewable, 0, or lower mean tile is not viewable by
     * the player.
     */
    private final Map<HexagonTile, Integer> mTilesAround = new HashMap<>();

    /** Link to the current capital. */
    private Reference<Building> mCapital = null;

    /** The number of tokens the player has, synchronised from server to client. */
    @Getter private SyncInt mTokens = new SyncInt(0);

    /** The colour of the player. */
    @Getter private final SyncVector3 mPlayerColour = new SyncVector3();

    @Getter private Vector4f mPlayerHighlightSelection;

    /** Store a reference to the {@link MapEffects} component. */
    private Reference<MapEffects> mMapEffects;

    /** Reference to the HexagonMap being used by the Player. */
    private Reference<HexagonMap> mMap = null;
    /** Reference to the GameState on the connection. */
    private Reference<GameState> mGameState = null;

    /** Whether they own the building. */
    private SyncBool mOwnsCapital = new SyncBool(true);

    /** Player configuration values used for timeout configuration. */
    private PlayerConfig mConfig;

    /** When the last time a player attacked. */
    private final SyncFloat mLastAttack = new SyncFloat(-10f);

    /** The total amount of time passed since the last time tokens where added. */
    private float mCumulativeTokenTime = 0f;

    /** Controls how deep into unviewable tiles we go for mTilesAround. */
    private static final int VIEWABILITY_LOWER_BOUND = -5;

    /** Used by the client to request that a building be placed by the server. */
    @Getter private transient ClientRequest<BuildData> mClientBuildRequest;
    /** Used by the client to request that a building attack another building. */
    @Getter private transient ClientRequest<AttackData> mClientAttackRequest;
    /** Used by the client to request that a building's stats be increased. */
    @Getter private transient ClientRequest<StatData> mClientStatRequest;
    /** Used by the client to request that a building be sold. */
    @Getter private transient ClientRequest<SellData> mClientSellRequest;
    /** Sent by the server to tell clients that an attack is happening. */
    @Getter private transient ServerEvent<AttackData> mServerAttackEvent;

    @Getter private transient ServerEvent<GameState.InvokeAudioEvent> mOwnerAudioEvent;
    @Getter private transient ServerEvent<GameState.InvokeAudioEvent> mGlobalAudioEvent;
    private float mAttackDuration = 1.5f;
    private Reference<AudioSource> mJukeBox;

    /** The base constructor for player. */
    public Player() {}

    @Override
    protected void onConnectedSyncvars() {
        if (getNetworkObject().isServer()) {
            int id = getNetworkObject().getOwnerId();
            mPlayerColour.set(PlayerColour.getColour(id));
        }
    }

    /**
     * We need to initialise client requests here, since java does not like to serialise lambdas.
     */
    @Override
    protected void onNetworkInitialise() {
        mClientBuildRequest = new ClientRequest<>(new BuildData(), this::buildRequest);
        mClientAttackRequest = new ClientRequest<>(new AttackData(), this::attackRequest);
        mClientStatRequest = new ClientRequest<>(new StatData(), this::statRequest);
        mClientSellRequest = new ClientRequest<>(new SellData(), this::sellRequest);

        mServerAttackEvent = new ServerEvent<>(new AttackData(), this::attackEffect);

        mOwnerAudioEvent =
                new ServerEvent<>(
                        new GameState.InvokeAudioEvent(),
                        this::triggerAudioEvent,
                        ServerEvent.EventRecipients.OWNER,
                        ServerEvent.EventTimeframe.INSTANT);

        mGlobalAudioEvent =
                new ServerEvent<>(
                        new GameState.InvokeAudioEvent(),
                        this::triggerAudioEvent,
                        ServerEvent.EventRecipients.ALL_CLIENTS,
                        ServerEvent.EventTimeframe.INSTANT);

        getNetworkManager().getIdSingletons(getNetworkObject().getOwnerId()).register(this);

        if (getNetworkObject().isMine()) Scene.getActiveScene().registerSingleton(this);
    }

    /**
     * Trigger audio event by creating an audio source for the event.
     *
     * @param data the audio information to be played
     */
    private void triggerAudioEvent(GameState.InvokeAudioEvent data) {
        if (data != null && Reference.isValid(mJukeBox)) {
            mJukeBox.get().playSound(data.getSoundId().getPath());
        }
    }

    @Override
    public void onStart() {

        if (getNetworkObject() == null) log.severe("Player has no NetworkObject.");

        if (getNetworkManager() == null) log.severe("Player has no NetworkManager.");

        if (!assignMap()) log.severe("Player has no HexagonMap.");

        mGameState = Scene.getActiveScene().getSingletonRef(GameState.class);

        if (getNetworkObject().isServer()) distributeCoordinates();

        Vector3fc col = mPlayerColour.get();
        mPlayerHighlightSelection =
                MapEffects.highlightSelectionFromColour(col.x(), col.y(), col.z());
        PlayerConfig cfg = getConfig();

        AudioSource source = new AudioSource();
        getGameObject().addComponent(source);
        mJukeBox = source.getReference(AudioSource.class);

        // TODO Get all Players & add to list
        updateTokens(cfg != null ? cfg.getTokenTime() : 1);
    }

    /** Used as a queue for tiles to be flood filled. */
    private final Deque<HexagonTile> mFillTiles = new ArrayDeque<>();

    /**
     * Adds building's viewable tiles to player's viewable tile list.
     *
     * @param building The building whose visible tiles should be made viewable.
     */
    public void updateViewableTiles(Building building) {
        for (HexagonTile tile : building.getClaimedTiles()) {
            mFillTiles.push(tile);
            mTilesAround.put(tile, building.getViewDistance().getValue());
        }
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        // Update the token count.
        if (!gameEnd()) updateTokens(deltaTime);
    }

    @Override
    protected void onDestroy() {}

    /**
     * Gets the game state component if it exists, otherwise gets the singleton reference from the
     * active scene. If there is no singleton registered, then it will return {@code null}.
     *
     * @return the game state
     */
    private GameState getGameState() {
        if (Reference.isValid(mGameState)) {
            return mGameState.get();
        }

        Scene activeScene = Scene.getActiveScene();

        if (activeScene == null) {
            return null;
        }

        mGameState = activeScene.getSingletonRef(GameState.class);

        if (!Reference.isValid(mGameState)) {
            return null;
        }

        return mGameState.get();
    }

    /**
     * Gets the player config from the game state. If no {@link GameState} exists then it will
     * return {@code null}.
     *
     * @return the config
     */
    public PlayerConfig getConfig() {
        if (mConfig != null) {
            return mConfig;
        }

        GameState state = getGameState();

        if (state == null) {
            return new GameConfig().getPlayer();
        }

        mConfig = state.getConfig().getPlayer();

        return mConfig;
    }

    /**
     * This will randomly place a capital using an angle so each person is within their own slice.
     */
    private void distributeCoordinates() {

        final int attempts = 20;

        final int maxPlayers;

        if (Reference.isValid(mGameState)) {
            GameState state = mGameState.get();
            maxPlayers = state.getNumPlayers().get();
        } else {
            maxPlayers = 6;
            log.severe("Failed to get max players from game state!");
        }

        for (int i = 0; i <= attempts; i++) {

            log.fine("This is attempt number " + i);

            // Add the building
            float angleBetween;
            if (i < attempts / 2) {
                float angleOfCircle = 360f / (maxPlayers + 1);
                angleBetween = (360 - (angleOfCircle * maxPlayers)) / maxPlayers;
            } else {
                angleBetween = 0;
            }
            Vector2f axial = createCoordinates(angleBetween);
            if (axial == null) {
                continue;
            }
            int x = (int) axial.x;
            int y = (int) axial.y;
            Building buildingToBecomeCapital = createBuilding(x, y, true);

            if (buildingToBecomeCapital == null) {
                log.severe("Unable to place an initial capital building.  X = " + x + " Y = " + y);
                continue;

            } else {
                buildingToBecomeCapital.setCapital(true);
                mGameState.get().getNumCapitalsStanding().add(1);
                return;
            }
        }

        List<HexagonTile> buildable =
                getMap().getAllTiles().filter(this::isBuildable).collect(Collectors.toList());
        if (buildable.isEmpty()) {
            // Cannot add a capital
            setOwnsCapital(false);

        } else {
            Random random = new Random();
            HexagonTile selectedTile = buildable.get(random.nextInt(buildable.size()));
            Building capital = createBuilding(selectedTile.getQ(), selectedTile.getR(), true);
            if (capital == null) {
                setOwnsCapital(false);
                return;
            }
            capital.setCapital(true);

            mGameState.get().getNumCapitalsStanding().add(1);
        }
    }

    /**
     * Determines if for a given player if this tile is buildable upon.
     *
     * @param tile The {@link HexagonTile} to check for
     * @return true if buildable, false otherwise
     */
    private boolean isBuildable(HexagonTile tile) {

        if (getMap() == null) {
            log.fine("map is null.");
            return false;
        }

        if (tile.isClaimed()) {
            log.fine("Tile already claimed.");
            return false;
        }

        if (tile.hasBuilding()) {
            log.fine("Building already on tile.");
            return false;
        }

        return true;
    }

    /**
     * Will create the coordinates to test.
     *
     * @param angleBetween The angle to add which states how far away a player should be
     * @return A {@link Vector2f} with the coordinates to use
     */
    private Vector2f createCoordinates(float angleBetween) {

        if (!Reference.isValid(mGameState)) {
            log.fine("Game State does not exist");
            return null;
        }

        final int maxPlayers = mGameState.get().getNumPlayers().get();

        float angleOfCircle = 360f / (maxPlayers + 1);

        // The number of players online
        int playersOnlineNow = getNetworkObject().getOwnerId() % maxPlayers;
        if (playersOnlineNow < 0) {
            playersOnlineNow += maxPlayers; // handle AI Players
        }

        // This gives us the angle to find our coordinates.  Stored in degrees
        float angleToStart = playersOnlineNow * (angleOfCircle + angleBetween);
        float angleToEnd = ((playersOnlineNow + 1) * (angleOfCircle + angleBetween)) - angleBetween;

        Random random = new Random();

        // Creates the vector coordinates to use
        float angle = random.nextFloat() * (angleToEnd - angleToStart) + angleToStart;
        Matrix2f rotation = new Matrix2f().rotate(angle * MathUtils.DEG_TO_RAD);
        Vector2f direction = new Vector2f(0f, 1f).mul(rotation);

        // Make sure the capital is not spawned over outside the circle
        float radius = (getMap().getSize() / 2);
        radius = (float) Math.sqrt(radius * radius * 0.75f);

        // Make sure the capital is not spawned near the centre
        int minDistance = 10;
        float distance = random.nextFloat() * (radius - minDistance) + minDistance;

        direction.mul(distance).mul(TransformHex.HEX_WIDTH);

        // Convert to Axial coordinates
        Vector3f cartesian = new Vector3f(direction.x, direction.y, 0f);
        Vector2f axial = new Vector2f();
        TransformHex.cartesianToAxial(cartesian, axial);

        return axial;
    }

    /**
     * This method will update the amount of tokens the user has at the rate defined in {@link
     * PlayerConfig#getPlayer()}. Goes through all owned {@link Building}s to check if need to
     * update tokens. Should only be ran on the server.
     *
     * @param time The time since the last update.
     */
    private void updateTokens(float time) {

        PlayerConfig cfg = getConfig();
        float tokenTime = cfg.getTokenTime();
        float tokenRate = cfg.getTokenRate();

        // Only the server should add tokens.
        if (getNetworkObject().isServer()) {
            // Increase the total amount of time since tokens where last added.
            mCumulativeTokenTime += time;

            // Check to see if enough time has passed.
            if (mCumulativeTokenTime >= tokenTime) {

                // Add tokens for each building.
                getOwnedBuildingsAsStream()
                        .filter(Reference::isValid)
                        .map(Reference::get)
                        .forEach(building -> mTokens.add(building.getTokenGeneration().getValue()));

                // Add a base amount of tokens.
                mTokens.add(Math.round(tokenRate));

                // Reduce the cumulative time by the TOKEN_TIME.
                mCumulativeTokenTime -= tokenTime;
            }
        }
    }

    /**
     * This will create a new {@link GameObject} with a {@link Building} component.
     *
     * @param qPos The q position of the building.
     * @param rPos The r position of the building.
     * @param checkIsland {@code true} when we don't want to place on an Island otherwise {@code
     *     false}
     * @return {@code true} a new building is created, otherwise {@code false}.
     */
    private Building createBuilding(int qPos, int rPos, boolean checkIsland) {

        if (getNetworkManager().getServerManager() == null) {
            log.warning("Unable to create building: Server manager is null.");
            return null;
        }

        HexagonMap map = getMap();
        if (map == null) {
            log.fine("Unable to create building: no HexagonMap.");
            return null;
        }

        // Get the HexagonTile.
        HexagonTile tile = map.getTile(qPos, rPos);
        if (tile == null) {
            log.fine("Unable to create building: Tile does not exist.");
            return null;
        }

        if (tile.isClaimed()) {
            log.fine("Unable to create building: Tile is already claimed by a Building.");
            return null;
        }

        if (tile.hasBuilding()) {
            log.fine("Unable to create building: Tile already has Building.");
            return null;
        }

        if (tile.getTileType() != TileType.LAND) {
            log.fine("Unable to create Building: Tile placed is not land");
            return null;
        }

        if (checkIsland) {
            if (getMap().isIsland(getMap().getTile(qPos, rPos))) {
                log.fine("This is an island and a capital cannot be placed here");
                return null;
            }
        }

        int playerId = getNetworkObject().getOwnerId();
        int template = getNetworkManager().findTemplateByName("building");
        Reference<NetworkObject> networkObject =
                getNetworkManager().getServerManager().spawnNetworkObject(playerId, template);

        if (!Reference.isValid(networkObject)) {
            log.fine("Unable to create building: Could not create a Network Object.");
            return null;
        }

        GameObject gameObject = networkObject.get().getGameObject();
        TransformHex transform = gameObject.getTransform(TransformHex.class);
        transform.setPosition(qPos, rPos);
        transform.setHeight(tile.getHeight());
        Reference<Building> building = gameObject.getComponent(Building.class);

        if (!Reference.isValid(building)) {
            log.fine("Unable to create building: Reference to Building component is invalid.");
            return null;
        }

        building.get().afterServerSpawn();

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

        if (tile == null) {
            log.warning(
                    "Unable to add Building to list of owned tiles as the Building's HexagonTile is null.");
            return;
        }

        // Add the building at the relevant position.
        mOwnedBuildings.put(tile, building.getReference(Building.class));

        updateViewableTiles(building);
    }

    /**
     * Remove a {@link Building} from the {@link List} of owned buildings.
     *
     * @param building The {@link Building} to remove.
     * @return {@code true} on success, otherwise {@code false}.
     */
    public boolean removeOwnership(Building building) {
        if (building == null) return false;

        HexagonTile tile = building.getTile();
        if (tile == null) return false;

        // We clear viewable tiles in this case, because they can be invalid
        mTilesAround.clear();

        Reference<Building> removed = mOwnedBuildings.remove(tile);
        return (removed != null);
    }

    /**
     * Checks whether a tile is viewable by the player.
     *
     * @param tile tile to check for viewability
     * @return {@code true} if the tile is viewable, {@code false} otherwise.
     */
    public boolean isTileViewable(HexagonTile tile) {
        ensureViewableTilesAreValid();

        Integer val = mTilesAround.get(tile);

        return val != null && val > 0;
    }

    /**
     * Get how viewable the tile is.
     *
     * @param tile tile to check.
     * @return integer value for tile viewability. Only positive values mean viewable.
     */
    public int getTileViewability(HexagonTile tile) {
        ensureViewableTilesAreValid();
        return mTilesAround.getOrDefault(tile, VIEWABILITY_LOWER_BOUND);
    }

    /**
     * Gets the stream of viewable tiles.
     *
     * <p>If viewable tile list is empty, this method will regenerate that list.
     *
     * @return stream of viewable tiles
     */
    public Stream<HexagonTile> getViewableTiles() {
        ensureViewableTilesAreValid();
        return mTilesAround.entrySet().stream().filter(e -> e.getValue() > 0).map(e -> e.getKey());
    }

    /**
     * Ran when a tile is claimed.
     *
     * @param tile the tile
     * @param building the building which is on the tile if it exists
     */
    public void onClaimTile(HexagonTile tile, Building building) {
        mTilesAround.put(tile, building.getViewDistance().getValue());
        mFillTiles.push(tile);
    }

    /** Filters all viewable tiles by {@link Reference#isValid(Reference)}. */
    private void ensureViewableTilesAreValid() {
        if (mTilesAround.isEmpty()) {
            mTilesAround.clear();
            mFillTiles.clear();
            getOwnedBuildingsAsStream()
                    .filter(Reference::isValid)
                    .map(Reference::get)
                    .forEach(this::updateViewableTiles);
        }

        if (!Reference.isValid(mMap)) {
            return;
        }

        HexagonMap map = mMap.get();

        map.floodFill(
                mFillTiles,
                (__, t, neighbours, out) -> {
                    Integer val = mTilesAround.get(t);

                    if (val == null || val <= VIEWABILITY_LOWER_BOUND) {
                        return;
                    }

                    Integer newVal = val - 1;

                    for (HexagonTile n : neighbours) {
                        Integer nval = mTilesAround.get(n);

                        if (nval == null || nval < newVal) {
                            mTilesAround.put(n, newVal);
                            mFillTiles.push(n);
                        }
                    }
                },
                1);
    }

    /**
     * Get the {@link Building}, that is on the specified {@link HexagonTile}, from {@link
     * #mOwnedBuildings}.
     *
     * @param tile The tile to get the building from.
     * @return The reference to the building, if it is in your {@link #mOwnedBuildings}, otherwise
     *     {@code null}.
     */
    public Reference<Building> getOwnedBuilding(HexagonTile tile) {
        return mOwnedBuildings.get(tile);
    }

    /**
     * Get the {@link #mOwnedBuildings} as a {@link List}.
     *
     * @return The Buildings the player owns, as a List.
     */
    public List<Reference<Building>> getOwnedBuildings() {
        return new ArrayList<Reference<Building>>(mOwnedBuildings.values());
    }

    /**
     * This will return a {@link Stream} of {@link Reference}s to {@link Building}s which are owned
     * by the player.
     *
     * @return A stream containing references to the buildings the player owns.
     */
    public Stream<Reference<Building>> getOwnedBuildingsAsStream() {
        return mOwnedBuildings.values().stream();
    }

    /**
     * The number of {@link Building}s the player owns (the number of buildings in {@link
     * #mOwnedBuildings}).
     *
     * @return The number of buildings the player currently owns.
     */
    public int getNumberOfOwnedBuildings() {
        return mOwnedBuildings.size();
    }

    /**
     * Check if the {@link Building}'s owner is the player.
     *
     * @param building The building to check.
     * @return {@code true} if the player owns the building, otherwise {@code false}.
     */
    public boolean isBuildingOwner(Building building) {
        if (building == null) {
            return false;
        }
        return building.getOwnerId() == getNetworkObject().getOwnerId();
    }

    /**
     * Check if the {@link HexagonTile}'s owner is the player.
     *
     * @param tile The {@link HexagonTile} to check
     * @return {@code true} if the Player owns the tile
     */
    public boolean hasClaimedTile(HexagonTile tile) {
        if (tile == null || getNetworkObject() == null) {
            return false;
        }

        if (tile.getClaimantId() == null) {
            return false;
        }
        return tile.getClaimantId() == getNetworkObject().getOwnerId();
    }

    /**
     * Assign a reference to the current {@link HexagonMap} to {@link #mMap}.
     *
     * <p>This assumes that the current active scene contains the HexagonMap.
     *
     * @return Whether the assignment was successful.
     */
    private boolean assignMap() {
        HexagonMap map = Scene.getActiveScene().getSingleton(HexagonMap.class);
        if (map == null) return false;

        mMap = map.getReference(HexagonMap.class);
        return Reference.isValid(mMap);
    }

    /**
     * Get the {@link HexagonMap} being used by the Player, as stored in {@link #mMap}.
     *
     * @return The HexagonMap, or {@code null}.
     */
    public HexagonMap getMap() {
        if (!Reference.isValid(mMap)) return null;
        return mMap.get();
    }

    /**
     * Get whether the player is currently in the attack cooldown.
     *
     * @return Whether the player is in the attack cooldown period.
     */
    public boolean inCooldown() {
        return getRemainingCooldown() > 0;
    }

    /**
     * Get the time left in the cooldown period.
     *
     * @return The remaining time to wait.
     */
    public float getRemainingCooldown() {
        return Math.max(
                getConfig().getAttackCooldown()
                        - (getNetworkManager().getServerTime() - mLastAttack.get()),
                0f);
    }

    /**
     * Process and parse a request in which the player wishes to place a {@link Building}.
     *
     * @param data The {@link BuildData} sent by the client.
     */
    private void buildRequest(BuildData data) {
        if (gameEnd()) {
            return;
        }
        HexagonMap map = getMap();
        if (map == null) {
            log.warning("Unable to parse BuildData: Map is null.");
            return;
        }

        HexagonTile tile = data.getTile(map);
        if (tile == null) {
            log.fine("Unable to parse BuildData: Tile from BuildData is null.");
            return;
        }

        BuildingDescriptor descriptor = data.getDescriptor();
        if (descriptor == null) {
            log.fine("Unable to parse BuildData: Descriptor from BuildData is null.");
            return;
        }

        // Try to place the building on the tile.
        if (buildAttempt(tile, descriptor)) {
            if (getNetworkObject().getOwnerId() < 0) return; // dont play sound if ai
            invokeSound(AudioFiles.BUILDING_SOUND, ServerEvent.EventRecipients.OWNER);
        }
    }

    /**
     * Attempt to place a building on a specific tile.
     *
     * <p>This first checks the tile to make sure it is fully eligible before any placement happens.
     *
     * @param tile The tile to place a building on.
     * @param descriptor the description of the building we want to attempt to build
     * @return Whether the attempt to build was successful.
     */
    private boolean buildAttempt(HexagonTile tile, BuildingDescriptor descriptor) {

        if (!buildCheck(tile, descriptor.getTotalCost(this))) {
            log.fine("Unable to pass build check.");
            return false;
        }

        Building building = createBuilding(tile.getQ(), tile.getR(), false);
        if (building == null) {
            log.fine("Unable to add building.");
            return false;
        }

        building.getAttack().setLevel(descriptor.getAttack());
        building.getDefence().setLevel(descriptor.getDefence());
        building.getTokenGeneration().setLevel(descriptor.getTokenGenerationLevel());
        building.setSellPrice(descriptor.getSellPrice(this));
        // Subtract the cost.
        mTokens.subtract(descriptor.getTotalCost(this));
        log.fine("Added building.");
        return true;
    }

    /**
     * Ensure that the {@link HexagonTile} is eligible to have a {@link Building} placed on it.
     *
     * @param tile The tile to put a building on.
     * @param buyPrice The cost of the building.
     * @return {@code true} if the tile is eligible, otherwise {@code false}.
     */
    public boolean buildCheck(HexagonTile tile, int buyPrice) {
        if (tile == null) {
            log.fine("Tile is null.");
            return false;
        }
        if (getTokens().get() < buyPrice) {
            log.fine("Not enough tokens to buy building.");
            return false;
        }
        return tile.isBuildable(this);
    }

    /**
     * Invoke animation when an attack is being performed.
     *
     * @param data information about the attack for effects to use
     */
    void attackEffect(AttackData data) {
        HexagonMap map = getMap();
        if (map == null) {
            log.fine("Unable to parse AttackData: Map is null.");
            return;
        }

        Building attacker = data.getAttacker(map);
        if (attacker == null) {
            log.fine("Unable to parse AttackData: attacking building is null.");
            return;
        }

        Building defender = data.getDefender(map);
        if (defender == null) {
            log.fine("Unable to parse AttackData: defending building is null.");
            return;
        }

        MapEffects fx = getMapEffects();

        if (fx == null) {
            log.warning("Map effects was null.");
            return;
        }

        fx.pulseHighlight(attacker, StandardHighlightType.SELECT.asSelection(), 0.2f, 0.5f, 2f);
        fx.pulseHighlight(defender, StandardHighlightType.INVALID.asSelection(), 0.2f, 0.5f, 2f);
        attacker.attackEffect(defender, mAttackDuration);
    }

    /**
     * Process and parse a request in which the player wishes to attack a {@link Building} from
     * another Building.
     *
     * @param data The {@link AttackData} sent by the client.
     */
    private void attackRequest(AttackData data) {
        HexagonMap map = getMap();
        if (map == null) {
            log.warning("Unable to parse AttackData: Map is null.");
            return;
        }

        Building attacker = data.getAttacker(map);
        if (attacker == null) {
            log.fine("Unable to parse AttackData: attacking building is null.");
            return;
        }

        Building defender = data.getDefender(map);
        if (defender == null) {
            log.fine("Unable to parse AttackData: defending building is null.");
            return;
        }

        // Try to run an attack.
        attackAttempt(attacker, defender);
    }

    /**
     * Attempt to attack an opponent {@link Building} from a player owned Building.
     *
     * <p>This first checks the buildings can attack each other before any attack happens.
     *
     * @param attacker The attacking building.
     * @param defender The defending building.
     * @return Whether the attempt to attack was successfully dispatched.
     */
    private boolean attackAttempt(Building attacker, Building defender) {

        if (gameEnd()) {
            return false;
        }

        if (!attackCheck(attacker, defender)) {
            log.fine("Unable to pass attack check.");
            return false;
        }
        attacker.invokeSound(AudioFiles.ATTACK_INVOKED_SOUND, ServerEvent.EventRecipients.OWNER);
        defender.invokeSound(AudioFiles.ATTACK_INVOKED_SOUND, ServerEvent.EventRecipients.OWNER);
        log.fine("Attacking");

        mLastAttack.set(getNetworkManager().getServerTime());

        // ATTACK!
        mTokens.subtract(attacker.getAttackCost());

        float lockTime =
                Engine.getInstance().getCurTime()
                        + mAttackDuration
                        - 0.1f; // show effects just before

        attacker.getActionLockTime().set(lockTime);
        attacker.setServerActionLocked(true);

        defender.getActionLockTime().set(lockTime);
        defender.setServerActionLocked(true);

        mServerAttackEvent.invoke((data) -> data.setData(attacker, defender));

        PlayerConfig cfg = getConfig();

        new AwaitFuture((__) -> !attacker.isTimeActionLocked())
                .then(
                        (__) -> {
                            attacker.setServerActionLocked(false);
                            defender.setServerActionLocked(false);

                            // Could happen when the game ends in the middle of attack
                            if (defender.getOwner() == null || defender.getOwner() == null) {
                                return;
                            }

                            boolean won;
                            if (defender.getOwner().hasLost()) won = true;
                            else won = attacker.attack(defender);

                            // If you've won attack
                            if (won) {
                                attacker.invokeSound(
                                        AudioFiles.ATTACK_SUCCESS_SOUND,
                                        ServerEvent.EventRecipients.OWNER);
                                defender.invokeSound(
                                        AudioFiles.DEFENCE_FAILED_SOUND,
                                        ServerEvent.EventRecipients.OWNER);

                                mBuildingInflationBonus.set(
                                        mBuildingInflationBonus.get()
                                                + cfg.getBuildingInflationBonus());

                                Player defenderPlayer = defender.getOwner();

                                if (defenderPlayer != null) {
                                    defenderPlayer.mBuildingInflationBonus.set(
                                            defenderPlayer.mBuildingInflationBonus.get()
                                                    + cfg.getBuildingLostInflationBonus());
                                }

                                // Special checks for Capital
                                if (defender.isCapital()) {
                                    defender.setCapital(false);

                                    mBuildingInflationBonus.set(
                                            mBuildingInflationBonus.get()
                                                    + cfg.getCapitalInflationBonus());

                                    GameState state = mGameState.get();
                                    state.getNumCapitalsStanding().add(-1);

                                    // Update stats
                                    ArrayList<SyncStat> stats = defender.getStats();
                                    for (SyncStat stat : stats) stat.set(SyncStat.LEVEL_MIN);

                                    defender.getOwner().setOwnsCapital(false);
                                    defender.afterStatChange();
                                }
                                defender.getNetworkObject()
                                        .setOwnerId(getNetworkObject().getOwnerId());
                            } else {
                                attacker.invokeSound(
                                        AudioFiles.ATTACK_FAILURE_SOUND,
                                        ServerEvent.EventRecipients.OWNER);
                                defender.invokeSound(
                                        AudioFiles.DEFENCE_SUCCESS_SOUND,
                                        ServerEvent.EventRecipients.OWNER);
                            }
                        })
                .schedule();

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

        // Checks if you have capital, and the game is still going
        if (gameEnd()) {
            return false;
        }

        if (attacker == null) {
            log.fine("Attacker is null.");
            return false;
        }

        if (defender == null) {
            log.fine("Defender is null.");
            return false;
        }

        // Checks you have the cash
        if (mTokens.get() < attacker.getAttackCost()) {
            log.fine("You don't have the cash to attack.");
            return false;
        }

        // Checks you own the building
        if (isBuildingOwner(attacker) == false) {
            log.fine("It's not your building.");
            return false;
        }

        // Checks if you have passed an attackable building
        if (!attacker.isBuildingAttackable(defender)) {
            log.fine("Player passed a non-attackable building!");
            return false;
        }

        // Checks you're not attacking your own building
        if (defender.getOwnerId() == attacker.getOwnerId()) {
            log.fine("Player passed a building which it owns");
            return false;
        }

        if (attacker.isActionLocked() || defender.isActionLocked()) {
            log.fine("One of the buildings is action locked.");
            return false;
        }

        // Checks if you're in cooldown
        if (inCooldown()) {
            log.fine("Still in cooldown: " + getNetworkManager().getServerTime());
            return false;
        }

        return true;
    }

    /**
     * Process and parse a request in which the player wishes to sell a {@link Building}.
     *
     * @param data The {@link SellData} sent by the client.
     */
    private void sellRequest(SellData data) {

        if (gameEnd()) {
            return;
        }

        HexagonMap map = getMap();
        if (map == null) {
            log.warning("Unable to parse StatData: Map is null.");
            return;
        }

        Building building = data.getBuilding(map);
        if (building == null) {
            log.fine("Unable to parse StatData: Building from StatData is null.");
            return;
        }

        // Try to sell the building on the tile.
        if (sellAttempt(building)) {
            building.invokeSound(AudioFiles.BUILDING_SELL_SOUND, ServerEvent.EventRecipients.OWNER);
        }
    }

    /**
     * Play a sound on the player.
     *
     * @param sound the sound to be played
     * @param audience the audience that can hear it
     */
    public void invokeSound(AudioFiles sound, ServerEvent.EventRecipients audience) {
        switch (audience) {
            case OWNER:
                mOwnerAudioEvent.invoke(new GameState.InvokeAudioEvent(sound));
                break;
            case ACTIVE_CLIENTS:
            case ALL_CLIENTS:
                mGlobalAudioEvent.invoke(new GameState.InvokeAudioEvent(sound));
                break;
        }
    }

    /**
     * Attempt to sell a specified building.
     *
     * <p>This first checks the building is fully eligible before any selling happens.
     *
     * @param building The building to sell.
     * @return Whether the attempt to sell the building was successful.
     */
    private boolean sellAttempt(Building building) {
        if (!sellCheck(building)) {
            log.fine("Unable to pass sell check.");
            return false;
        }

        // Adds the tokens
        mTokens.add(building.getSellPrice());

        // Remove the building
        building.remove();

        return true;
    }

    /**
     * Ensure that the {@link Building} is eligible to be sold.
     *
     * @param building The building to sell.
     * @return {@code true} if the building can be sold, otherwise {@code false}.
     */
    public boolean sellCheck(Building building) {

        if (building == null) {
            log.fine("building is null.");
            return false;
        }

        if (building.isActionLocked()) {
            log.fine("Action locked.");
            return false;
        }

        // Checks that you own the building
        if (isBuildingOwner(building) == false) {
            log.fine("You do not own the building.");
            return false;
        }

        if (building.isCapital()) {
            log.fine("You cannot sell your capital.");
            return false;
        }

        return true;
    }

    /**
     * Process and parse a request in which the player wishes to increase a specific {@link
     * StatType} of a {@link Building}.
     *
     * @param data The {@link StatData} sent by the client.
     */
    private void statRequest(StatData data) {

        if (gameEnd()) {
            return;
        }

        HexagonMap map = getMap();
        if (map == null) {
            log.warning("Unable to parse StatData: Map is null.");
            return;
        }

        Building building = data.getBuilding(map);
        if (building == null) {
            log.fine("Unable to parse StatData: Building from StatData is null.");
            return;
        }

        StatType statType = data.getStat();
        if (statType == null) {
            log.fine("Unable to parse StatData: StatType from StatData is null.");
            return;
        }

        // Try to change the stats of the building.
        if (statAttempt(building, statType)) {
            building.invokeSound(AudioFiles.BUILDING_SOUND, ServerEvent.EventRecipients.OWNER);
        }
    }

    /**
     * Attempt to increase a specific {@link StatType} of a {@link Building}.
     *
     * <p>This first checks the building and stat to make sure that they are fully eligible before
     * any stat changes happen.
     *
     * @param building The building whose stat will be changed.
     * @param statType The stat to increase.
     * @return Whether the attempt to change the stat was successful.
     */
    private boolean statAttempt(Building building, StatType statType) {
        if (!statCheck(building, statType)) {
            log.fine("Unable to pass stat check.");
            return false;
        }

        SyncStat stat = building.getStat(statType);
        // Remove the cost.
        mTokens.subtract(stat.getCost());
        // Increase the stat level.
        stat.increaseLevel();
        // Update the building on the server.
        building.afterStatChange(stat.getType());

        return true;
    }

    /**
     * Ensure that the {@link Building} is eligible to have its {@link StatType} changed.
     *
     * @param building The building to have its stat increased.
     * @param statType The stat to increase.
     * @return {@code true} if the building and stat is eligible, otherwise {@code false}.
     */
    public boolean statCheck(Building building, StatType statType) {

        if (building == null) {
            log.warning("Building is null.");
            return false;
        }

        if (building.isActionLocked()) {
            log.fine("Action locked.");
            return false;
        }

        if (statType == null) {
            log.fine("StatType is null.");
            return false;
        }

        // Checks you own the building
        if (isBuildingOwner(building) == false) {
            log.fine("Building not owned.");
            return false;
        }

        SyncStat stat = building.getStat(statType);

        if (stat == null) {
            log.fine("Building is missing specified stat.");
            return false;
        }

        if (stat.isUpgradeable() == false) {
            log.fine("Building stat not upgradeable.");
            return false;
        }

        if (mTokens.get() < stat.getCost()) {
            log.fine("Cannot afford building upgrade.");
            return false;
        }

        return true;
    }

    /**
     * This will return whether the player has lost their capital and thus the game.
     *
     * @return {@code true} if they have they have lost there capital and thus the game {@code
     *     false} if not
     */
    public boolean hasLost() {
        return !mOwnsCapital.get();
    }

    /**
     * This will return whether the game has ended and no further actions can take place.
     *
     * <p>This method includes {@link #hasLost()} check.
     *
     * @return {@code true} if the game has has ended, {@code false} if not
     */
    public boolean gameEnd() {
        if (hasLost()) {
            for (Reference<Building> building : getOwnedBuildings()) {
                if (!Reference.isValid(building)) continue;
                // Remove the building
                building.get().remove();
            }

            return true;
        }

        if (!Reference.isValid(mGameState)) {
            Scene activeScene = Scene.getActiveScene();

            if (activeScene != null) {
                mGameState = activeScene.getSingletonRef(GameState.class);
            }
        }

        return Reference.isValid(mGameState) && !mGameState.get().getInGame().get();
    }

    /**
     * Sets if the player owns a capital or not.
     *
     * @param hasCapital true if they own a capital, false if not
     */
    public void setOwnsCapital(boolean hasCapital) {
        mOwnsCapital.set(hasCapital);
    }

    /**
     * Retrieve current player inflation.
     *
     * <p>This method will essentially return {@code globalInflation * buildingInflation}, if game
     * state exists.
     *
     * @return player inflation. Defaults to 1, if game state is not valid.
     */
    public float getInflation() {
        float inflation = 1f;

        if (Reference.isValid(mGameState)) {
            GameState state = mGameState.get();
            inflation *= state.getGlobalInflation();
            inflation *=
                    Math.pow(
                            mOwnedBuildings.size() + mBuildingInflationBonus.get(),
                            state.getConfig().getPlayer().getInflationPerBuilding());
        }

        return inflation;
    }

    /**
     * Gets the global inflation, if game state exists.
     *
     * @return the inflation value
     */
    public float getGlobalInflation() {
        GameState state = getGameState();

        if (state != null) {
            return state.getGlobalInflation();
        }

        return 1;
    }

    /**
     * Retrieves the current capital, if one exists
     *
     * <p>This method exists on assumption that a player can only lose a capital, but it itself can
     * not change.
     *
     * @return capital, if one exists
     */
    public Building getCapital() {
        if (!mOwnsCapital.get()) return null;

        if (Reference.isValid(mCapital)) return mCapital.get();

        mCapital =
                mOwnedBuildings.values().stream()
                        .filter(Reference::isValid)
                        .map(Reference::get)
                        .filter(Building::isCapital)
                        .map(b -> b.getReference(Building.class))
                        .findFirst()
                        .orElse(null);

        return mCapital != null ? mCapital.get() : null;
    }

    /**
     * Get the {@link MapEffects}.
     *
     * <p>If {@link #mMapEffects} is not valid, it will attempt to get a valid MapEffects.
     *
     * @return The {@link MapEffects}; otherwise {@code null}.
     */
    public MapEffects getMapEffects() {
        if (!Reference.isValid(mMapEffects)) {
            MapEffects mapEffects = Scene.getActiveScene().getSingleton(MapEffects.class);

            if (mapEffects == null) return null;
            mMapEffects = mapEffects.getReference(MapEffects.class);
        }

        return mMapEffects.get();
    }
}
