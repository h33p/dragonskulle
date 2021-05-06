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
import org.dragonskulle.ui.*;
import org.dragonskulle.utils.MathUtils;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

@Log
@Accessors(prefix = "m")
public class PlayerStats implements UIManager.IUIBuildHandler {
    private Reference<Player> mPlayerReference;
    private final ServerNetworkManager mServerManager;
    @Getter private final Integer mId;
    @Getter private final int mFakeId;
    private Vector4f mDidSetOverlay;

    public PlayerStats(ServerNetworkManager mServerManager, Integer id, int mFakeId) {
        this.mServerManager = mServerManager;
        this.mId = id;
        this.mFakeId = mFakeId;
    }

    private void ensurePlayerReference() {
        if (mId != null && !Reference.isValid(mPlayerReference)) {
            Player playerReference = mServerManager.getIdSingletons(mId).get(Player.class);
            if (playerReference != null)
                mPlayerReference = playerReference.getReference(Player.class);
        }
    }

    @Override
    public void handleUIBuild(GameObject go) {
        go.getTransform(TransformUI.class).setMaintainAspect(false);
        UIRenderable bg =
                new UIRenderable(UIManager.getInstance().getAppearance().getHintTexture());
        go.addComponent(bg);
        // labels are in order, playerTextAndStatus, TokenText, Building Counter
        UIText[] labels;
        ensurePlayerReference();
        setOverlay(bg);
        if (Reference.isValid(mPlayerReference)) {
            Player player = mPlayerReference.get();
            labels =
                    new UIText[] {
                        new UIText(getPlayerIdAndStatus(player)),
                        new UIText(getTokens(player)),
                        new UIText(getNumberOfBuildings(player))
                    };
        } else {
            labels = new UIText[] {new UIText(""), new UIText(""), new UIText("")};
        }
        UIManager.getInstance().buildVerticalUi(go, 0.35f, 0f, 1.35f, labels);
        if (mId != null) {
            LambdaFrameUpdate updater =
                    new LambdaFrameUpdate(
                            (delta) -> {
                                ensurePlayerReference();
                                if (mDidSetOverlay == null) setOverlay(bg);
                                if (mServerManager != null
                                        && mServerManager
                                                .getGameState()
                                                .equals(
                                                        ServerNetworkManager.ServerGameState
                                                                .IN_PROGRESS)) updateLabels(labels);
                            });
            go.addComponent(updater);
        }
    }

    private void setOverlay(UIRenderable bg) {
        if (Reference.isValid(mPlayerReference)) {
            Vector3fc v =
                    MathUtils.blendColour(
                            mPlayerReference.get().getPlayerColour().get(),
                            new Vector3f(0.823f, 0.490f, 0.172f));
            mDidSetOverlay = ((UIMaterial) bg.getMaterial()).mColour.set(v, 0.8f);
        }
    }

    private void updateLabels(UIText[] labels) {
        if (Reference.isValid(mPlayerReference)) {
            Player player = mPlayerReference.get();
            setLabel(
                    labels,
                    new String[] {
                        getPlayerIdAndStatus(player),
                        getTokens(player),
                        getNumberOfBuildings(player)
                    });
        }
    }

    private String getNumberOfBuildings(Player player) {
        return "Owned Buildings: " + player.getNumberOfOwnedBuildings();
    }

    private String getTokens(Player player) {
        return "Tokens: " + player.getTokens().get();
    }

    private String getPlayerIdAndStatus(Player player) {
        return "Player "
                + getFakeId()
                + (player.hasLost() ? " is not playing." : "is still playing.");
    }

    private void setLabel(UIText[] labels, String[] contents) {
        if (labels.length != contents.length) return;
        for (int i = 0; i < labels.length; i++) {
            labels[i].setText(contents[i]);
        }
    }
}
