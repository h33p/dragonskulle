/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import lombok.extern.java.Log;
import org.dragonskulle.components.lambda.LambdaFrameUpdate;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.ServerNetworkManager;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;

@Log
public class PlayerStats implements UIManager.IUIBuildHandler {
    private Reference<NetworkObject> mPlayerReference;
    private final ServerNetworkManager mServerManager;
    private Player mPlayer;

    public PlayerStats(
            Reference<NetworkObject> networkObjectReference, ServerNetworkManager mServerManager) {
        this.mPlayerReference = networkObjectReference;
        this.mServerManager = mServerManager;
    }

    @Override
    public void handleUIBuild(GameObject go) {
        final String[] content = {""};
        final Reference[] contents = new Reference[] {new Reference<UIText>(null)};
        final int[] id = {-555};
        if (Reference.isValid(mPlayerReference)) {
            NetworkObject networkObject = mPlayerReference.get();
            id[0] = networkObject.getId();
            mPlayer =
                    (Player)
                            networkObject.getNetworkableComponents().stream()
                                    .filter(Reference::isValid)
                                    .map(Reference::get)
                                    .filter(s -> s instanceof Player)
                                    .findFirst()
                                    .orElse(null);
            if (mPlayer != null) {
                content[0] =
                        String.format(
                                "Player %d\nTokens: %d\nBuildings: %d\nIs Playing: %b",
                                id[0],
                                mPlayer.getTokens().get(),
                                mPlayer.getNumberOfOwnedBuildings(),
                                !mPlayer.hasLost());
            } else {
                log.info("mplayer is null");
            }
        } else {
            log.info("Player reference is invalid");
        }
        UITextRect text = new UITextRect(content[0]);
        LambdaFrameUpdate updater =
                new LambdaFrameUpdate(
                        (delta) -> {
                            if (mServerManager != null
                                    && mServerManager
                                    .getGameState()
                                    .equals(
                                            ServerNetworkManager.ServerGameState
                                                    .IN_PROGRESS)
                                    && Reference.isValid(mPlayerReference)) {
                                if (mPlayer != null) {
                                    String newContent =
                                            String.format(
                                                    "Player %d\nTokens: %d\nBuildings: %d\nIs Playing: %b",
                                                    id[0],
                                                    mPlayer.getTokens().get(),
                                                    mPlayer.getNumberOfOwnedBuildings(),
                                                    !mPlayer.hasLost());
                                    if (Reference.isValid(contents[0])) {
                                        ((UIText) contents[0].get()).setText(newContent);
                                    } else {
                                        contents[0] = text.getLabelText();
                                    }
                                }
                            }
                        });
        go.addComponent(text);
        contents[0] = text.getLabelText();
        go.addComponent(updater);
    }
}
