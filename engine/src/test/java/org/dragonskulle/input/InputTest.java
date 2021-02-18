package org.dragonskulle.input;

import static org.junit.Assert.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link Input}.
 * @author Craig Wilbourne
 */
public class InputTest {
	 
	public static final Logger LOGGER = Logger.getLogger("InputTest");
	
	private long window;
	
	/** Before every test, create a window. */
	@Before
    public void createWindow() {
    	initWindow(100, 100, "TestWindow");
    }
    
	/** After every test, destroy the window. */
    @After
    public void destroyWindow() {
    	cleanup();
    }   
    
    @Test
    public void inputTest1() {    	
    	assertTrue(true);
    }
    
    /** Creates a GLFW window */
    private void initWindow(int width, int height, String appName) {
        LOGGER.info("Initialising test GLFW window.");

        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW for InputTest");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, appName, NULL, NULL);

        if (window == NULL) {
            throw new RuntimeException("Cannot create window for TestInput");
        }
    }
    
    private void cleanup() {
        LOGGER.info("Destroying test GLFW window.");
        glfwDestroyWindow(window);
        glfwTerminate();
    }
    
}