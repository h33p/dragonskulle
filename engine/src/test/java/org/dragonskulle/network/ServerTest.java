/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

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
import org.dragonskulle.network.testing.NetworkedTestContext;
import org.junit.Test;

/** @author Oscar L, Aurimas Blažulionis */
@Log
public class ServerTest {
    public static final long TIMEOUT = 1;

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
                            handle.addComponent(new NetworkedTransform());
                        }),
                new GameObject(
                        "capital",
                        (handle) -> {
                            handle.addComponent(new TestCapitalBuilding());
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
                (__, man, id) -> {
                    log.info("CONNECTED");
                    man.getServerManager().spawnNetworkObject(id, TEMPLATE_MANAGER.find("cube"));
                    man.getServerManager().spawnNetworkObject(id, TEMPLATE_MANAGER.find("capital"));
                },
                null,
                null);
    }

    @Test
    public void testConnect() {
        NetworkedTestContext ctx = buildTestContext();

        connect(ctx);

        ctx.execute();
    }

    private static void connect(NetworkedTestContext ctx) {
        ctx.getServer()
                .awaitTimeout(TIMEOUT, (__) -> !ctx.getServerManager().getClients().isEmpty());
        ctx.getClient()
                .awaitTimeout(
                        TIMEOUT,
                        (__) ->
                                ctx.getClientManager().getConnectionState()
                                        == ConnectionState.JOINED_GAME);
    }

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
                                        ctx.getServerComponent(TestCapitalBuilding.class) != null));

        ctx.getClient()
                .syncWith(ctx.getServer())
                .awaitTimeout(
                        TIMEOUT, (__) -> ctx.getClientComponent(TestCapitalBuilding.class) != null);

        ctx.getClient()
                .then(
                        (__) -> {
                            Reference<TestCapitalBuilding> clientCapital =
                                    ctx.getClientComponent(TestCapitalBuilding.class);
                            assertNotNull(clientCapital);
                            Reference<TestCapitalBuilding> serverCapital =
                                    ctx.getServerComponent(TestCapitalBuilding.class);
                            assertNotNull(serverCapital);

                            int capitalId = clientCapital.get().getNetworkObject().getId();

                            log.info(
                                    "\t-----> "
                                            + capitalId
                                            + " "
                                            + serverCapital.get().getNetworkObject().getId());
                            assertEquals(capitalId, serverCapital.get().getNetworkObject().getId());
                            log.info("\t-----> " + capitalId);
                            assert (serverCapital.get().getSyncMe().get() == false);
                            assert (serverCapital
                                    .get()
                                    .getSyncMeAlso()
                                    .get()
                                    .equals("Hello World"));
                        });

        ctx.getServer().syncWith(ctx.getClient());
    }

    @Test
    public void testModifyCapital() {
        NetworkedTestContext ctx = buildTestContext();

        connect(ctx);
        spawnObject(ctx);
        modifyCapital(ctx);

        ctx.execute();
    }

    private void modifyCapital(NetworkedTestContext ctx) {
        ctx.getClient()
                .then(
                        (__) -> {
                            TestCapitalBuilding component =
                                    ctx.getClientComponent(TestCapitalBuilding.class).get();
                            assertFalse(component.getSyncMe().get());
                            assertFalse(component.getSyncMeAlso().get().equals("Goodbye World"));
                        });

        ctx.getServer()
                .syncWith(ctx.getClient())
                .then(
                        (__) -> {
                            TestCapitalBuilding component =
                                    ctx.getServerComponent(TestCapitalBuilding.class).get();
                            component.setBooleanSyncMe(true);
                            component.setStringSyncMeAlso("Goodbye World");
                        });

        ctx.getClient()
                .syncWith(ctx.getServer())
                .awaitTimeout(
                        TIMEOUT,
                        (__) -> {
                            TestCapitalBuilding component =
                                    ctx.getClientComponent(TestCapitalBuilding.class).get();
                            return component.getSyncMe().get()
                                    && component.getSyncMeAlso().get().equals("Goodbye World");
                        });

        ctx.getServer().syncWith(ctx.getClient());
    }

    @Test
    public void testSubmitRequest() {
        NetworkedTestContext ctx = buildTestContext();

        connect(ctx);
        spawnObject(ctx);
        submitRequest(ctx);

        ctx.execute();
    }

    private void submitRequest(NetworkedTestContext ctx) {
        TestCapitalBuilding[] cap = {null};

        ctx.getClient()
                .then(
                        (__) -> {
                            cap[0] = ctx.getClientComponent(TestCapitalBuilding.class).get();
                            cap[0].mPasswordRequest.invoke(
                                    new TestAttackData(TestCapitalBuilding.CORRECT_PASSWORD, 354));
                        })
                .awaitTimeout(TIMEOUT, (__) -> cap[0].getClientToggled().get() == 354);

        ctx.getServer().syncWith(ctx.getClient());
    }

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
                                ctx.getServerComponent(TestCapitalBuilding.class)
                                        .get()
                                        .getGameObject()
                                        .destroy());
        ctx.getClient()
                .syncWith(ctx.getServer())
                .awaitTimeout(
                        TIMEOUT,
                        (__) ->
                                !Reference.isValid(
                                        ctx.getClientComponent(TestCapitalBuilding.class)));

        ctx.getServer().syncWith(ctx.getClient());
    }

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
        TestCapitalBuilding[] serverCap = {null};
        TestCapitalBuilding[] clientCap = {null};

        ctx.getServer()
                .then(
                        (__) ->
                                serverCap[0] =
                                        ctx.getServerComponent(TestCapitalBuilding.class).get())
                .then((__) -> ownerId[0] = serverCap[0].getNetworkObject().getOwnerId());

        ctx.getClient()
                .syncWith(ctx.getServer())
                .then(
                        (__) ->
                                clientCap[0] =
                                        ctx.getClientComponent(TestCapitalBuilding.class).get())
                .then(
                        (__) -> {
                            assertEquals(ownerId[0], clientCap[0].getNetworkObject().getOwnerId());
                        });

        ctx.getServer()
                .syncWith(ctx.getClient())
                .then((__) -> serverCap[0].getNetworkObject().setOwnerId(ownerId[0] + 1));

        ctx.getClient()
                .awaitTimeout(
                        TIMEOUT,
                        (__) -> clientCap[0].getNetworkObject().getOwnerId() == ownerId[0] + 1);

        ctx.getServer().syncWith(ctx.getClient());
    }
}
