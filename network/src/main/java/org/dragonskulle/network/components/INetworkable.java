package org.dragonskulle.network.components;

import org.dragonskulle.network.NetworkClient;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class INetworkable {
    /**
     * Connects all sync vars in the Object to the network Object. This allows them to be updated.
     */

    final NetworkObject netObj;

    INetworkable(NetworkObject object) {
        this.netObj = object;
    }

    void connectSyncVars() throws IllegalAccessException {
        List<Field> fields = Arrays.stream(this.getClass().getDeclaredFields()).filter(field -> ISyncVar.class.isAssignableFrom(field.getType())).collect(Collectors.toList());
        System.out.println("No. Fields: " + fields.size());
        for (Field f : fields) {
            ISyncVar fd = (ISyncVar) f.get(this);
            netObj.registerSyncVar(fd);
        }
    }
}


