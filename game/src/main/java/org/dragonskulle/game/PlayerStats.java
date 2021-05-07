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
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIManager;
import org.dragonskulle.ui.UIMaterial;
import org.dragonskulle.ui.UIRenderable;
import org.dragonskulle.ui.UIText;
import org.dragonskulle.utils.MathUtils;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/** This class is used to build an updating box, filled with information about a certain player. */
@Log
@Accessors(prefix = "m")
public class PlayerStats implements UIManager.IUIBuildHandler {
    private Reference<Player> mPlayerReference;
    private final ServerNetworkManager mServerManager;
    @Getter private final Integer mId;
    @Getter private final int mFakeId;
    private Vector4f mDidSetOverlay;

    /**
     * Constructor.
     *
     * @param mServerManager the server manager
     * @param id the id of the player, used to find it's {@link Player} component
     * @param mFakeId the id displayed in the pane
     */
    public PlayerStats(ServerNetworkManager mServerManager, Integer id, int mFakeId) {
        this.mServerManager = mServerManager;
        this.mId = id;
        this.mFakeId = mFakeId;
    }

    /** Ensures that a valid player reference is stored in {@link this#mPlayerReference}. */
    private void ensurePlayerReference() {
        if (mId != null && !Reference.isValid(mPlayerReference)) {
            Player playerReference = mServerManager.getIdSingletons(mId).get(Player.class);
            if (playerReference != null) {
                mPlayerReference = playerReference.getReference(Player.class);
            }
        }
    }

    @Override
    public void handleUIBuild(GameObject go) {
        go.getTransform(TransformUI.class).setMaintainAspect(false);
        UIRenderable bg =
                new UIRenderable(UIManager.getInstance().getAppearance().getHintTexture());
        go.addComponent(bg);
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
                                                                .IN_PROGRESS)) {
                                    updateLabels(labels);
                                }
                            });
            go.addComponent(updater);
        }
    }

    /**
     * Sets the result of {@link Player#getCapital()} as an overlay on top of the renderable.
     *
     * @param bg the bg
     */
    private void setOverlay(UIRenderable bg) {
        if (Reference.isValid(mPlayerReference)) {
            Vector3fc v =
                    MathUtils.blendColour(
                            mPlayerReference.get().getPlayerColour().get(),
                            new Vector3f(1f, 1f, 1f));
            mDidSetOverlay = ((UIMaterial) bg.getMaterial()).mColour.set(v, 0.8f);
        }
    }

    /**
     * Update the information about the player, and display it on the screen.
     *
     * @param labels the labels to be updated
     */
    private void updateLabels(UIText[] labels) {
        if (Reference.isValid(mPlayerReference)) {
            Player player = mPlayerReference.get();
            setLabels(
                    labels,
                    new String[] {
                        getPlayerIdAndStatus(player),
                        getTokens(player),
                        getNumberOfBuildings(player)
                    });
        }
    }

    /**
     * Gets the text to be shown in the panel.
     *
     * @param player the player to get information about
     * @return the number of buildings the player owns, prefixed with a string
     */
    private String getNumberOfBuildings(Player player) {
        return "Owned Buildings: " + player.getNumberOfOwnedBuildings();
    }

    /**
     * Gets the text to be shown in the panel.
     *
     * @param player the player to get information about
     * @return the number of tokens the player has, prefixed with a string
     */
    private String getTokens(Player player) {
        return "Tokens: " + player.getTokens().get();
    }

    /**
     * Gets the text to be shown in the panel.
     *
     * @param player the player to get information about
     * @return the players id, and its game status
     */
    private String getPlayerIdAndStatus(Player player) {
        return "Player "
                + getFakeId()
                + (player.hasLost() ? " is not playing." : "is still playing.");
    }

    /**
     * Sets all labels about the player.
     *
     * @param labels the labels to be set
     * @param contents the contents to be set in its equivalently indexed {@code labels} array
     */
    private void setLabels(UIText[] labels, String[] contents) {
        if (labels.length != contents.length) return;
        for (int i = 0; i < labels.length; i++) {
            labels[i].setText(contents[i]);
        }
    }
}
