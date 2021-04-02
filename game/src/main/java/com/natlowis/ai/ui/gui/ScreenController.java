package com.natlowis.ai.ui.gui;

import java.util.HashMap;

import org.apache.log4j.Logger;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * Edited from:
 * https://stackoverflow.com/questions/37200845/how-to-switch-scenes-in-javafx
 * This holds all the <code> Scenes </code> which are being used by the programs
 * 
 * @author low101043
 *
 */
public class ScreenController {

	// All the variables needed
	private HashMap<String, Object[]> screenMap = new HashMap<>(); // Contains the object needed
	private Scene main; // The scene being shown
	private Stage stage; // The stage being used

	private static Logger logger = Logger.getLogger(ScreenController.class);

	/**
	 * Constructor which makes the ScreenController
	 * 
	 * @param main  The initial <code> Scene </code> being shown
	 * @param stage The <code> Stage </code> being used
	 */
	public ScreenController(Scene main, Stage stage) {
		this.main = main;
		this.stage = stage;
	}

	/**
	 * This adds a screen which can be used
	 * 
	 * @param name     The name of the screen to be added
	 * @param pane     The pane which is being added
	 * @param controls The {@code Window} which has the controls for the specific
	 *                 {@code Pane}
	 */
	public void addScreen(String name, Pane pane, Window controls) {

		// Adding all the items to the hash map
		Object[] array = { controls, pane };
		logger.trace("Added Items:" + " " + name);
		screenMap.put(name, array);
	}

	/**
	 * Removes a Screen to be used
	 * 
	 * @param name The name of the screen to be removed
	 */
	public void removeScreen(String name) {
		screenMap.remove(name);
	}

	/**
	 * Activates the correct scene
	 * 
	 * @param name The name of the scene to be used
	 * @return The {@code Window} which holds the controls for that screen
	 */
	public Window activate(String name) {

		Object[] data = screenMap.get(name); // Gets the data
		logger.trace("In activate");
		Pane sceneToSwapTo = (Pane) data[1];
		Window controlsToUse = (Window) data[0];
		main.setRoot(sceneToSwapTo); // Set the scene to be used

		return controlsToUse;
	}

	/**
	 * Will return the {@code Scene} which is being used
	 * 
	 * @return The {@code Scene} which is on
	 */
	public Pane getPaneOn() {
		return (Pane) main.getRoot();
	}

	/**
	 * Will return the {@code Stage} which is being used
	 * 
	 * @return The {@code Stage} which is being used
	 */
	public Stage getStage() {
		return stage;
	}
}