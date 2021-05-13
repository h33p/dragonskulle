/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.SingletonStore;
import org.dragonskulle.game.App;
import org.dragonskulle.game.GameConfig.PlayerConfig;
import org.dragonskulle.game.GameState;
import org.dragonskulle.game.GameUIAppearance;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.game.misc.ArcPath;
import org.dragonskulle.game.misc.ArcPath.IArcHandler;
import org.dragonskulle.game.misc.ArcPath.IPathUpdater;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.ServerNetworkManager;
import org.dragonskulle.network.components.requests.ServerEvent;
import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncFloat;
import org.dragonskulle.network.components.sync.SyncInt;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * A Building component.
 *
 * <p>Once created, the GameObject needs a {@link TransformHex} component to place it in the game
 * and to allow the logic to access its position.
 *
 * <p>The owner of the Building also needs to be set via {@link NetworkObject#setOwnerId}.
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
@Log
public class Building extends NetworkableComponent implements IOnAwake, IOnStart, IFixedUpdate {

    /** A map between {@link StatType}s and their {@link SyncStat} values. */
    EnumMap<StatType, SyncStat> mStats = new EnumMap<StatType, SyncStat>(StatType.class);

    /** Stores the attack strength of the building. */
    @Getter private final SyncStat mAttack = new SyncStat(this);
    /** Stores the defence strength of the building. */
    @Getter private final SyncStat mDefence = new SyncStat(this);
    /** Stores how many tokens the building can generate in one go. */
    @Getter private final SyncStat mTokenGeneration = new SyncStat(this);
    /** Stores the view range of the building. */
    @Getter private final SyncStat mViewDistance = new SyncStat(this);
    /** Stores the build range of the building. */
    @Getter private final SyncStat mBuildDistance = new SyncStat(this);
    /** Stores the claim range of the building. */
    @Getter private final SyncStat mClaimDistance = new SyncStat(this);

    /** Whether the building is a capital. */
    public final SyncBool mIsCapital = new SyncBool(false);

    private Reference<AudioSource> mJukeBox;

    /** List of networked prop templates to spawn. */
    private static final String[] PROPS = {"defence_prop", "attack_prop", "token_generation_prop"};

    /**
     * Whether actions on this building (sell, upgrade, attack, etc. etc.) are locked.
     *
     * <p>It is used in attacks to prevent the building from being destroyed, taken over, attacked
     * twice.
     */
    @Getter private final SyncFloat mActionLockTime = new SyncFloat();

    /**
     * Action lock. This value is only needed to be set by the server, and it is meant to ensure
     * there are no race conditions when building becomes unlocked by the action lock, but the
     * action was still not processed.
     */
    @Getter @Setter private boolean mServerActionLocked = false;

    /** The tiles the building claims, including the tile the building is currently on. */
    @Getter private final Set<HexagonTile> mClaimedTiles = new HashSet<>();

    /** Tiles that are around {@link #mClaimedTiles}. */
    private final Map<HexagonTile, Integer> mNeighboringTiles = new HashMap<>();

    /** The tiles the building can currently attack (those with claims neighboring our claims). */
    private final ArrayList<HexagonTile> mAttackableTiles = new ArrayList<>();

    /** Building templates, used to distinguish the buildings. */
    private static final Resource<GLTF> sBuildingTemplates = GLTF.getResource("building_templates");

    /** Store a random number generator. */
    private Random mRandom = new Random();

    static final GameObject FIREBALL_TEMPLATE =
            App.TEMPLATES.get().getDefaultScene().findRootObject("attack_ball");

    /**
     * Store {@link HexagonTile}s that are known to be theoretically fine locations for placing a
     * Building.
     *
     * <p>Tiles excluded from this set:
     *
     * <ul>
     *   <li>Non-land tiles.
     *   <li>Tiles claimed by <b>this</b> building.
     * </ul>
     *
     * <p>To actually get a more refined set that excludes tiles claimed by other buildings, please
     * use: {@link #getBuildableTiles()}.
     */
    private ArrayList<HexagonTile> mPlaceableTiles = new ArrayList<HexagonTile>();

    private Vector3i mPosition = null;

    private boolean mInitialised = false;

    /** Controls how deep around claimed tiles we go for neighbouring tile calculation. */
    private static final int NEIGHBOUR_BOUND = 5;

    /** The reimbursement from selling a {@link Building}. */
    private SyncInt mSellPrice = new SyncInt(2);

    /**
     * The base price for upgrading a stat. Automatically added to {@link SyncStat#getCost()}.
     *
     * <p>Calculated in {@link #generateStatBaseCost()}.
     */
    @Getter private int mStatBaseCost = 0;

    /** Store the {@link HexagonMap} that the {@link Building} is on. */
    private Reference<HexagonMap> mMap = null;

    /**
     * This is used as a flag to determine when any of the stats have changed, thus requiring a
     * visible update to UI. It is enabled in {@link this#afterStatChange} and has to be manually
     * disabled once the needed update is finished.
     */
    @Getter
    @Accessors(prefix = "m")
    private int mStatUpdateCount = 0;

    /** The base building mesh. */
    private Reference<GameObject> mBaseMesh;
    /** The Mesh for when the highest stat is defence. */
    private Reference<GameObject> mDefenceMesh;
    /** The Mesh for when the highest stat is attacking. */
    private Reference<GameObject> mAttackMesh;
    /** The Mesh for when the highest stat is generation. */
    private Reference<GameObject> mGenerationMesh;
    /** The current building mesh. */
    private Reference<GameObject> mVisibleMesh;

    private transient ServerEvent<GameState.InvokeAudioEvent> mOwnerAudioEvent;
    private transient ServerEvent<GameState.InvokeAudioEvent> mGlobalAudioEvent;

    /**
     * Gets an array of stats that will be available to upgrade in the shop.
     *
     * @return an array of stat types
     */
    public static List<StatType> getShopStatTypes() {
        return Arrays.asList(StatType.ATTACK, StatType.DEFENCE, StatType.TOKEN_GENERATION);
    }

    /**
     * Gets the number of stats displayed in the shop.
     *
     * @return the number of stats
     */
    public static int getNumberOfShopStatTypes() {
        return getShopStatTypes().size();
    }

    /** Increments {@code mStatUpdateCount} to signify an update is needed. */
    public void setStatsRequireVisualUpdate() {
        mStatUpdateCount++;
    }

    /**
     * Create a new {@link Building}. This should be added to a {@link HexagonTile}. {@link
     * HexagonTile}.
     */
    public Building() {}

    @Override
    public void onConnectedSyncvars() {
        // Initialise each SyncStat with the relevant StatType.
        initiliseStat(mAttack, StatType.ATTACK);
        initiliseStat(mDefence, StatType.DEFENCE);
        initiliseStat(mTokenGeneration, StatType.TOKEN_GENERATION);
        initiliseStat(mViewDistance, StatType.VIEW_DISTANCE);
        initiliseStat(mBuildDistance, StatType.BUILD_DISTANCE);
        initiliseStat(mClaimDistance, StatType.CLAIM_DISTANCE);
    }

    @Override
    protected void afterNetUpdate() {
        if (!isCapital() && mIsCapital.isClientDirty()) {
            assignMesh();
        }
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        checkInitialise();
    }

    @Override
    public void onAwake() {
        GLTF gltf = sBuildingTemplates.get();

        GameObject baseMesh =
                GameObject.instantiate(gltf.getDefaultScene().findRootObject("base_building"));
        GameObject defenceMesh =
                GameObject.instantiate(gltf.getDefaultScene().findRootObject("defence_building"));
        GameObject attackMesh =
                GameObject.instantiate(gltf.getDefaultScene().findRootObject("attack_building"));
        GameObject generationMesh =
                GameObject.instantiate(
                        gltf.getDefaultScene().findRootObject("generation_building"));
        getGameObject().addChild(baseMesh);
        getGameObject().addChild(defenceMesh);
        getGameObject().addChild(attackMesh);
        getGameObject().addChild(generationMesh);
        baseMesh.setEnabled(false);
        defenceMesh.setEnabled(false);
        attackMesh.setEnabled(false);
        generationMesh.setEnabled(false);
        mBaseMesh = baseMesh.getReference();
        mDefenceMesh = defenceMesh.getReference();
        mAttackMesh = attackMesh.getReference();
        mGenerationMesh = generationMesh.getReference();
    }

    /** Initialise the building only when it is properly on the map and the tile is synced. */
    void checkInitialise() {
        if (mInitialised) {
            return;
        }

        HexagonTile tile = getTile();

        if (tile == null) {
            return;
        }

        // Add the Building to the owner's mOwnedBuildings.
        Player owningPlayer = getOwner();

        if (owningPlayer == null) {
            return;
        }

        owningPlayer.addOwnership(this);

        // Generate the base cost of upgrading stats.
        generateStatBaseCost();

        // Generate the lists of tiles that are influenced by the Stats of the Building.
        generateTileLists();

        mInitialised = true;
    }

    @Override
    public void onOwnerIdChange(int newId) {
        Player owningPlayer = getOwner();
        if (owningPlayer != null) {
            owningPlayer.removeOwnership(this);
        }
        Player newOwningPlayer = getOwner(newId);
        if (newOwningPlayer == null) {
            log.severe("New owner is null!");
            return;
        }
        newOwningPlayer.addOwnership(this);
    }

    @Override
    public void onStart() {

        mapCheck();

        checkInitialise();

        AudioSource src = new AudioSource();
        getGameObject().addComponent(src);
        mJukeBox = src.getReference(AudioSource.class);

        if (isCapital()) {
            GLTF gltf = sBuildingTemplates.get();

            GameObject capital_mesh =
                    GameObject.instantiate(
                            gltf.getDefaultScene().findRootObject("capital_building"));

            getGameObject().addChild(capital_mesh);
            capital_mesh.setEnabled(true);
            mVisibleMesh = capital_mesh.getReference();
            return;
        }

        assignMesh();
    }

    @Override
    protected void onNetworkInitialise() {
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
    }

    /** Method to be called after the building is created by {@link Player} on the server. */
    public void afterServerSpawn() {
        mapCheck();

        // Add the building to the relevant HexagonTile.
        getTile().setBuilding(this);

        // Generate the list of tiles that have been claimed by the Building. This is always a set
        // radius so only needs to be generated once.
        generateClaimTiles();
    }

    /** Fetches a valid {@link Reference} to the map and stores it in {@link #mMap}. */
    private void mapCheck() {
        // Store the map.
        Reference<HexagonMap> mapCheck =
                getGameObject().getScene().getSingletonRef(HexagonMap.class);
        if (Reference.isValid(mapCheck)) {
            mMap = mapCheck;
        } else {
            log.severe("mapCheck is null.");
        }
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

    /** Checks whether arc paths need updating, and updates them. */
    private void checkPaths() {
        final int div = 1;
        final int numFireballs = (mAttack.getLevel() + div - 1) / div;

        if (mPaths.size() == numFireballs || mGameObject == null) {
            return;
        }

        mPaths.stream()
                .filter(Reference::isValid)
                .map(Reference::get)
                .forEach(mGameObject::removeComponent);

        mPaths.clear();

        for (int i = 0; i < numFireballs; i++) {
            ArcPath path = new ArcPath();
            path.setTemplate(FIREBALL_TEMPLATE);
            getGameObject().addComponent(path);
            mPaths.add(path.getReference(ArcPath.class));
        }
    }

    /**
     * Ensures that changes to stats are reflected in the building.
     *
     * <ul>
     *   <li><b>Needs to manually be called on the server</b> after stats have been changed.
     *   <li>Automatically called on the client via {@link SyncStat}s.
     * </ul>
     */
    public void afterStatChange() {
        log.fine("After stats change.");

        generateStatBaseCost();

        generateTileLists();
        if (!isCapital()) assignMesh();

        setStatsRequireVisualUpdate();

        checkPaths();
    }

    /**
     * Ensures that changes to stats are reflected in the building.
     *
     * <ul>
     *   <li><b>Needs to manually be called on the server</b> after stats have been changed.
     *   <li>Automatically called on the client via {@link SyncStat}s.
     * </ul>
     *
     * @param type the stat which has been updated individually
     */
    public void afterStatChange(StatType type) {
        afterStatChange();
    }

    /** Assigns a visible mesh to be displayed depending on the maximum stat level. */
    public void assignMesh() {
        Map<StatType, Integer> statLevels =
                getShopStats().stream()
                        .collect(Collectors.toMap(SyncStat::getType, SyncStat::getLevel));
        if (statLevels.values().stream().distinct().count() <= 1) {
            log.fine("the stats are all the same");
            if (Reference.isValid(mVisibleMesh)) {
                mVisibleMesh.get().setEnabled(false);
            }
            if (Reference.isValid(mBaseMesh)) {
                mBaseMesh.get().setEnabled(true);
                mVisibleMesh = mBaseMesh;
            }
        } else {
            Map.Entry<StatType, Integer> max = null;
            for (Map.Entry<StatType, Integer> entry : statLevels.entrySet()) {
                if (max == null || entry.getValue().compareTo(max.getValue()) > 0) {
                    max = entry;
                }
            }
            if (max != null) {
                if (Reference.isValid(mVisibleMesh)) {
                    mVisibleMesh.get().setEnabled(false);
                }
                switch (max.getKey()) {
                    case ATTACK:
                        if (Reference.isValid(mAttackMesh)) {
                            mAttackMesh.get().setEnabled(true);
                            mVisibleMesh = mAttackMesh;
                        }
                        break;
                    case DEFENCE:
                        if (Reference.isValid(mDefenceMesh)) {
                            mDefenceMesh.get().setEnabled(true);
                            mVisibleMesh = mDefenceMesh;
                        }
                        break;
                    case TOKEN_GENERATION:
                        if (Reference.isValid(mGenerationMesh)) {
                            mGenerationMesh.get().setEnabled(true);
                            mVisibleMesh = mGenerationMesh;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /** Generate the stored lists of {@link HexagonTile}s. */
    private void generateTileLists() {
        generateNeighboringTiles();
        generateAttackableTiles();
        generatePlaceableTiles();
    }

    /** Cleanup generated tile lists. */
    private void cleanupTileLists() {
        // Reset the list of claimed, viewable and attackable tiles.
        mAttackableTiles.clear();
        mPlaceableTiles.clear();
        mNeighboringTiles.clear();
    }

    /**
     * Generate the base cost of buying an upgrade. The more upgraded a building is, the higher the
     * base cost.
     */
    private void generateStatBaseCost() {
        int totalUpgrades = 0;
        for (SyncStat stat : getStats()) {
            totalUpgrades += stat.getLevel() - SyncStat.LEVEL_MIN;
        }

        mStatBaseCost = 1 + totalUpgrades * 3;
    }

    /** Claim the tiles around the building and the tile the building is on. */
    private void generateClaimTiles() {
        if (getNetworkObject().isServer()) {
            // Get the map.
            HexagonMap map = getMap();
            if (map == null) {
                return;
            }

            int distance = mClaimDistance.getValue();

            HexagonTile tile = getTile();

            List<HexagonTile> tiles = map.getTilesInRadius(tile, distance, true, new ArrayList<>());

            mClaimedTiles.clear();

            for (HexagonTile hexagonTile : tiles) {
                hexagonTile.setClaimedBy(this);
            }

            // Add props on the object.
            ServerNetworkManager serverMan = getNetworkManager().getServerManager();

            int ownerId = getNetworkObject().getOwnerId();
            int objId = getNetworkObject().getId();

            int propId = 0;

            // Defence is always spawned on the tile.
            Reference<NetworkObject> nob =
                    serverMan.spawnNetworkObject(
                            ownerId, getNetworkManager().findTemplateByName(PROPS[propId++]));
            nob.get()
                    .getGameObject()
                    .getComponent(BuildingProps.class)
                    .get()
                    .getBuildingNetId()
                    .set(objId);
            nob.get()
                    .getGameObject()
                    .getTransform(TransformHex.class)
                    .setPosition(tile.getQ(), tile.getR());

            for (HexagonTile hexagonTile : tiles) {
                if (propId >= PROPS.length) break;
                if (hexagonTile == tile) continue;
                if (hexagonTile.getTileType() != TileType.LAND) continue;
                if (hexagonTile.getClaimedBy() != this) continue;
                nob =
                        serverMan.spawnNetworkObject(
                                ownerId, getNetworkManager().findTemplateByName(PROPS[propId++]));
                nob.get()
                        .getGameObject()
                        .getComponent(BuildingProps.class)
                        .get()
                        .getBuildingNetId()
                        .set(objId);
                nob.get()
                        .getGameObject()
                        .getTransform(TransformHex.class)
                        .setPosition(hexagonTile.getQ(), hexagonTile.getR());
            }
        }
    }

    /**
     * The tiles the building claims, excluding the tile the building is currently on.
     *
     * @return the surrounding tiles
     */
    public Set<HexagonTile> getSurroundingTiles() {
        Set<HexagonTile> claimed = getClaimedTiles();
        claimed.remove(this.getTile());
        return claimed;
    }

    /**
     * Ran on claim tile.
     *
     * @param tile the tile
     */
    public void onClaimTile(HexagonTile tile) {
        if (mClaimedTiles.add(tile)) {
            Player owner = getOwner();

            if (owner != null) {
                owner.onClaimTile(tile, this);
            }

            cleanupTileLists();
        }
    }

    /**
     * Store the tiles that can be attacked around the building, excluding the tile the building is
     * on.
     */
    private void generateNeighboringTiles() {
        // Get the map.
        HexagonMap map = getMap();
        if (map == null) {
            return;
        }

        mNeighboringTiles.clear();

        Deque<HexagonTile> tilesToFill = new ArrayDeque<>();

        tilesToFill.addAll(mClaimedTiles);

        map.floodFill(
                tilesToFill,
                (__, t, neighbours, out) -> {
                    Integer val = mNeighboringTiles.getOrDefault(t, 0);

                    if (val >= NEIGHBOUR_BOUND) {
                        return;
                    }

                    Integer newVal = val + 1;

                    for (HexagonTile n : neighbours) {
                        Integer nval = mNeighboringTiles.get(n);

                        if ((nval == null || nval > newVal) && n.getClaimedBy() != this) {
                            mNeighboringTiles.put(n, newVal);
                            out.push(n);
                        }
                    }
                },
                1);
    }

    /**
     * Explores around the neighbouring tiles and stores those that are within an attacking range in
     * {@link #mAttackableTiles}.
     */
    private void generateAttackableTiles() {
        // Clear the current list of attackable tiles.
        mAttackableTiles.clear();

        mNeighboringTiles.forEach(
                (tile, val) -> {
                    if (val == 1) {
                        mAttackableTiles.add(tile);
                    }
                });
    }

    /** Store the tiles that are suitable for placing a building on. */
    private void generatePlaceableTiles() {
        // Clear the current list of buildable tiles.
        mPlaceableTiles.clear();

        // Get the current build distance.
        int distance = mBuildDistance.getValue();

        mNeighboringTiles.forEach(
                (tile, val) -> {
                    if (tile.getTileType() == TileType.LAND && val <= distance) {
                        mPlaceableTiles.add(tile);
                    }
                });
    }

    /**
     * Play a sound on the buildings owner.
     *
     * @param sound the sound to play
     * @param audience the audience who can hear it
     */
    public void invokeSound(
            GameUIAppearance.AudioFiles sound, ServerEvent.EventRecipients audience) {
        if (this.getOwnerId() < 0) return; // if non human
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

    /** Arc updater for attack visuals. */
    private class ArcUpdater implements IPathUpdater, IArcHandler {
        private final float mAttackStart;
        private final float mAttackTime;

        private final float mRotx;
        private final float mRoty;
        private final float mRotz;

        /**
         * Constructor for {@link ArcUpdater}.
         *
         * @param attackTime how long the attack takes.
         */
        public ArcUpdater(float attackTime) {
            mAttackStart = Engine.getInstance().getCurTime();
            mAttackTime = attackTime;
            mRotx = (float) Math.random() * 360f;
            mRoty = (float) Math.random() * 360f;
            mRotz = (float) Math.random() * 360f;
        }

        @Override
        public void handle(ArcPath arcPath) {
            float curtime = Engine.getInstance().getCurTime();

            float lerptime = (curtime - mAttackStart) / mAttackTime;

            if (lerptime >= 1f) {
                mUpdater.clear();
                return;
            }

            arcPath.setSpawnOffset(lerptime);
        }

        @Override
        public void handle(int id, float pathPoint, Transform3D transform) {
            transform.rotateDeg(pathPoint * mRotx, pathPoint * mRoty, pathPoint * mRotz);
        }
    }

    private final List<Reference<ArcPath>> mPaths = new ArrayList<>();
    private Reference<ArcUpdater> mUpdater = null;

    /**
     * Show a visual effect representing an attack.
     *
     * @param defender defending building to attack
     * @param attackTime how long should the effect last
     */
    public void attackEffect(Building defender, float attackTime) {
        if (Reference.isValid(mUpdater)) {
            mUpdater.clear();
        }

        if (mPaths.isEmpty()) {
            checkPaths();
        }

        HexagonTile defenderTile = defender.getTile();
        HexagonTile tile = getTile();

        if (defenderTile == null || tile == null) {
            return;
        }

        mUpdater = new Reference<>(new ArcUpdater(attackTime));

        List<HexagonTile> tiles = defender.getClaimedTiles().stream().collect(Collectors.toList());
        Collections.shuffle(tiles);

        int o = -2;

        Vector3f cartesianDelta = new Vector3f();
        Vector2f axialCoords = new Vector2f();

        for (Reference<ArcPath> pathRef : mPaths) {
            if (!Reference.isValid(pathRef)) {
                continue;
            }

            ArcPath path = pathRef.get();

            HexagonTile targetTile = defenderTile;

            if (++o >= 0) {
                if (o >= tiles.size() || tiles.get(o) == defenderTile) {
                    continue;
                }

                targetTile = tiles.get(o);
            }

            axialCoords.set(targetTile.getQ() - tile.getQ(), targetTile.getR() - tile.getR());

            TransformHex.axialToCartesian(
                    axialCoords,
                    targetTile.getSurfaceHeight() - targetTile.getHeight(),
                    cartesianDelta);

            path.setObjGap(1f);
            path.setUpdater(mUpdater.cast(IPathUpdater.class));
            path.setArcHandler(mUpdater.cast(IArcHandler.class));

            path.getPosStart().set(0, 0, 0);
            path.getPosTarget().set(cartesianDelta);

            float amplitude =
                    (targetTile.distTo(tile.getQ(), tile.getR()) + (float) Math.random() * 2) * 0.4f
                            + 0.2f;

            path.setAmplitude(amplitude);
        }
    }

    private static final int DIE_SIDES = 100;

    /**
     * Gets value of attacking, this is the value used to determine if the attack succeeds. The
     * larger it is the higher the chance of an attack succeeding.
     *
     * @param opponent the opponent
     * @return the attack val
     */
    private float getAttackVal(Building opponent) {
        HexagonTile myTile = getTile();
        HexagonTile opponentTile = opponent.getTile();

        float heightDelta;

        if (myTile != null && opponentTile != null) {
            heightDelta = myTile.getHeight() - opponentTile.getHeight();
        } else {
            heightDelta = 0;
        }

        Player p = getOwner();

        if (p != null) {
            PlayerConfig cfg = p.getConfig();

            if (cfg != null) {
                heightDelta *= cfg.getAttackHeightMul();
            }
        }

        return getAttack().getValue() + heightDelta;
    }

    /**
     * Calculates the odds of winning an attack against another building.
     *
     * @param opponent the opponent we are attacking
     * @return the odds of winning
     */
    public float calculateAttackOdds(Building opponent) {
        // Get the attacker and defender's stats.
        double attack = getAttackVal(opponent);
        double defence = opponent.getDefence().getValue();

        // We use doubles for precision reasons. Small values would get distorted otherwise.
        double accum = 0;

        for (int i = 0; i < DIE_SIDES; i++) {
            accum +=
                    (Math.pow((double) (i + 1) / DIE_SIDES, attack)
                                    - Math.pow((double) i / DIE_SIDES, attack))
                            * Math.pow((double) i / DIE_SIDES, defence);
        }

        return (float) accum;
    }

    /**
     * Attack an opponent building.
     *
     * <p><b> Has no effect other than the basic calculations (i.e. it does not transfer ownership
     * of Buildings). </b>
     *
     * <p>There is a chance this will either fail or succeed, influenced by the attack stat of the
     * attacking building and the defence stats of the opponent building.
     *
     * @param opponent The building to attack.
     * @return Whether the attack was successful or not.
     */
    public boolean attack(Building opponent) {
        // Get the attacker and defender's stats.
        float attack = getAttackVal(opponent);
        float defence = opponent.getDefence().getValue();

        // Stores the highest result of rolling a dice a set number of times, defined by the attack
        // stat.
        int highestAttack = 0;
        // Stores the highest result of rolling a dice a set number of times, defined by the defence
        // stat.
        int highestDefence = 0;

        // Roll a die a number of times defined by the attack stat.
        for (int i = 0; i <= attack; i++) {
            int value = (int) (Math.random() * DIE_SIDES + 1);
            // Store the highest value achieved.
            if (value > highestAttack) {
                highestAttack = value;
            }
        }

        // Roll a die a number of times defined by the defence stat.
        for (int i = 0; i <= defence; i++) {
            int value = (int) (Math.random() * DIE_SIDES + 1);
            // Store the highest value achieved.
            if (value > highestDefence) {
                highestDefence = value;
            }
        }

        // Check to see who has the highest value, and won.
        return highestAttack > highestDefence;
    }

    /**
     * Get a {@link Set} of opponent {@link Building}s that neighbour our tiles.
     *
     * @return An ArrayList of opponent Buildings that can be attacked.
     */
    public Set<Building> getAttackableBuildings() {
        if (mAttackableTiles.isEmpty()) {
            generateTileLists();
        }

        HashSet<Building> attackableBuildings = new HashSet<>();

        Player owner = getOwner();

        mAttackableTiles.forEach(
                (tile) -> {
                    Building b = tile.getClaimedBy();
                    if (b != null && b.getOwner() != owner) {
                        attackableBuildings.add(b);
                    }
                });

        return attackableBuildings;
    }

    /**
     * Get a random attackable opponent {@link Building}.
     *
     * @return An opponent Building that can be attacked from this Building; otherwise {@code null}.
     */
    public Building getRandomAttackableBuilding() {
        List<Building> buildings = new ArrayList<Building>(getAttackableBuildings());
        if (buildings.size() == 0) return null;

        int index = mRandom.nextInt(buildings.size());
        return buildings.get(index);
    }

    /**
     * Get a {@link List} of tiles that we can attack.
     *
     * @return a {@link List} of tiles that can be attacked.
     */
    public List<HexagonTile> getAttackableTiles() {
        if (mAttackableTiles.isEmpty()) {
            generateTileLists();
        }

        return mAttackableTiles;
    }

    /**
     * Get whether the building is action locked, i.e. should not have any actions happening to it.
     *
     * @return {@code true} if building is action locked, {@code false} otherwise.
     */
    public boolean isActionLocked() {
        return isTimeActionLocked() || mServerActionLocked;
    }

    /**
     * Get whether the building is action locked by time, i.e. should definitely not have any
     * actions happening to it.
     *
     * @return {@code true} if building is action locked, {@code false} otherwise.
     */
    public boolean isTimeActionLocked() {
        return mActionLockTime.get() > Engine.getInstance().getCurTime();
    }

    /**
     * Get whether the target {@link Building} is within attackable range from the Building.
     *
     * @param target The Building to attack.
     * @return {@code true} if the target is within attackable distance, otherwise {@code false}.
     */
    public boolean isBuildingAttackable(Building target) {
        return getAttackableBuildings().contains(target);
    }

    /**
     * Get a {@link HashSet} of {@link HexagonTile}s surrounding this building that are currently
     * able to have {@link Building}s placed on them.
     *
     * @return Nearby HexagonTiles able to have Buildings placed on them.
     */
    public Set<HexagonTile> getBuildableTiles() {

        HashSet<HexagonTile> buildableTiles = new HashSet<>();

        HexagonMap map = getMap();
        if (map == null) {
            return buildableTiles;
        }

        if (mPlaceableTiles.isEmpty()) {
            generateTileLists();
        }

        ArrayList<HexagonTile> tilesAround = new ArrayList<>();

        mPlaceableTiles.forEach(
                (tile) -> {
                    if (tile.isClaimed() || tile.hasBuilding()) {
                        return;
                    }

                    map.getTilesInRadius(tile, 1, false, tilesAround);

                    for (HexagonTile tileAround : tilesAround) {
                        if (tileAround.hasBuilding()) {
                            return;
                        }
                    }

                    buildableTiles.add(tile);
                });

        return buildableTiles;
    }

    /**
     * Get the {@link HexagonTile} the {@link Building} is on.
     *
     * @return The tile the building is on, or {@code null}.
     */
    public HexagonTile getTile() {
        HexagonMap map = getMap();
        if (map == null) {
            return null;
        }

        Vector3i position = getPosition();

        if (position != null) {
            HexagonTile tile = map.getTile(position.x(), position.y());
            mPosition = position;
            return tile;
        }

        return null;
    }

    /**
     * Get the current axial coordinates of the building using the {@link GameObject}'s {@link
     * TransformHex}.
     *
     * @return A 3d-vector of integers containing the x, y and z position of the building, or {@code
     *     null}.
     */
    private Vector3i getPosition() {

        if (mPosition != null) {
            return mPosition;
        }

        TransformHex tranform = getGameObject().getTransform(TransformHex.class);

        if (tranform == null) {
            return null;
        }

        Vector3f floatPosition = new Vector3f();
        tranform.getLocalPosition(floatPosition);

        Vector3i integerPosition = new Vector3i();
        integerPosition.set(
                (int) floatPosition.x(), (int) floatPosition.y(), (int) floatPosition.z());

        return integerPosition;
    }

    /**
     * Get the {@link HexagonMap} being used.
     *
     * @return The map, if it exists, or {@code null}.
     */
    private HexagonMap getMap() {
        return Reference.isValid(mMap) ? mMap.get() : null;
    }

    /**
     * Get the ID of the owner of the building.
     *
     * @return The ID of the owner.
     */
    public int getOwnerId() {
        return getNetworkObject().getOwnerId();
    }

    /**
     * Get the {@link Player} which owns the {@link Building}.
     *
     * @return The owning player, or {@code null}.
     */
    public Player getOwner() {
        if (getNetworkObject() == null) return null;
        return getOwner(getNetworkObject().getOwnerId());
    }

    /**
     * Get the {@link Player} which owns the {@link Building}.
     *
     * @param ownerId target owner ID
     * @return The owning player, or {@code null}.
     */
    private Player getOwner(int ownerId) {
        SingletonStore singletons = getNetworkObject().getNetworkManager().getIdSingletons(ownerId);

        if (singletons == null) {
            return null;
        }

        return singletons.get(Player.class);
    }

    /**
     * Get whether the building is a capital.
     *
     * @return Whether the building is a capital.
     */
    public boolean isCapital() {
        return mIsCapital.get();
    }

    /**
     * Set the building to be a capital.
     *
     * <p>By default, buildings are not capitals.
     *
     * @param isCapital Whether the building should be capital.
     */
    public void setCapital(boolean isCapital) {
        mIsCapital.set(isCapital);
    }

    /**
     * Will generate the cost it takes to attack <b>from</b> this Building.
     *
     * @return The cost to attack from this Building.
     */
    public int getAttackCost() {

        // Base cost
        int cost = 20;

        // Update cost on different stats
        cost += (mDefence.getLevel() * 3);
        cost += (mAttack.getLevel() * 3);
        cost += (mTokenGeneration.getLevel());

        if (isCapital()) {
            cost += 10;
        }

        return cost;
    }

    /**
     * Initialise a {@link SyncStat} via specifying a {@link StatType}. The SyncStat will take on
     * the properties of the StatType, and be stored in {@link #mStats} under the StatType.
     *
     * @param stat The SyncStat.
     * @param type The type of stat the SyncStat should be.
     */
    private void initiliseStat(SyncStat stat, StatType type) {
        // Initialise the SyncStat.
        stat.initialise(type);
        // Store the stat.
        storeStat(type, stat);
    }

    /**
     * Store a {@link SyncStat} in {@link #mStats} using a {@link StatType} as the key.
     *
     * @param type The type of stat to be stored.
     * @param stat The relevant SyncStat.
     */
    private void storeStat(StatType type, SyncStat stat) {
        if (type == null || stat == null) {
            log.warning("Unable to store stat: type or SyncStat is null.");
            return;
        }
        mStats.put(type, stat);
    }

    /**
     * Get the {@link SyncStat} stored in {@link #mStats} under the relevant type.
     *
     * @param type The type of the desired stat.
     * @return The SyncStat, otherwise {@code null}.
     */
    public SyncStat getStat(StatType type) {
        if (type == null) {
            log.warning("Unable to get stat: type is null.");
            return null;
        }
        return mStats.get(type);
    }

    /**
     * Get an {@link ArrayList} of non-fixed {@link SyncStat}s. These stats can theoretically be
     * upgraded.
     *
     * @return An ArrayList of SyncStats which are not fixed at one value.
     */
    public ArrayList<SyncStat> getStats() {
        ArrayList<SyncStat> stats = new ArrayList<SyncStat>();

        for (SyncStat stat : mStats.values()) {
            StatType type = stat.getType();
            if (type != null && stat.isFixed() == false) {
                stats.add(stat);
            }
        }

        return stats;
    }

    /**
     * Get an {@link ArrayList} of {@link SyncStat}s that can currently be upgraded.
     *
     * @return An ArrayList of SyncStats that can currently be upgraded.
     */
    public ArrayList<SyncStat> getUpgradeableStats() {
        ArrayList<SyncStat> stats = new ArrayList<>();

        for (SyncStat stat : mStats.values()) {
            if (stat.isUpgradeable()) {
                stats.add(stat);
            }
        }

        return stats;
    }

    /**
     * Gets the stats to display in the shop.
     *
     * @return the shop stats
     */
    public List<SyncStat> getShopStats() {
        return getShopStatTypes().stream().map(mStats::get).collect(Collectors.toList());
    }

    /**
     * Set the sell price.
     *
     * @param price The price.
     */
    public void setSellPrice(int price) {
        mSellPrice.set(price);
    }

    /**
     * Get the sell price.
     *
     * @return The sell price.
     */
    public int getSellPrice() {
        return mSellPrice.get();
    }

    /** Remove this building from the game (calls {@link GameObject#destroy()}). */
    public void remove() {
        // Request that the entire building GameObject should be destroyed.
        getGameObject().destroy();
    }

    /**
     * Destroy the Building. To correctly trigger this, please call {@link #remove()}.
     *
     * <ul>
     *   <li>Removes the Building from the owner {@link Player}'s list of owned Buildings.
     *   <li>Removes any links to any {@link HexagonTile}s.
     * </ul>
     */
    @Override
    protected void onDestroy() {
        Player owner = getOwner();

        if (owner != null) {
            // Remove the ownership of the building from the owner.
            getOwner().removeOwnership(this);
        }

        HexagonTile tile = getTile();

        if (tile != null && getNetworkObject().isServer()) {
            // Remove the building from the tile.
            getTile().setBuilding(null);

            // Remove any claims.
            for (HexagonTile hexagonTile : mClaimedTiles) {
                // Make sure it's our tile. It should always be our tile, but better safe than
                // sorry.
                if (hexagonTile.getClaimedBy() == this) {
                    hexagonTile.removeClaim();
                }
            }
        }

        mClaimedTiles.clear();
        cleanupTileLists();
    }
}
