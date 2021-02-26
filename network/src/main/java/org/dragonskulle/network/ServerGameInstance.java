/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import org.dragonskulle.game.map.HexMap;
import org.dragonskulle.game.map.HexagonTile;

public class ServerGameInstance {
<<<<<<< HEAD
    private
=======
    private HexagonTile[][] map;

    ServerGameInstance() {
        this.map = new HexMap(9).createHexMap();
        System.out.println("Map is: " + Arrays.deepToString(map));
    }

    public byte[] cloneMap() throws IOException {
        // TODO be replaced with HexMap.serialize();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(map);
        oos.flush();
        return bos.toByteArray();
    }

    public boolean isSetup() {
        if (this.map != null) {
            return true;
        }
        return false;
    }
>>>>>>> 4328d84... creating map on server creation and then sending bytes to connecting clients. Need to alter send recieve bytes protocl because messages are large and can get chopped in half
}
