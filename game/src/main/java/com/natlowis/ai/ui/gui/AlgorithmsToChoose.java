package com.natlowis.ai.ui.gui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * This Page allows you to choose which set of algorithms you want to use
 * 
 * @author low101043
 *
 */
public class AlgorithmsToChoose extends Application implements Window {

	private Button backHome; // Button which takes you back to the home page
	private Button search; // Button which takes you to the search algorithms
	private Button regression; // Button which takes you to the regression page
	private Button qLearning; // Button which takes you to the q learning page
	private Button optimisation; // Button which takes you to optimisation page

	/**
	 * The constructor which sets up the page
	 * 
	 * @param sceneChooser This is the <code> ScreenController </code> which has all
	 *                     the different pages used.
	 */
	public AlgorithmsToChoose(ScreenController sceneChooser) {

		// Sets ups the mane pane
		BorderPane root = new BorderPane();

		// Sets up the back home button
		backHome = new Button("Go To Home Page");
		root.setLeft(backHome);

		// Sets where all other buttons go
		VBox centreNodes = new VBox();
		Label label1 = new Label("Press the button to take you to which algorithm you want to try");
		search = new Button("Search Algorithms");
		regression = new Button("Supervised Learning");
		qLearning = new Button("Unsupervised Learning");
		optimisation = new Button("Optimisation");

		root.setTop(label1);
		centreNodes.getChildren().addAll(search, regression, qLearning, optimisation);
		root.setCenter(centreNodes);

		// Adds the algorithm to ScreenController
		sceneChooser.addScreen("Algorithms To Choose", root, this);
	}

	@Override
	public void controls(ScreenController sceneChooser) {

		// This will take the user back to the home page
		backHome.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				sceneChooser.activate("Main Page"); // activates the screen
				return;
			}
		});

		// This will take the user to to q learning page
		qLearning.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Window controls = sceneChooser.activate("Q Learning Input"); // activates screen
				controls.controls(sceneChooser); // transfer control to right set
			}
		});

		// This will take the user to the regression page
		regression.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Window controls = sceneChooser.activate("Supervised Page"); // activates screen
				controls.controls(sceneChooser); // transfer control to right set
			}
		});

		// This will take the user to the search page
		search.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Window controls = sceneChooser.activate("Search Problems Page"); // activates screen
				controls.controls(sceneChooser); // transfer control to right set
			}
		});

		// This will take the user to the search page
		optimisation.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Window controls = sceneChooser.activate("Optimisation"); // Activates scree
				controls.controls(sceneChooser); // Transfers control
			}
		});
	}

	@Override
	/**
	 * This contains nothing as all control is in controls
	 */
	public void start(Stage primaryStage) throws Exception {

	}

}
