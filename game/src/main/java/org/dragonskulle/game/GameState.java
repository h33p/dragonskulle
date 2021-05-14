/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ServerEvent;
import org.dragonskulle.network.components.requests.ServerEvent.EventRecipients;
import org.dragonskulle.network.components.requests.ServerEvent.EventTimeframe;
import org.dragonskulle.network.components.sync.INetSerializable;
import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncFloat;
import org.dragonskulle.network.components.sync.SyncInt;

/**
 * Stores networked game state.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class GameState extends NetworkableComponent implements IOnAwake, IFixedUpdate {
    /** Event invoked on game end. */
    private static class GameEndEventData implements INetSerializable {
        /** Winner of the game. */
        private int mWinnerId;

        @Override
        public void serialize(DataOutput stream, int clientId) throws IOException {
            stream.writeInt(mWinnerId);
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mWinnerId = stream.readInt();
        }
    }

    /**
     * A networked event that allows playing of a certain clip from {@link
     * org.dragonskulle.game.GameUIAppearance.AudioFiles}.
     */
    public static class InvokeAudioEvent implements INetSerializable {

        /** Constructor. */
        public InvokeAudioEvent() {}

        /**
         * Constructor.
         *
         * @param sound the sound to play
         */
        public InvokeAudioEvent(GameUIAppearance.AudioFiles sound) {
            mSoundId = sound;
        }

        @Getter
        @Accessors(prefix = "m")
        private GameUIAppearance.AudioFiles mSoundId;

        @Override
        public void serialize(DataOutput stream, int __) throws IOException {
            stream.writeInt(mSoundId.ordinal());
        }

        @Override
        public void deserialize(DataInput stream) throws IOException {
            mSoundId = GameUIAppearance.AudioFiles.get(stream.readInt());
        }
    }

    /** Handler for game end event. */
    public static interface IGameEndEvent {
        /**
         * Handle the game end.
         *
         * @param winnerId ID of the winner's network ID.
         */
        void handle(int winnerId);
    }

    /** Get an initial config. */
    @Getter private GameConfig mConfig = GameConfig.getDefaultConfig();

    @Getter private final SyncInt mNumPlayers = new SyncInt(0);
    @Getter private final SyncInt mNumCapitalsStanding = new SyncInt(0);
    @Getter private final SyncFloat mStartTime = new SyncFloat();

    @Getter private SyncBool mInGame = new SyncBool(true);

    private transient ServerEvent<GameEndEventData> mGameEndEvent;

    private final List<Reference<IGameEndEvent>> mGameEndListeners = new ArrayList<>();

    @Override
    protected void onNetworkInitialise() {
        // Retrieve the config again, in case its been updated.
        mConfig = GameConfig.getDefaultConfig();

        mGameEndEvent =
                new ServerEvent<>(
                        new GameEndEventData(),
                        (data) -> {
                            mGameEndListeners.stream()
                                    .filter(Reference::isValid)
                                    .map(Reference::get)
                                    .forEach(e -> e.handle(data.mWinnerId));
                        },
                        EventRecipients.ALL_CLIENTS,
                        EventTimeframe.INSTANT);

        Scene.getActiveScene().registerSingleton(this);
    }

    @Override
    public void onAwake() {
        mStartTime.set(Engine.getInstance().getCurTime());
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        if (mNumCapitalsStanding.get() > 1 || !getNetworkObject().isServer()) {
            return;
        }

        Player winner =
                getNetworkManager()
                        .getNetworkObjects()
                        .map(c -> c.getGameObject().getComponent(Player.class))
                        .filter(Reference::isValid)
                        .map(Reference::get)
                        .filter(c -> !c.hasLost())
                        .findFirst()
                        .orElse(null);

        int winnerId = winner != null ? winner.getNetworkObject().getOwnerId() : -1000;

        mInGame.set(false);
        mGameEndEvent.invoke((data) -> data.mWinnerId = winnerId);

        setEnabled(false);
    }

    @Override
    public void onDestroy() {}

    /**
     * Register the game end listener.
     *
     * <p>This listener will be invoked whenever the game end event is invoked.
     *
     * @param e the event listener.
     */
    public void registerGameEndListener(Reference<IGameEndEvent> e) {
        if (Reference.isValid(e)) {
            mGameEndListeners.add(e);
        }
    }

    /**
     * Retrieve current global inflation level.
     *
     * @return float representing inflation. Value of 1 means no inflation, values lower than 1
     *     represent deflation.
     */
    public float getGlobalInflation() {
        NetworkManager networkManager = getNetworkManager();
        if (networkManager == null) return 1;
        float deltaTime = networkManager.getServerTime() - mStartTime.get();
        return (float) Math.pow(mConfig.getGlobal().getInflation(), deltaTime);
    }

    /**
     * Gets the {@link GameConfig} singleton from the scene if it exists, otherwise {@code null}.
     *
     * @return the scene config
     */
    public static GameConfig getSceneConfig() {
        GameState state = Scene.getActiveScene().getSingleton(GameState.class);

        if (state != null) {
            return state.getConfig();
        }

        return null;
    }
}
