/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.ServerNetworkManager;
import org.dragonskulle.network.components.requests.ServerEvent;
import org.dragonskulle.network.components.requests.ServerEvent.EventRecipients;
import org.dragonskulle.network.components.requests.ServerEvent.EventTimeframe;
import org.dragonskulle.network.components.sync.INetSerializable;
import org.dragonskulle.network.components.sync.SyncInt;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;

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

    public void buildServerPlayerView() {
        UIManager.IUIBuildHandler[] playerInfos = buildPlayerInfos();
        // things for player thingys
        getGameObject()
                .buildChild(
                        "game_state",
                        new TransformUI(true),
                        (self) -> {
                            UIManager.getInstance().buildVerticalUi(self, 0.1f, 0f, 1f, playerInfos);
                            UIRenderable drawer =
                                    new UIRenderable(GameUIAppearance.getDrawerTexture());
                            TransformUI tran = self.getTransform(TransformUI.class);
                            tran.setMargin(0f, 0f, 0f, 0f);
                            tran.setPivotOffset(0f, 0f);
                            tran.setParentAnchor(0f, 0f);
                            self.addComponent(drawer);
                        });

    }

    private UIManager.IUIBuildHandler[] buildPlayerInfos() {
        NetworkManager networkManager = getNetworkManager();
        ServerNetworkManager serverManager = networkManager.getServerManager();
        Set<Integer> playerIds = serverManager.getNonHumanPlayerIds();
        playerIds.addAll(serverManager.getClients().stream().map(ServerClient::getNetworkID).collect(Collectors.toList()));
        UIManager.IUIBuildHandler[] playerInfoBox = new UIManager.IUIBuildHandler[mNumPlayers.get()];
        Iterator<Integer> playerIterator = playerIds.iterator();
        for (int j = 0; j < mNumPlayers.get(); j++) {
            try {
                Integer next = playerIterator.next();
                playerInfoBox[j] = new PlayerStats(serverManager, next);
            } catch (NoSuchElementException e) {
                playerInfoBox[j] = new PlayerStats(serverManager, null);
            }
        }
        return playerInfoBox;
    }
}
