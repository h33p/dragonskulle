package org.dragonskulle.core;

import org.junit.Assert;
import org.junit.Test;

public class ReferenceTest {

    /**
     * Test that two references to equal objects return true when using Reference.equals()
     */
    @Test
    public void referenceEqualsSameObject() {
        Object obj = new Object();

        Reference<Object> objRef1 = new Reference<>(obj);
        Reference<Object> objRef2 = new Reference<>(obj);

        Assert.assertEquals("References to the same object were not equal", objRef1, objRef2);
    }

    /**
     * Test that two references to non-equal objects return false when using Reference.equals()
     */
    @Test
    public void referenceEqualsDifferentObject() {
        Object obj1 = new Object();
        Object obj2 = new Object();

        Reference<Object> objRef1 = new Reference<>(obj1);
        Reference<Object> objRef2 = new Reference<>(obj2);

        Assert.assertNotEquals("References to different objects were equal", objRef1, objRef2);
    }

    /**
     * Test that when a reference is cleared, attempting to get the referenced object returns null
     */
    @Test
    public void referenceIsNullWhenCleared() {
        Object obj = new Object();
        Reference<Object> ref = new Reference<>(obj);

        ref.clear();

        Assert.assertNull("Object reference was not null after being cleared", ref.get());
    }
}
