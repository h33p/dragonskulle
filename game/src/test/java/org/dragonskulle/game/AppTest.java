/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import org.dragonskulle.network.testing.NetworkedTestContext;

/** Unit test for simple App. */
public class AppTest {
    public static NetworkedTestContext buildTestContext(int numClients, App app) {
        return new NetworkedTestContext(
                numClients,
                app.createTemplateManager(),
                App::createMainScene,
                app::onClientConnected,
                app::onGameStarted,
                null // intentionally do not spawn human player
                );
    }

    public static NetworkedTestContext buildTestContext(App app) {
        return buildTestContext(1, app);
    }
}
