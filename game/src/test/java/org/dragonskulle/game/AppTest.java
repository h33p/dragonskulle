/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.dragonskulle.network.testing.NetworkedTestContext.TIMEOUT;

import org.dragonskulle.game.lobby.Lobby;
import org.dragonskulle.network.components.ClientNetworkManager.ConnectionState;
import org.dragonskulle.network.testing.NetworkedTestContext;
import org.junit.Test;

/** Unit test for simple App. */
public class AppTest {
    public static NetworkedTestContext buildTestContext(int numClients, App app) {

        int[] cnt = {0};

        NetworkedTestContext ctx =
                new NetworkedTestContext(
                        numClients,
                        app.createTemplateManager(),
                        App::createMainScene,
                        (__1, man, __2) -> {
                            if (++cnt[0] >= numClients) {
                                man.getServerManager().start();
                            }
                        },
                        Lobby::onClientLoaded,
                        Lobby::onGameStarted,
                        null // intentionally do not spawn human player
                        );

        // Await and sync, to make sure that the conditions do not run after disconnect
        ctx.getClients().stream()
                .forEach(
                        c ->
                                ctx.getServer()
                                        .syncWith(
                                                c.awaitTimeout(
                                                        TIMEOUT,
                                                        (__) ->
                                                                ctx.getClientManager()
                                                                                .getConnectionState()
                                                                        == ConnectionState
                                                                                .JOINED_GAME)));

        return ctx;
    }

    public static NetworkedTestContext buildTestContext(App app) {
        return buildTestContext(1, app);
    }

    @Test
    public void testGameConnect() {
        buildTestContext(new App()).execute();
    }

    @Test
    public void testMultipleGameConnect() {
        buildTestContext(8, new App()).execute();
    }
}
