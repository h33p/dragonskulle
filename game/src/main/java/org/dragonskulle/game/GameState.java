/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ServerEvent;
import org.dragonskulle.network.components.requests.ServerEvent.EventRecipients;
import org.dragonskulle.network.components.requests.ServerEvent.EventTimeframe;
import org.dragonskulle.network.components.sync.INetSerializable;
import org.dragonskulle.network.components.sync.SyncInt;

/**
 * Stores networked game state.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
public class GameState extends NetworkableComponent implements IOnAwake {
    private static class GameEndEventData implements INetSerializable {
        private int mWinnerId;

        @Override
        public void serialize(DataOutputStream stream, int clientId) throws IOException {
            stream.writeInt(mWinnerId);
        }

        @Override
        public void deserialize(DataInputStream stream) throws IOException {
            mWinnerId = stream.readInt();
        }
    }

    public static class InvokeAudioEvent implements INetSerializable {

        public InvokeAudioEvent() {}

        public InvokeAudioEvent(GameUIAppearance.AudioEvent sound) {
            mSoundId = sound;
        }

        @Getter
        @Accessors(prefix = "m")
        private GameUIAppearance.AudioEvent mSoundId;

        @Override
        public void serialize(DataOutputStream stream, int __) throws IOException {
            stream.writeInt(mSoundId.ordinal());
        }

        @Override
        public void deserialize(DataInputStream stream) throws IOException {
            mSoundId = GameUIAppearance.AudioEvent.get(stream.readInt());
        }
    }


    public static interface IGameEndEvent {
        void handle(int winnerId);
    }

    @Getter
    private final SyncInt mNumPlayers = new SyncInt(0);
    @Getter
    private final SyncInt mNumCapitalsStanding = new SyncInt(0);

    @Getter
    private boolean mInGame = true;

    private transient ServerEvent<GameEndEventData> mGameEndEvent;
    @Getter
    private transient ServerEvent<InvokeAudioEvent> mOwnerAudioEvent;
    @Getter
    private transient ServerEvent<InvokeAudioEvent> mGlobalAudioEvent;

    private final List<Reference<IGameEndEvent>> mGameEndListeners = new ArrayList<>();

    @Override
    protected void onNetworkInitialize() {
        mGameEndEvent =
                new ServerEvent<>(
                        new GameEndEventData(),
                        (data) -> {
                            mInGame = false;
                            mGameEndListeners.stream()
                                    .filter(Reference::isValid)
                                    .map(Reference::get)
                                    .forEach(e -> e.handle(data.mWinnerId));
                        },
                        EventRecipients.ALL_CLIENTS,
                        EventTimeframe.INSTANT);

        mOwnerAudioEvent =
                new ServerEvent<>(
                        new InvokeAudioEvent(),
                        (data) -> {
                            GameUIAppearance.getSource().playSound(data.getSoundId().getPath());
                        },
                        EventRecipients.OWNER,
                        EventTimeframe.INSTANT);

        mGlobalAudioEvent =
                new ServerEvent<>(
                        new InvokeAudioEvent(),
                        (data) -> {
                            GameUIAppearance.getSource().playSound(data.getSoundId().getPath());
                        },
                        EventRecipients.ACTIVE_CLIENTS,
                        EventTimeframe.INSTANT);
    }

    @Override
    public void onAwake() {
        Scene.getActiveScene().registerSingleton(this);
    }

    @Override
    public void onDestroy() {}

    public void endGame(int winnerId) {
        mGameEndEvent.invoke((data) -> data.mWinnerId = winnerId);
    }

    public void registerGameEndListener(Reference<IGameEndEvent> e) {
        if (Reference.isValid(e)) {
            mGameEndListeners.add(e);
        }
    }
}
