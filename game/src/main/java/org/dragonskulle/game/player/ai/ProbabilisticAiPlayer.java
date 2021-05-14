/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.GameConfig.ProbabilisticAiConfig;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.player.BuildingDescriptor;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.game.player.PredefinedBuildings;

/**
 * This will create AI which will choose an action with a certain probability.
 *
 * @author DragonSkulle
 */
@Log
public class ProbabilisticAiPlayer extends AiPlayer {

    /** Used to run events for building and attacking. */
    public interface IRunBuildingEvent {
        /**
         * This will run the event to do.
         *
         * @param building The {@link Building} to use
         * @return {@code true} if it is invoked otherwise {@code false}
         */
        public boolean runEvent(Building building);
    }

    /** A Constructor for an AI Player. */
    public ProbabilisticAiPlayer() {}

    @Override
    protected void simulateInput() {

        // If only one building assumed that its capital
        if (getPlayer().getNumberOfOwnedBuildings() == 1) {

            if (addBuilding()) {
                return;
            } else if (!getPlayer().inCooldown() && attack()) {
                return;
            } else {
                upgradeBuilding();
                return;
            }

        } else {

            // Pick a random number to choose whether to place a building or to use a building
            float randomNumber = mRandom.nextFloat();

            ProbabilisticAiConfig cfg = getConfig().getProbabilisticAi();

            // Choose an action to take.
            if (randomNumber <= cfg.getBuildProbability()) {
                addBuilding();
            } else if (randomNumber <= cfg.getBuildProbability() + cfg.getUpgradeProbability()) {
                upgradeBuilding();
            } else if (randomNumber
                    <= cfg.getBuildProbability()
                            + cfg.getUpgradeProbability()
                            + cfg.getAttackProbability()) {
                // Only attempt to attack if you're not in cooldown.
                if (!getPlayer().inCooldown()) {
                    attack();
                }
            } else if (randomNumber
                    <= cfg.getBuildProbability()
                            + cfg.getUpgradeProbability()
                            + cfg.getAttackProbability()
                            + cfg.getSellProbability()) {
                sell();
            } else {
                log.fine("AI probabilites do not sum to one- no action performed.");
            }
        }
    }

    /**
     * A function which goes through and checks if you can run code.
     *
     * @param lambdaMethod What to on a building
     * @return {@code true} if invoked on the server otherwise {@code false}
     */
    protected boolean attemptRunEvent(IRunBuildingEvent lambdaMethod) {

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
        log.fine("Probabilistic: Placing Building");

        return attemptRunEvent(this::tryToAddBuilding);
    }

    /**
     * Try and place a {@link Building} from the specified Building.
     *
     * @param building the {@link Building} to try and build from.
     * @return Whether building placement was invoked.
     */
    private boolean tryToAddBuilding(Building building) {

        // Get a building type that they can afford.
        BuildingDescriptor option = getRandomBuildingType();
        if (option == null) return false;

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
                    getPlayer()
                            .getClientBuildRequest()
                            .invoke((d) -> d.setTile(tile, PredefinedBuildings.getIndex(option)));
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
    protected boolean upgradeBuilding() {
        log.fine("Probabilistic: Upgrading");

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
        log.fine("Probabilistic: Attacking");

        return attemptRunEvent(this::tryToAttack);
    }

    /**
     * This will try to attack from a building.
     *
     * @param attacker The building to lauch the attack from.
     * @return Whether attacking was invoked.
     */
    protected boolean tryToAttack(Building attacker) {

        // Ensure the player can afford to attack.
        if (attacker == null || attacker.getAttackCost() > getPlayer().getTokens().get()) {
            return false;
        }

        Building defender = attacker.getRandomAttackableBuilding();
        if (defender == null) return false;

        getPlayer().getClientAttackRequest().invoke(d -> d.setData(attacker, defender));
        return true;
    }

    /**
     * Pick a {@link Building} and sell it.
     *
     * @return Whether the attempt to sell a building was invoked.
     */
    private boolean sell() {
        log.fine("Probablistic: Selling");
        if (getPlayer().getNumberOfOwnedBuildings() > 1) {

            int buildingIndex = mRandom.nextInt(getPlayer().getNumberOfOwnedBuildings());
            Reference<Building> buildingReference =
                    getPlayer().getOwnedBuildings().get(buildingIndex);
            if (!Reference.isValid(buildingReference)) {
                log.fine("Probabilistic: could not get building to sell.");
                return false;
            }

            // Get the Building.
            Building building = buildingReference.get();
            if (building.isCapital()) {

                return false;
            }
            getPlayer().getClientSellRequest().invoke(d -> d.setData(building));
            return true;
        }

        return false;
    }
}
