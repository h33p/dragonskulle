/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import static org.dragonskulle.network.testing.NetworkedTestContext.TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import lombok.extern.java.Log;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.network.components.ClientNetworkManager.ConnectionState;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.ServerNetworkManager.ServerGameState;
import org.dragonskulle.network.testing.NetworkedTestContext;
import org.junit.Test;

/** @author Oscar L, Aurimas Blažulionis */
@Log
public class ServerTest {
    private static final TemplateManager TEMPLATE_MANAGER = new TemplateManager();
    private static final Scene CLIENT_NETMAN_SCENE = new Scene("client_netman_test");
    private static final NetworkManager CLIENT_NETWORK_MANAGER =
            new NetworkManager(TEMPLATE_MANAGER, (__1, __2) -> new Scene("client_net_test"));

    private static final Scene SERVER_NETMAN_SCENE = new Scene("server_netman_test");
    private static final NetworkManager SERVER_NETWORK_MANAGER =
            new NetworkManager(TEMPLATE_MANAGER, (__1, __2) -> new Scene("server_net_test"));

    static {
        TEMPLATE_MANAGER.addAllObjects(
                new GameObject(
                        "cube",
                        (handle) -> {
                            handle.addComponent(new NetworkedTransformTestComponent());
                        }),
                new GameObject(
                        "test_comp",
                        (handle) -> {
                            handle.addComponent(new TestNetworkComponent());
                        }));

        CLIENT_NETMAN_SCENE.addRootObject(
                new GameObject("netman", handle -> handle.addComponent(CLIENT_NETWORK_MANAGER)));
        SERVER_NETMAN_SCENE.addRootObject(
                new GameObject("netman", handle -> handle.addComponent(SERVER_NETWORK_MANAGER)));
    }

    private static NetworkedTestContext buildTestContext() {
        return new NetworkedTestContext(
                TEMPLATE_MANAGER,
                (__, isServer) -> new Scene(isServer ? "server_net_test" : "client_net_test"),
                (__, man, id) -> man.getServerManager().start(),
                null,
                (man) -> {
                    man.getServerManager().spawnNetworkObject(0, TEMPLATE_MANAGER.find("cube"));
                    man.getServerManager()
                            .spawnNetworkObject(0, TEMPLATE_MANAGER.find("test_comp"));
                },
                null);
    }

    /** Test if connecting is possible over the network. */
    @Test
    public void testConnect() {
        NetworkedTestContext ctx = buildTestContext();

        connect(ctx);

        ctx.execute();
    }

    private static void connect(NetworkedTestContext ctx) {
        ctx.getServer()
                .awaitTimeout(TIMEOUT, (__) -> !ctx.getServerManager().getClients().isEmpty())
                .awaitTimeout(
                        TIMEOUT,
                        (__) ->
                                ctx.getServerManager().getGameState()
                                        == ServerGameState.IN_PROGRESS);

        ctx.getClient()
                .awaitTimeout(
                        TIMEOUT,
                        (__) ->
                                ctx.getClientManager().getConnectionState()
                                        == ConnectionState.JOINED_GAME);
    }

    /** Test if spawning is possible over the network. */
    @Test
    public void testSpawnObject() {
        NetworkedTestContext ctx = buildTestContext();

        connect(ctx);
        spawnObject(ctx);

        ctx.execute();
    }

    private static void spawnObject(NetworkedTestContext ctx) {
        ctx.getServer()
                .then(
                        (__) ->
                                assertTrue(
                                        ctx.getServerComponent(TestNetworkComponent.class)
                                                != null));

        ctx.getClient()
                .syncWith(ctx.getServer())
                .awaitTimeout(
                        TIMEOUT, (__) -> ctx.getClientComponent(TestNetworkComponent.class) != null)
                .then(
                        (__) -> {
                            Reference<TestNetworkComponent> clientTestComp =
                                    ctx.getClientComponent(TestNetworkComponent.class);
                            assertNotNull(clientTestComp);
                            Reference<TestNetworkComponent> serverTestComp =
                                    ctx.getServerComponent(TestNetworkComponent.class);
                            assertNotNull(serverTestComp);

                            int testCompId = clientTestComp.get().getNetworkObject().getId();

                            log.info(
                                    "\t-----> "
                                            + testCompId
                                            + " "
                                            + serverTestComp.get().getNetworkObject().getId());
                            assertEquals(
                                    testCompId, serverTestComp.get().getNetworkObject().getId());
                            log.info("\t-----> " + testCompId);
                            assert (serverTestComp.get().getSyncMe().get() == false);
                            assert (serverTestComp
                                    .get()
                                    .getSyncMeAlso()
                                    .get()
                                    .equals("Hello World"));
                        });

        ctx.getServer().syncWith(ctx.getClient());
        ctx.getClient().syncWith(ctx.getServer());
    }

