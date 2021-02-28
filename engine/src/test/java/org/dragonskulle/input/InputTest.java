/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.dragonskulle.input.custom.MyActions;
import org.dragonskulle.input.test_bindings.TestActions;
import org.dragonskulle.input.test_bindings.TestBindings;
import org.joml.Vector2d;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.glfw.GLFW;

/**
 * Unit test for {@link Input}.
 *
 * @author Craig Wilbourne
 */
public class InputTest {

    public static final Logger LOGGER = Logger.getLogger("InputTest");

    /** An arbitrary key codes used for testing key presses. */
    public static final int TEST_KEY_1 = -12345;
    public static final int TEST_KEY_2 = -12300;

    /** The input being tested. Will be reset before every test. */
    private Input mInput;

    /** Before every test, create a window and attach Input to it. */
    @Before
    public void createWindowInput() {
    	TestBindings bindings = new TestBindings();
    	mInput = new Input(null, bindings);
        
    	// Testing: Editing bindings after Input is created.
    	bindings.add(GLFW.GLFW_KEY_P, MyActions.BONUS);
    	bindings.submit();
        
    }

    @Test
    public void buttonsNotNull() {
        Buttons buttons = mInput.getButtons();
        assertNotNull(buttons);
    }
    
    @Test
    public void cursorNotNull() {
        Cursor cursor = Actions.cursor;
        assertNotNull(cursor);
    }

    @Test
    public void scrollNotNull() {
        Scroll scroll = Actions.scroll;
        assertNotNull(scroll);
    }

    /** Ensure button activations and deactivations can be triggered and stored. */
    @Test
    public void buttonShouldStoreActivation() {
        boolean activated;

        Buttons buttons = mInput.getButtons();
        assertNotNull(buttons);

        buttons.setActivated(TEST_KEY_1, true);
        activated = buttons.isActivated(TEST_KEY_1);
        assertTrue("Button TEST_KEY_1 should be activated (true).", activated);

        buttons.setActivated(TEST_KEY_1, false);
        activated = buttons.isActivated(TEST_KEY_1);
        assertFalse("Button TEST_KEY_1 should be deactivated (false).", activated);
    }

    /**
     * Ensure that pressing a button activates and deactivates an action.
     *
     * <p>As action bindings are currently hard-coded:
     *
     * <ul>
     *   <li>GLFW_KEY_UP triggers {@link Action#UP}
     * </ul>
     */
    @Test
    public void buttonShouldActivateAction() {
        // Parameters:
        int button = TEST_KEY_1;
        Action action = TestActions.TEST_ACTION;

        // For logic:
        boolean activated;

        // For error messages:
        String buttonName = String.format("[Button Code: %d]", button);
        String actionName = action.toString();

        // Run the test:
        Buttons buttons = mInput.getButtons();
        assertNotNull(buttons);

        buttons.press(button);
        activated = action.isActivated();
        assertTrue(
                String.format(
                        "%s should be activated (true) as %s has been pressed.",
                        actionName, buttonName),
                activated);

        buttons.release(button);
        activated = action.isActivated();
        assertFalse(
                String.format(
                        "%s should be deactivated (false) as %s has been released.",
                        actionName, buttonName),
                activated);
    }

    /**
     * Ensure that pressing a multiple buttons activates and deactivates an action.
     *
     * <p>As action bindings are currently hard-coded:
     *
     * <ul>
     *   <li>GLFW_KEY_UP triggers {@link Action#UP}
     *   <li>GLFW_KEY_W triggers {@link Action#UP}
     * </ul>
     */
    @Test
    public void multipleButtonsShouldActivateAction() {
        // Parameters:
        int button1 = TEST_KEY_1;
        int button2 = TEST_KEY_2;
        Action action = TestActions.TEST_ACTION;

        // For logic:
        boolean activated;

        // For error messages:
        String button1Name = String.format("[Button Code: %d]", button1);
        String button2Name = String.format("[Button Code: %d]", button2);
        String actionName = action.toString();

        // Run the test:
        Buttons buttons = mInput.getButtons();
        assertNotNull(buttons);

        buttons.press(button1);
        activated = action.isActivated();
        assertTrue(
                String.format(
                        "%s should be activated (true) as %s has been pressed.",
                        actionName, button1Name),
                activated);

        buttons.press(button2);
        activated = action.isActivated();
        assertTrue(
                String.format(
                        "%s should be activated (true) as %s and %s has been pressed.",
                        actionName, button1Name, button2Name),
                activated);

        buttons.release(button1);
        activated = action.isActivated();
        assertTrue(
                String.format(
                        "%s should be activated (true) as %s is still being pressed.",
                        actionName, button1Name),
                activated);

        buttons.release(button2);
        activated = action.isActivated();
        assertFalse(
                String.format(
                        "%s should be deactivated (false) as %s and %s have been released.",
                        actionName, button1Name, button2Name),
                activated);
    }

    /**
     * Ensure that actions that do not have any triggers remain deactivated.
     */
    @Test
    public void actionWithoutTrigger() {
    	// Parameters:
        Action action = TestActions.UNLINKED_ACTION;

        // For logic:
        boolean activated;

        // For error messages:
        String actionName = action.toString();

        activated = action.isActivated();
        assertFalse(
                String.format(
                        "%s should be deactivated (false) as nothing can activate it.",
                        actionName),
                activated);
    }
    
    /** Ensure {@link Scroll} is storing the amount scrolled (since last {@link Scroll#reset()}). */
    @Test
    public void scrollShouldStoreAmount() {
    	Scroll scroll = TestActions.scroll;
        assertNotNull(scroll);

        assertEquals("Scroll amount incorrect. ", scroll.getAmount(), 0, 0);

        scroll.add(10);
        assertEquals("Scroll amount incorrect. ", scroll.getAmount(), 10, 0);

        scroll.add(-999);
        assertEquals("Scroll amount incorrect. ", scroll.getAmount(), -989, 0);
    }

