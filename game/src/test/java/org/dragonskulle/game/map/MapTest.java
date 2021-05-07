/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import static org.dragonskulle.network.testing.NetworkedTestContext.TIMEOUT;
import static org.junit.Assert.assertNotNull;

import org.dragonskulle.game.App;
import org.dragonskulle.game.AppTest;
import org.dragonskulle.game.GameState;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.network.testing.NetworkedTestContext;
import org.junit.Test;

/** Unit test for map spawning. */
public class MapTest {
    /** Tests if a map spawns over the network, and the client has some map synced. */
    @Test
    public void mapSpawns() {
        App app = new App();
        NetworkedTestContext ctx = AppTest.buildTestContext(app);
        ctx.getClient()
                .awaitTimeout(
                        TIMEOUT,
                        (__) -> ctx.getClientScene().getSingleton(HexagonMap.class) != null)
                .then(
                        (__) ->
                                assertNotNull(
                                        ctx.getClientScene().getSingleton(GameState.class) != null))
                .awaitTimeout(
                        TIMEOUT,
                        (__) ->
                                ctx.getClientScene()
                                                .getSingleton(HexagonMap.class)
                                                .getAllTiles()
                                                .filter(t -> t.getTileType() != TileType.FOG)
                                                .findFirst()
                                                .orElse(null)
                                        != null);

        ctx.execute();
    }
}