    /** Test if modifying components is possible over the network. */
    @Test
    public void testModifyTestComp() {
        NetworkedTestContext ctx = buildTestContext();

        connect(ctx);
        spawnObject(ctx);
        modifyTestComp(ctx);

        ctx.execute();
    }

    private void modifyTestComp(NetworkedTestContext ctx) {
        ctx.getClient()
                .then(
                        (__) -> {
                            TestNetworkComponent component =
                                    ctx.getClientComponent(TestNetworkComponent.class).get();
                            assertFalse(component.getSyncMe().get());
                            assertFalse(component.getSyncMeAlso().get().equals("Goodbye World"));
                        });

        ctx.getServer()
                .syncWith(ctx.getClient())
                .then(
                        (__) -> {
                            TestNetworkComponent component =
                                    ctx.getServerComponent(TestNetworkComponent.class).get();
                            component.setBooleanSyncMe(true);
                            component.setStringSyncMeAlso("Goodbye World");
                        });

        ctx.getClient()
                .syncWith(ctx.getServer())
                .awaitTimeout(
                        TIMEOUT,
                        (__) -> {
                            TestNetworkComponent component =
                                    ctx.getClientComponent(TestNetworkComponent.class).get();
                            return component.getSyncMe().get()
                                    && component.getSyncMeAlso().get().equals("Goodbye World");
                        });

        ctx.getServer().syncWith(ctx.getClient());
    }

    /** Test if client can submit requests over the network. */
    @Test
    public void testSubmitRequest() {
        NetworkedTestContext ctx = buildTestContext();

        connect(ctx);
        spawnObject(ctx);
        submitRequest(ctx);

        ctx.execute();
    }

    private void submitRequest(NetworkedTestContext ctx) {
        TestNetworkComponent[] comp = {null};

        ctx.getClient()
                .then(
                        (__) -> {
                            comp[0] = ctx.getClientComponent(TestNetworkComponent.class).get();
                            comp[0].mPasswordRequest.invoke(
                                    new TestAttackData(TestNetworkComponent.CORRECT_PASSWORD, 354));
                        })
                .awaitTimeout(TIMEOUT, (__) -> comp[0].getClientToggled().get() == 354);

        ctx.getServer().syncWith(ctx.getClient());
    }

    /** Test if objects are destroyed over the network. */
    @Test
    public void testDestroy() {
        NetworkedTestContext ctx = buildTestContext();

        connect(ctx);
        spawnObject(ctx);
        destroy(ctx);

        ctx.execute();
    }

    private void destroy(NetworkedTestContext ctx) {
        ctx.getServer()
                .then(
                        (__) ->
                                ctx.getServerComponent(TestNetworkComponent.class)
                                        .get()
                                        .getGameObject()
                                        .destroy());
        ctx.getClient()
                .syncWith(ctx.getServer())
                .awaitTimeout(
                        TIMEOUT,
                        (__) ->
                                !Reference.isValid(
                                        ctx.getClientComponent(TestNetworkComponent.class)));

        ctx.getServer().syncWith(ctx.getClient());
    }

    /** Test if object owner IDs are set over the network. */
    @Test
    public void testSetOwnerId() {
        NetworkedTestContext ctx = buildTestContext();

        connect(ctx);
        spawnObject(ctx);
        setOwnerId(ctx);

        ctx.execute();
    }

    private void setOwnerId(NetworkedTestContext ctx) {
        int[] ownerId = {-10000};
        TestNetworkComponent[] serverComp = {null};
        TestNetworkComponent[] clientComp = {null};

        ctx.getServer()
                .then(
                        (__) ->
                                serverComp[0] =
                                        ctx.getServerComponent(TestNetworkComponent.class).get())
                .then((__) -> ownerId[0] = serverComp[0].getNetworkObject().getOwnerId());

        ctx.getClient()
                .syncWith(ctx.getServer())
                .then(
                        (__) ->
                                clientComp[0] =
                                        ctx.getClientComponent(TestNetworkComponent.class).get())
                .then(
                        (__) -> {
                            assertEquals(ownerId[0], clientComp[0].getNetworkObject().getOwnerId());
                        });

        ctx.getServer()
                .syncWith(ctx.getClient())
                .then((__) -> serverComp[0].getNetworkObject().setOwnerId(ownerId[0] + 1));

        ctx.getClient()
                .awaitTimeout(
                        TIMEOUT,
                        (__) -> clientComp[0].getNetworkObject().getOwnerId() == ownerId[0] + 1);

        ctx.getServer().syncWith(ctx.getClient());
    }
}
