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
 * The screen which shows the instructions on how to use the application
 * 
 * @author low101043
 *
 */
public class Instructions extends Application implements Window {

	private Button backHome; // Takes the user back to the main page

	/**
	 * This is the Constructor which sets up the page and adds it to sceneChanger
	 * 
	 * @param sceneChanger The <code> ScreenController </code> which has all the
	 *                     different screens being used
	 */
	public Instructions(ScreenController sceneChanger) {

		// Sets the main root
		BorderPane root = new BorderPane();

		// sets the correct button
		backHome = new Button("Go To Home Page");

		// Makes all the labels needed
		Label label1 = new Label(
				"This is a program where you can try some different AI Algorithms. \nThese are the algorithms which you can use which are generalised:");
		Label label2 = new Label("-> Regression");
		Label label3 = new Label("-> Logistic Regression");
		Label label4 = new Label("-> Breadth First Search");
		Label label5 = new Label("-> Depth First Search");
		Label label6 = new Label("-> Q-Learning");
		Label label7 = new Label("-> K Nearest Neighbours");
		Label label8 = new Label("-> A* Algorithm");
		Label label9 = new Label("-> Ant Colony Optimisation");

		Label label10 = new Label("\n How to use each algorithm is specified on it's page");

		// Add everything to the pane
		VBox instructions = new VBox();
		instructions.getChildren().addAll(label1, label2, label3, label4, label5, label6, label7, label8, label9,
				label10);
		root.setLeft(backHome);
		root.setTop(new Label("AI Instructions"));
		root.setCenter(instructions);

		// Adds it to the ScreenChanger
		sceneChanger.addScreen("Instructions", root, this);

	}

	@Override
	public void controls(ScreenController sceneChooser) {

		// Takes the user back to the home page
		backHome.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				sceneChooser.activate("Main Page"); // activates the screen
				return;
			}
		});
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// empty method which is not needed

	}

}
