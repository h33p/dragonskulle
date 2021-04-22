/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    /** Used to run events for building and attacking. */
    public interface IRunBuildingEvent {
        public boolean runEvent(Building building);
    }

    /** A Constructor for an AI Player. */
    public ProbabilisticAiPlayer() {}

    @Override
    protected void simulateInput() {

        // If only one building assumed that its capital
        if (getPlayer().getNumberOfOwnedBuildings() == 1) {

            log.info("One building left - try to add another");

            if (addBuilding()) {
                return;
            } else if (attack()) {
                return;
            } else {
                upgradeBuilding();
                return;
            }

        } else {
            log.info(
                    "AI: I have "
                            + getPlayer().getNumberOfOwnedBuildings()
                            + " buildings. Should be  more than  one.");

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
     * A function which goes through and checks if you can run code.
     *
     * @param lambdaMethod What to on a building
     * @return If the stuff is invoked on the server
     */
    public boolean attemptRunEvent(IRunBuildingEvent lambdaMethod) {

        Player player = getPlayer();

        if (player == null) {
            return false;
        }

        if (player.getNumberOfOwnedBuildings() == 0) {
            return false;
        }

        int index = mRandom.nextInt(player.getNumberOfOwnedBuildings());
        final int end = index;

        List<Reference<Building>> buildings = player.getOwnedBuildings();

        // Goes through the ownedBuildings
        while (true) {
            Reference<Building> building = buildings.get(index);

            // Checks the building is valid
            if (Reference.isValid(building)) {
                // Check
                boolean completed = lambdaMethod.runEvent(building.get());
                if (completed) {
                    return true;
                }
            }
            index++;

            // If gone over start at 0
            if (index >= player.getNumberOfOwnedBuildings()) {
                index = 0;
            }

            // Checks if we've gone through the whole list
            if (index == end) {
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

        return attemptRunEvent(this::tryToAddBuilding);
    }

    /**
     * Try and place a {@link Building} from the specified Building.
     *
     * @param building The building to place from.
     * @return Whether building placement was invoked.
     */
    private boolean tryToAddBuilding(Building building) {

        if (building.getBuildableTiles().size() != 0) {

            // Get the buildable tiles
            List<HexagonTile> buildableTiles =
                    new ArrayList<HexagonTile>(building.getBuildableTiles());
            int index = mRandom.nextInt(buildableTiles.size());
            final int END = index;

            // Checks if we can use one of the tiles to build from
            while (true) {
                HexagonTile tile = buildableTiles.get(index);
                if (tile.isClaimed() == false && tile.hasBuilding() == false) {
                    getPlayer().getClientBuildRequest().invoke((d) -> d.setTile(tile));
                    return true;
                }
                index++;
                if (index >= buildableTiles.size()) {
                    index = 0;
                }
                if (index == END) {
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

        return attemptRunEvent(this::tryToUpgrade);
    }

    /**
     * Try and upgrade a building.
     *
     * @param building The building to upgrade.
     * @return Whether upgrading was invoked.
     */
    private boolean tryToUpgrade(Building building) {
        ArrayList<SyncStat> stats = building.getUpgradeableStats();
        if (stats.size() == 0) {
            return false;
        }

        int index = mRandom.nextInt(stats.size());
        final int end = index;
        do {
            SyncStat stat = stats.get(index);

            // If the stat is still able to be upgraded and can be afforded, attempt an upgrade.
            if (stat.isUpgradeable() && stat.getCost() <= getPlayer().getTokens().get()) {
                getPlayer().getClientStatRequest().invoke(d -> d.setData(building, stat));
                return true;
            }

            // Go to the next stat.
            index++;
            if (index >= stats.size()) {
                index = 0;
            }
        } while (index != end);

        return false;
    }

    /**
     * Attack an opponent {@link Building} from an owned Building.
     *
     * @return Whether the attempt to attack an opponent was invoked.
     */
    private boolean attack() {
        log.info("AI: Attacking");

        return attemptRunEvent(this::tryToAttack);
    }

    /**
     * This will try to attack from a building.
     *
     * @param attacker The building to attack from.
     * @return Whether attacking was invoked.
     */
    private boolean tryToAttack(Building attacker) {
        Set<Building> builds = attacker.getAttackableBuildings();

        if (builds.size() != 0) {
            // Gets the defending and attacking buildings
            int buildingChoice = mRandom.nextInt(builds.size());
            Building defender = builds.stream().skip(buildingChoice).findFirst().orElse(null);
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
            if (!Reference.isValid(buildingReference)) {
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
     * Gets the player.
     *
     * @return The player.
     */
    private Player getPlayer() {
        Player player = mPlayer.get();
        if (player == null) {
            log.severe("Reference to mPlayer is null!");
        }
        return player;
    }
}
