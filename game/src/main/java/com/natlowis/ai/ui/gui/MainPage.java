package com.natlowis.ai.ui.gui;

import org.apache.log4j.Logger;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The main page which the user will see at first
 * 
 * @author low101043
 *
 */
public class MainPage extends Application implements Window {

	private static Logger logger = Logger.getLogger(MainPage.class);
	private ScreenController sceneChooser; // The screen chooser which is used

	@Override
	public void start(Stage stage) throws InterruptedException {

		// Sets the main stage and scene used
		stage.setTitle("AI Project");
		BorderPane root = new BorderPane();
		Scene mainPage = new Scene(root, 800, 500);
		stage.setScene(mainPage);
		sceneChooser = new ScreenController(mainPage, stage); // Sets up the ScreenController

		// Adds the first label and makes screen pretty
		Label label1 = new Label("This is a program which uses some of the AI algorithms which I have learnt in first year");
		root.setTop(label1);
		root.setLeft(new Label("\t"));

		// Adds some buttons
		Button algorithms = new Button("Algorithms");
		Button instructions = new Button("Instructions");

		VBox centre = new VBox();
		centre.getChildren().addAll(algorithms, instructions);
		root.setCenter(centre);

		// Sets up more labels
		root.setBottom(new Label("Copyright Nathaniel Lowis"));

		sceneChooser.addScreen("Main Page", root, this); // Adds everything to the ScreenController

		addAllScreens(); // Sets up all other scenes

		stage.show(); // Shows the stage

		// Takes you to the page which has all the algorithms on it
		algorithms.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Window controls = sceneChooser.activate("Algorithms To Choose"); // activates the screen
				controls.controls(sceneChooser); // Transfers control
			}
		});

		// Takes user to instruction page
		instructions.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Window controls = sceneChooser.activate("Instructions"); // activates the screen
				controls.controls(sceneChooser); // Transfers control
			}
		});

		logger.trace("Finished");

	}

	/**
	 * This is how to launch the whole project
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args); // Launches the screen!
	}

	@Override
	public void controls(ScreenController sceneChooser) {
		// Needed to make it a window
	}

	/**
	 * This sets up all the other scenes used so they can be used
	 */
	private void addAllScreens() {
		new Instructions(sceneChooser);
		new AlgorithmsToChoose(sceneChooser);
		new QLearningInput(sceneChooser);
		new RegressionChoice(sceneChooser);
		new SearchProblems(sceneChooser);

		new KNN(sceneChooser);
		new SupervisedPage(sceneChooser);

		new Optimisation(sceneChooser);

	}
}