    /** Ensure calling {@link Scroll#reset()} resets the scroll values. */
    @Test
    public void scrollResetClearsValues() {
        // For logic:
        boolean activated;

        Buttons buttons = mInput.getButtons();
        assertNotNull(buttons);

        Scroll scroll = Actions.scroll;
        assertNotNull(scroll);

        // Manually simulate scrolling.
        buttons.press(Scroll.UP);
        buttons.press(Scroll.DOWN);
        scroll.add(-100d);

        // Reset the scrolling.
        scroll.reset();

        // Ensure all the button presses and the scroll amount has been reset.
        activated = buttons.isActivated(Scroll.UP);
        assertFalse(
                "Scroll.UP should be deactivated (false) as scrolling has been reset.", activated);

        activated = buttons.isActivated(Scroll.DOWN);
        assertFalse(
                "Scroll.DOWN should be deactivated (false) as scrolling has been reset.",
                activated);

        assertEquals("Scroll amount has been reset and should be 0.", scroll.getAmount(), 0, 0);
    }

    /** Ensure the cursor position is correctly stored. */
    @Test
    public void cursorPositionShouldBeStored() {
        Cursor cursor = Actions.cursor;
        assertNotNull(cursor);

        cursor.setPosition(123d, 456d);
        Vector2d desired = new Vector2d(123d, 456d);

        assertEquals(
                "Cursor position should equal Vector2d(123d, 456d).",
                cursor.getPosition(),
                desired);
    }

    /**
     * Ensure that when no dragging is taking place, the drag start location is null and the angle
     * and distance is 0.
     */
    @Test
    public void noDragShouldCauseNullOrZero() {
        Cursor cursor = Actions.cursor;
        assertNotNull(cursor);

        assertNull("No drag has begun, so DragStart should be null.", cursor.getDragStart());

        assertEquals(
                "No drag has begun, so the drag distance should be 0.",
                cursor.getDragDistance(),
                0,
                0);
        assertEquals(
                "No drag has begun, so the drag angle should be 0.", cursor.getDragAngle(), 0, 0);
    }

    /** Ensure that dragging correctly stores the start position. */
    @Test
    public void cursorDragStartPosition() {
        Cursor cursor = Actions.cursor;
        assertNotNull(cursor);

        // Set the cursor's position.
        cursor.setPosition(123d, 456d);
        Vector2d desired = new Vector2d(123d, 456d);

        // Start the drag.
        cursor.startDrag();
        assertEquals(
                "Drag start should equal Vector2d(123d, 456d).", cursor.getDragStart(), desired);

        // Move the cursor.
        cursor.setPosition(789d, 876d);
        assertEquals(
                "The cursor has moved. Drag start should equal Vector2d(123d, 456d).",
                cursor.getDragStart(),
                desired);

        // End the drag.
        cursor.endDrag();
        assertNull("Dragging has ended, so DragStart should be null.", cursor.getDragStart());
    }

    /** Ensure the distance calculated between the drag start and current position is correct. */
    @Test
    public void cursorDistanceCorrect() {
        Cursor cursor = Actions.cursor;
        assertNotNull(cursor);

        Vector2d desiredStart = new Vector2d(123d, 456d);
        Vector2d desiredEnd1 = new Vector2d(6d, 110d);
        Vector2d desiredEnd2 = new Vector2d(777d, 0d);
        double desiredDistance1 = desiredEnd1.distance(desiredStart);
        double desiredDistance2 = desiredEnd2.distance(desiredStart);

        cursor.setPosition(123d, 456d);
        cursor.startDrag();

        cursor.setPosition(6d, 110d);
        assertEquals(
                "The distance from the drag start point to the current position is not correct.",
                cursor.getDragDistance(),
                desiredDistance1,
                1e-15);

        cursor.setPosition(777d, 0d);
        assertEquals(
                "The distance from the drag start point to the current position is not correct.",
                cursor.getDragDistance(),
                desiredDistance2,
                1e-15);

        cursor.endDrag();
    }

    /** Ensure the angle calculated between the drag start and current position is correct. */
    @Test
    public void cursorAngleCorrect() {
        Cursor cursor = Actions.cursor;
        assertNotNull(cursor);

        Vector2d desiredStart = new Vector2d(123d, 456d);
        Vector2d desiredEnd1 = new Vector2d(6d, 110d);
        Vector2d desiredEnd2 = new Vector2d(777d, 0d);
        double desiredAngle1 = desiredEnd1.angle(desiredStart);
        double desiredAngle2 = desiredEnd2.angle(desiredStart);

        cursor.setPosition(123d, 456d);
        cursor.startDrag();

        cursor.setPosition(6d, 110d);
        assertEquals(
                "The angle between the drag start point and current position is not correct.",
                cursor.getDragAngle(),
                desiredAngle1,
                1e-15);

        cursor.setPosition(777d, 0d);
        assertEquals(
                "The angle between the drag start point and current position is not correct.",
                cursor.getDragAngle(),
                desiredAngle2,
                1e-15);

        cursor.endDrag();
    }

    /** Ensure the cursor can detect when it is being dragged. */
    @Test
    public void cursorDetectInDrag() {
        Cursor cursor = Actions.cursor;
        assertNotNull(cursor);

        assertFalse("Cursor is in drag, but it should not be.", cursor.inDrag());

        cursor.startDrag();
        assertTrue("Cursor is not drag, but it should be.", cursor.inDrag());

        cursor.endDrag();
        assertFalse("Cursor is in drag, but it should not be.", cursor.inDrag());
    }
}
