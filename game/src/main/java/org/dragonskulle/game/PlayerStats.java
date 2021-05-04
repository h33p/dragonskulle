/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.lambda.LambdaFrameUpdate;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.ServerNetworkManager;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.ui.UITextRect;

@Log
@Accessors(prefix = "m")
public class PlayerStats implements UIManager.IUIBuildHandler {
    private Reference<Player> mPlayerReference;
    private final ServerNetworkManager mServerManager;
    @Getter
    private final Integer mId;

    public PlayerStats(ServerNetworkManager mServerManager, Integer id) {
        this.mServerManager = mServerManager;
        this.mId = id;
    }

    private void ensurePlayerReference() {
        if (mId != null && !Reference.isValid(mPlayerReference)) {
            Player playerReference = mServerManager.getIdSingletons(mId).get(Player.class);
            if (playerReference != null) mPlayerReference = playerReference.getReference(Player.class);
        }
    }

    @Override
    public void handleUIBuild(GameObject go) {
        final String[] content = {""};
        final Reference[] contents = new Reference[]{new Reference<UIText>(null)};
        ensurePlayerReference();
        if (Reference.isValid(mPlayerReference)) {
            Player player = mPlayerReference.get();
            content[0] =
                    String.format(
                            "Player %d\nTokens: %d\nBuildings: %d\nIs Playing: %b",
                            getId(),
                            player.getTokens().get(),
                            player.getNumberOfOwnedBuildings(),
                            !player.hasLost());
        } else {
            log.info("mplayer is null");
        }

        UITextRect text = new UITextRect(content[0]);

        go.addComponent(text);
        contents[0] = text.getLabelText();
        if (mId != null) {
            LambdaFrameUpdate updater =
                    new LambdaFrameUpdate(
                            (delta) -> {
                                ensurePlayerReference();
                                if (mServerManager != null
                                        && mServerManager
                                        .getGameState()
                                        .equals(
                                                ServerNetworkManager.ServerGameState
                                                        .IN_PROGRESS)
                                        && Reference.isValid(mPlayerReference)) {
                                    if (Reference.isValid(mPlayerReference)) {
                                        Player player = mPlayerReference.get();
                                        String newContent =
                                                String.format(
                                                        "Player %d\nTokens: %d\nBuildings: %d\nIs Playing: %b",
                                                        getId(),
                                                        player.getTokens().get(),
                                                        player.getNumberOfOwnedBuildings(),
                                                        !player.hasLost());
                                        if (Reference.isValid(contents[0])) {
                                            ((UIText) contents[0].get()).setText(newContent);
                                        } else {
                                            contents[0] = text.getLabelText();
                                        }
                                    }
                                }
                            });
            go.addComponent(updater);
        }
    }
}
