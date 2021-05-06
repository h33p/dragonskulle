/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import static org.dragonskulle.network.testing.NetworkedTestContext.TIMEOUT;

import org.dragonskulle.game.App;
import org.dragonskulle.game.AppTest;
import org.dragonskulle.network.components.ClientNetworkManager;
import org.dragonskulle.network.testing.NetworkedTestContext;
import org.junit.Test;

/** Unit test for player things */
public class PlayerTest {
    /** This test performs a simple check whether a player spawns within timeout (1 second). */
    @Test
    public void testPlayerSpawns() {
        App app = new App();
        NetworkedTestContext ctx = AppTest.buildTestContext(app);

        // This is an array, because it gets modified within the future.
        // This value will stay null even after calling waitPlayerSpawn,
        // because the futures are only executed on ctx.execute() line.
        Player[] myPlayer = {null};

        waitPlayerSpawn(ctx, myPlayer);

        ctx.execute();
    }

    private void waitPlayerSpawn(NetworkedTestContext ctx, Player[] myPlayer) {
        ctx.getClient()
                .awaitTimeout(
                        TIMEOUT,
                        (__) -> {
                            ClientNetworkManager clientMan = ctx.getClientManager();
                            myPlayer[0] =
                                    clientMan
                                            .getIdSingletons(clientMan.getNetId())
                                            .get(Player.class);
                            return myPlayer[0] != null;
                        });
    }

    /**
     * This test performs a simple check whether a player spawns with a capital within timeout (1
     * second).
     */
    @Test
    public void testCapitalSpawns() {
        App app = new App();
        NetworkedTestContext ctx = AppTest.buildTestContext(app);

        Player[] myPlayer = {null};

        waitPlayerSpawn(ctx, myPlayer);
        ctx.getClient().awaitTimeout(TIMEOUT, (__) -> myPlayer[0].getCapital() != null);

        ctx.execute();
    }
}
