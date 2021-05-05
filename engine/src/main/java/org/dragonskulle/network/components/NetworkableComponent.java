/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.network.components.requests.ClientRequest;
import org.dragonskulle.network.components.requests.ServerEvent;
import org.dragonskulle.network.components.sync.ISyncVar;

/**
 * Base component for any networkable game components.
 *
 * @author Oscar L
 *     <p>Any component that extends this, its syncvars will be updated with the server.
 */
@Accessors(prefix = "m")
public abstract class NetworkableComponent extends Component {

    @Getter private NetworkObject mNetworkObject = null;

    /** Instantiates a new Networkable component. */
    public NetworkableComponent() {}

    /** The Fields. */
    private Field[] mFields;

    /**
     * Init fields.
     *
     * @param networkObject the network object.
     * @param outRequests the requests it can deal with
     * @param outEvents the events it can deal with
     */
    public void initialise(
            NetworkObject networkObject,
            List<ClientRequest<?>> outRequests,
            List<ServerEvent<?>> outEvents) {

        mNetworkObject = networkObject;

        onNetworkInitialise();

        mFields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> ISyncVar.class.isAssignableFrom(field.getType()))
                        .toArray(Field[]::new);

        for (Field f : mFields) {
            f.setAccessible(true);
        }

        Field[] requestFields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> ClientRequest.class.isAssignableFrom(field.getType()))
                        .toArray(Field[]::new);

        try {
            for (Field f : requestFields) {
                f.setAccessible(true);
                ClientRequest<?> req = (ClientRequest<?>) f.get(this);
                outRequests.add(req);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        Field[] eventFields =
                Arrays.stream(this.getClass().getDeclaredFields())
                        .filter(field -> ServerEvent.class.isAssignableFrom(field.getType()))
                        .toArray(Field[]::new);

        try {
            for (Field f : eventFields) {
                f.setAccessible(true);
                ServerEvent<?> req = (ServerEvent<?>) f.get(this);
                outEvents.add(req);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        onConnectedSyncvars();
    }

    /** Event called whenever object is spawned and is being initialized by the network manager. */
    protected void onNetworkInitialise() {}

    /** Event called after {@link onNetworkInitialize}, when all syncvars have been initialized. */
    protected void onConnectedSyncvars() {}

    /** Event called before serialization takes place. */
    protected void beforeNetSerialize() {}

    /** Event called after the object has been updated on the client. */
    protected void afterNetUpdate() {}

    /**
     * Event called whenever the object's owner ID has changed.
     *
     * @param newId new owner ID of the object
     */
    protected void onOwnerIdChange(int newId) {}

    /**
     * Get stream of syncvars.
     *
     * @return stream of syncvars on this component
     */
    Stream<ISyncVar> getSyncVars() {
        return Arrays.stream(mFields)
                .map(
                        f -> {
                            try {
                                return (ISyncVar) f.get(this);
                            } catch (IllegalAccessException e) {
                                return null;
                            }
                        });
    }

    @Override
    public String toString() {
        StringBuilder fieldsString = new StringBuilder("Field{\n");
        for (Field field : mFields) {
            try {
                fieldsString.append(field.get(this).toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        fieldsString.append("\n}");

        return "NetworkableComponent{" + ", fields=" + fieldsString + '}';
    }

    /**
     * Get the {@link NetworkObject}'s {@link NetworkManager}, if the NetworkObject exists.
     *
     * @return The NetworkManager, or {@code null}.
     */
    public NetworkManager getNetworkManager() {
        if (mNetworkObject == null) {
            return null;
        }
        return mNetworkObject.getNetworkManager();
    }
}
