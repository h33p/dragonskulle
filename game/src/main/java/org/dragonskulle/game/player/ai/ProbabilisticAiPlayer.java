/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.Player;

/**
 * This base class will allow AI players to be created and used throughout the game.
 *
 * @author DragonSkulle
 */
@Log
public class ProbabilisticAiPlayer extends AiPlayer {

    /** Probability of placing a new {@link Building}. */
    protected float mBuildProbability = 0.65f;
    /** Probability of upgrading an owned {@link Building}. */
    protected float mUpgradeProbability = 0.15f;
    /** Probability of attacking an opponent {@link Building}. */
    protected float mAttackProbability = 0.15f;
    /** Probability of selling an owned {@link Building}. */
    protected float mSellProbability = 0.05f;

    /** Used to handle events for building and attacking */
    public interface IHandleBuildingEvent {
        public boolean handleEvent(int index);
    }

    /** A Constructor for an AI Player */
    public ProbabilisticAiPlayer() {}

    @Override
    protected void simulateInput() {

        // TODO: Remove/formalise this.
        if (getPlayer().getNumberOfOwnedBuildings() == 0) {
            log.info("AI player has no buildings... Ending turn.");
            return;
        }

        // If only one building assumed that its capital
        if (getPlayer().getNumberOfOwnedBuildings() == 1) {
            // TODO: What if there are no valid places to put a building? Should it attack/upgrade
            // instead?
            log.info("AI Player has 1 building. Placing another building.");

            addBuilding();
            return;

        } else {
            log.info(
                    "AI: I have "
                            + getPlayer().getNumberOfOwnedBuildings()
                            + " buildings. Should be  more than  one");

            // Pick a random number to choose whether to place a building or to use a building
            float randomNumber = mRandom.nextFloat();

            // Choose an action to take.
            if (randomNumber <= mBuildProbability) {
                addBuilding();
            } else if (randomNumber <= mBuildProbability + mUpgradeProbability) {
                upgradeBuilding();
            } else if (randomNumber
                    <= mBuildProbability + mUpgradeProbability + mAttackProbability) {
                attack();
            } else if (randomNumber
                    <= mBuildProbability
                            + mUpgradeProbability
                            + mAttackProbability
                            + mSellProbability) {
                sell();
            } else {
                log.info("AI probabilites do not sum to one- no action performed.");
            }
        }
    }

    /**
     * A function which goes through and checks if you can run code
     *
     * @param lambdaMethod What to on a building
     * @return If the stuff is invoked on the server
     */
    public boolean checkBuilding(IHandleBuildingEvent lambdaMethod) {
        if (getPlayer() == null) {
            return false;
        }
        int index = mRandom.nextInt(getPlayer().getNumberOfOwnedBuildings());

        final int END = index;

        while (true) {
            boolean completed = lambdaMethod.handleEvent(index);
            if (completed) {
                return true;
            }
            index++;
            if (index >= getPlayer().getNumberOfOwnedBuildings()) {
                index = 0;
            }
            if (index == END) {

                return false;
            }
        }
    }

    /**
     * Pick and attempt to place a {@link Building}.
     *
     * @return Whether the attempt to pick and add a building was invoked.
     */
    private boolean addBuilding() {
        log.info("Placing Building");

        return checkBuilding(this::tryToAddBuilding);
    }

    /**
     * Will try and building a building
     *
     * @param index the index to check
     * @return whether the code was invoked
     */
    private boolean tryToAddBuilding(int index) {
        ArrayList<Reference<Building>> buildings = getPlayer().getOwnedBuildings();

        if (buildings.get(index).isValid()
                && buildings.get(index).get().getViewableTiles().size() != 0) {
            List<HexagonTile> visibleTiles = buildings.get(index).get().getViewableTiles();
            int j = mRandom.nextInt(visibleTiles.size());
            final int END = j;
            while (true) {
                HexagonTile tile = visibleTiles.get(j);
                if (tile.isClaimed() == false && tile.hasBuilding() == false) {
                    getPlayer().getClientBuildRequest().invoke((d) -> d.setTile(tile));

                    return true;
                }
                j++;
                if (j >= visibleTiles.size()) {
                    j = 0;
                }
                if (j == END) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Pick a {@link Building} and attempt to upgrade one of its stats.
     *
     * @return Whether the attempt to upgrade a building's stats was invoked.
     */
    private boolean upgradeBuilding() {
        log.info("AI: Upgrading");

        if (getPlayer() == null) {
            return false;
        }
        if (getPlayer().getNumberOfOwnedBuildings() == 0) {
            return false;
        }

        int buildingIndex = mRandom.nextInt(getPlayer().getNumberOfOwnedBuildings());
        Reference<Building> buildingReference = getPlayer().getOwnedBuildings().get(buildingIndex);
        if (buildingReference == null || buildingReference.isValid() == false) {
            log.info("AI: could not get building to upgrade.");
            return false;
        }

        // Get the Building.
        Building building = buildingReference.get();

        // Get Stat to upgrade.
        ArrayList<SyncStat<?>> stats = building.getStats();
        SyncStat<?> stat = stats.get(mRandom.nextInt(stats.size()));

        getPlayer().getClientStatRequest().invoke(d -> d.setData(building, stat));
        return true;
    }

    /**
     * Attack an opponent {@link Building} from an owned Building.
     *
     * @return Whether the attempt to attack an opponent was invoked.
     */
    private boolean attack() {
        log.info("AI: Attacking");

        return checkBuilding(this::tryToAttack);
    }

    /**
     * This will try to attack from a building
     *
     * @param index where in the list to get it
     * @return whether it was invoked
     */
    private boolean tryToAttack(int index) {
        ArrayList<Reference<Building>> buildings = getPlayer().getOwnedBuildings();
        if (buildings.get(index).isValid()
                && buildings.get(index).get().getAttackableBuildings().size() != 0) {
            int buildingChoice =
                    mRandom.nextInt(buildings.get(index).get().getAttackableBuildings().size());
            Building defender =
                    buildings.get(index).get().getAttackableBuildings().get(buildingChoice);
            Building attacker = buildings.get(index).get();

            getPlayer().getClientAttackRequest().invoke(d -> d.setData(attacker, defender));

            return true;
        }
        return false;
    }

    /**
     * Pick a {@link Building} and sell it.
     *
     * @return Whether the attempt to sell a building was invoked.
     */
    private boolean sell() {
        log.info("AI: Selling");
        if (getPlayer().getNumberOfOwnedBuildings() > 1) {

            int buildingIndex = mRandom.nextInt(getPlayer().getNumberOfOwnedBuildings());
            Reference<Building> buildingReference =
                    getPlayer().getOwnedBuildings().get(buildingIndex);
            if (buildingReference == null || buildingReference.isValid() == false) {
                log.info("AI: could not get building to sell.");
                return false;
            }

            // Get the Building.
            Building building = buildingReference.get();
            if (building.isCapital()) {
                log.info("Tried selling capital");
                return false;
            }
            getPlayer().getClientSellRequest().invoke(d -> d.setData(building));
            return true;
        }

        return false;
    }

    /**
     * Gets the player
     *
     * @return the player
     */
    private Player getPlayer() {
        Player player = mPlayer.get();
        if (player == null) {
            log.severe("Reference to mPlayer is null!");
        }
        return player;
    }
}
