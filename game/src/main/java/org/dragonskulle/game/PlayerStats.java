/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Transform;
import org.dragonskulle.components.lambda.LambdaFrameUpdate;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.ServerNetworkManager;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;

@Log
@Accessors(prefix = "m")
public class PlayerStats implements UIManager.IUIBuildHandler {
    private Reference<Player> mPlayerReference;
    private final ServerNetworkManager mServerManager;
    @Getter
    private final Integer mId;
    @Getter
    private final int mFakeId;

    public PlayerStats(ServerNetworkManager mServerManager, Integer id, int mFakeId) {
        this.mServerManager = mServerManager;
        this.mId = id;
        this.mFakeId = mFakeId;
    }

    private void ensurePlayerReference() {
        if (mId != null && !Reference.isValid(mPlayerReference)) {
            Player playerReference = mServerManager.getIdSingletons(mId).get(Player.class);
            if (playerReference != null) mPlayerReference = playerReference.getReference(Player.class);
        }
    }

    @Override
    public void handleUIBuild(GameObject go) {
        UIRenderable bg = new UIRenderable(UIManager.getInstance().getAppearance().getHintTexture());
        go.buildChild("player_text", new TransformUI(true), self -> {
            self.addComponent(bg);
            //labels are in order, playerTextAndStatus, TokenText, Building Counter
            UIText[] labels;
            ensurePlayerReference();
            if (Reference.isValid(mPlayerReference)) {
                Player player = mPlayerReference.get();
                labels = new UIText[]{new UIText(getPlayerIdAndStatus(player)), new UIText(getTokens(player)), new UIText(getNumberOfBuildings(player))};
            } else {
                labels = new UIText[]{new UIText(""), new UIText(""), new UIText("")};
            }
            final TransformUI textTran = self.getTransform(TransformUI.class);
            UIManager.getInstance().buildVerticalUi(self, 0.2f, 0.1f, 0.9f, labels);
            if (mId != null) {
                LambdaFrameUpdate updater =
                        new LambdaFrameUpdate(
                                (delta) -> {
                                    ensurePlayerReference();
                                    if (mServerManager != null
                                            && mServerManager
                                            .getGameState()
                                            .equals(ServerNetworkManager.ServerGameState.IN_PROGRESS)
                                    ) updateLabels(labels);
                                });
                self.addComponent(updater);
            }
        });

    }

    private void updateLabels(UIText[] labels) {
        if (Reference.isValid(mPlayerReference)) {
            Player player = mPlayerReference.get();
            setLabel(labels, new String[]{getPlayerIdAndStatus(player), getTokens(player), getNumberOfBuildings(player)});
        }
    }

    private String getNumberOfBuildings(Player player) {
        return "Owned Buildings: " + player.getNumberOfOwnedBuildings();
    }

    private String getTokens(Player player) {
        return "Tokens: " + player.getTokens().get();
    }

    private String getPlayerIdAndStatus(Player player) {
        return "Player " + getFakeId() + ", " + (player.hasLost() ? "is not playing." : "is playing.");
    }

    private void setLabel(UIText[] labels, String[] contents) {
        if (labels.length != contents.length) return;
        for (int i = 0; i < labels.length; i++) {
            labels[i].setText(contents[i]);
        }
    }
}


