/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import static org.junit.Assert.assertEquals;

import org.joml.Vector3f;
import org.junit.Test;

public class TransformHexTest {
    /** Test whether a child GameObject's parent always has the child */
    @Test
    public void testAxialTranslation() {
        TransformHex transform = new TransformHex();
        transform.translate(2f, 3f);

        Vector3f pos = transform.getLocalPosition(new Vector3f());

        assert (pos.x == 2f);
        assert (pos.y == 3f);
    }

    /** Test whether a child GameObject's parent always has the child */
    @Test
    public void testHexTranslation() {
        TransformHex transform = new TransformHex();
        transform.translate(1f, 1f);

        Vector3f pos = transform.getLocalPosition(new Vector3f());

        assertEquals(1f, pos.x, 0.01f);
        assertEquals(1f, pos.y, 0.01f);

        transform.translate(1f, 0f, 0f);

        transform.getLocalPosition(pos);
        assertEquals(2f, pos.x, 0.01f);
        assertEquals(1f, pos.y, 0.01f);

        transform.translate(0f, 1f, 0f);

        transform.getLocalPosition(pos);
        assertEquals(3f, pos.x, 0.01f);
        assertEquals(0f, pos.y, 0.01f);

        transform.translate(0f, 0f, 1f);

        transform.getLocalPosition(pos);
        assertEquals(3f, pos.x, 0.01f);
        assertEquals(-1f, pos.y, 0.01f);
    }
}
