/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// The below tests were generated by https://www.diffblue.com/

public class ScrollTest {
    @Test
    public void testReset() {
        Scroll scroll = new Scroll(new Buttons(new Bindings()));
        scroll.reset();
        assertEquals(0.0, scroll.getAmount(), 0.0);
    }

    @Test
    public void testAdd() {
        Scroll scroll = new Scroll(new Buttons(new Bindings()));
        scroll.add(10.0);
        assertEquals(10.0, scroll.getAmount(), 0.0);
    }
}