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
        String playerId = "";
        String tokens = "";
        String numberOfBuildings = "";
        String hasLost = "";
        String[] contents = {playerId, tokens, numberOfBuildings, hasLost};

        UITextRect rect = new UITextRect("");
        rect.setRectTexture(UIManager.getInstance().getAppearance().getHintTexture());
        final TransformUI tran = go.getTransform(TransformUI.class);
        tran.setMaintainAspect(false);
        tran.setMargin(0f, 0.08f);
        ensurePlayerReference();
        if (Reference.isValid(mPlayerReference)) {
            Player player = mPlayerReference.get();
            playerId = getId().toString();
            tokens = String.valueOf(player.getTokens().get());
            numberOfBuildings = String.valueOf(player.getNumberOfOwnedBuildings());
            hasLost = String.valueOf(player.hasLost());
        } else {
            log.info("mplayer is null");
        }
//
//        //create text elements for each
        UIText playerText = new UIText("Player " + playerId);
        UIText tokenText = new UIText("Tokens " + tokens);
        UIText buildingCounterText = new UIText("Owned Buildings " + numberOfBuildings);
        UIText hasLostText = new UIText("Has Lost " + hasLost);
//
        UIText[] labels = new UIText[]{playerText, tokenText, buildingCounterText, hasLostText};
        go.addComponent(rect);
        UIManager.getInstance().buildVerticalUi(go, 0.2f, 0f, 1f, labels);

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
                                        contents[0] = "Player " + getId().toString();
                                        contents[1] = "Tokens " + player.getTokens().get();
                                        contents[2] = "Owned Buildings " + player.getNumberOfOwnedBuildings();
                                        contents[3] = "Has Lost " + player.hasLost();
                                        setLabel(labels, contents);
                                    }
                                }
                            });
            go.addComponent(updater);
        }
    }

    private void setLabel(UIText[] labels, String[] contents) {
        if (labels.length != contents.length) return;
        for (int i = 0; i < labels.length; i++) {
            labels[i].setText(contents[i]);
        }
    }
}


