/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.dragonskulle.game.map.HexMap;
import org.dragonskulle.game.map.HexagonTile;

/** The type Server game instance. */
public class ServerGameInstance {
    /** The Map store, should be depreciated in favour of a HexMap object. */
    private HexagonTile[][] map;

    /** Instantiates a new Server game instance. */
    ServerGameInstance() {
        this.map = new HexMap(9).createHexMap();
    }

    /**
     * Gets the bytes of the map.
     *
     * @return the bytes of the map
     * @throws IOException thrown if map serialization fails
     */
    public byte[] cloneMap() throws IOException {
        // TODO be replaced with HexMap.serialize();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(map);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * True if the instance is setup.
     *
     * @return the boolean
     */
    public boolean isSetup() {
        return this.map != null;
    }
>>>>>>> 4328d84... creating map on server creation and then sending bytes to connecting clients. Need to alter send recieve bytes protocl because messages are large and can get chopped in half
}
