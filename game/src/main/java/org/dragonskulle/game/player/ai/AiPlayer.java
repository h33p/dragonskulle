/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.List;
import java.util.Random;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameConfig;
import org.dragonskulle.game.GameConfig.AiConfig;
import org.dragonskulle.game.GameState;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.PredefinedBuildings;

/**
 * This {@code abstract} class contains all the needed methods and variables which are needed by all
 * AI Player to actually play.
 *
 * @author DragonSkulle
 */
@Log
public abstract class AiPlayer extends Component implements IFixedUpdate, IOnStart {

    /** The time since the last check if the AI player can play. (Start at 0). */
    protected float mTimeSinceStart;
    /** Will hold how long the AI player has to wait until playing. */
    protected float mTimeToWait;

    /** AI configuration values used for timeout configuration. */
    protected AiConfig mConfig;

    /** The Random Number Generator. */
    protected Random mRandom = new Random();

    /** This is the {@link Reference} to the {@link Player} which is used by this AI Player. */
    protected Reference<Player> mPlayer;

    /** Basic Constructor. */
    public AiPlayer() {}

    @Override
    public void onStart() {

        // Sets up all unitialised variables
        mPlayer = getGameObject().getComponent(Player.class);
        mTimeSinceStart = 0;
        createNewRandomTime();
    }

    /**
     * This will check to see whether the AI Player can actually play or not.
     *
     * @param deltaTime The time since the last fixed update
     * @return A boolean to say whether the AI player can play
     */
    private boolean shouldPlayGame(float deltaTime) {
        mTimeSinceStart += deltaTime;

        // Checks to see how long since last time AI player played and if longer than how long they
        // have to wait.
        if (mTimeSinceStart >= mTimeToWait) {
            mTimeSinceStart = 0;
            createNewRandomTime(); // Creates new Random Number until next move
            return true;
        }

        return false;
    }

    /** This will set how long the AI player has to wait until they can play. */
    protected void createNewRandomTime() {
        AiConfig cfg = getConfig();

        mTimeToWait =
                mRandom.nextInt() % (cfg.getUpperBoundTime() - cfg.getLowerBoundTime())
                        + cfg.getLowerBoundTime();
    }

    /**
     * This will get the config value for the AI Player.
     *
     * @return The {@link AiConfig} to be used
     */
    protected AiConfig getConfig() {
        if (mConfig != null) {
            return mConfig;
        }

        GameConfig cfg = GameState.getSceneConfig();

        if (cfg != null && cfg.getAi().size() > 0) {
            int index = mRandom.nextInt(cfg.getAi().size());
            mConfig = cfg.getAi().get(index);

        } else {
            mConfig = new AiConfig();
        }

        return mConfig;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        // Ensure the AI only runs on the server, and if it is its time to run.
        if (!shouldPlayGame(deltaTime)) return;

        // Ensure the player exists and hasn't lost.
        Player player = getPlayer();
        if (player == null || player.gameEnd() || player.getNumberOfOwnedBuildings() == 0) return;

        simulateInput();
    }

    /**
     * This will simulate the action to be done by the AI player. This will only be called when we
     * have not lost. For the base class this will be done using probability
     */
    protected abstract void simulateInput();

    /**
     * Get a random {@link BuildingDescriptor} for a building that the {@link Player} can afford.
     *
     * @return A {@link BuildingDescriptor} the Player can afford, or {@code null} if they cannot
     *     afford a building.
     */
    protected BuildingDescriptor getRandomBuildingType() {
        List<BuildingDescriptor> options = PredefinedBuildings.getPurchasable(getPlayer());
        // Test if they can afford to build anything.
        if (options.size() == 0) return null;

        int optionIndex = mRandom.nextInt(options.size());
        return options.get(optionIndex);
    }

    /**
     * Gets the player.
     *
     * @return The player.
     */
    protected Player getPlayer() {
        Player player = mPlayer.get();
        if (player == null) {
            log.severe("Reference to mPlayer is null!");
        }
        return player;
    }
}
