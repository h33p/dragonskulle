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
 * This will output the Supervised page
 * 
 * @author low101043
 *
 */
public class SupervisedPage extends Application implements Window {

	private Button backHome; // Takes the user back to the main page
	private Button knn; // Takes user to KNN page
	private Button regression; // Takes the User to regression page

	/**
	 * The constructor which creates the page
	 * 
	 * @param sceneChooser
	 */
	public SupervisedPage(ScreenController sceneChooser) {

		// Sets the main root
		BorderPane root = new BorderPane();

		// sets the correct button
		backHome = new Button("Go To Home Page");
		root.setLeft(backHome);

		root.setTop(new Label("Supervised Learning"));

		knn = new Button("KNN");
		regression = new Button("Regression/Logistic Regression");

		VBox controls = new VBox();
		controls.getChildren().addAll(knn, regression);

		root.setCenter(controls);

		sceneChooser.addScreen("Supervised Page", root, this);
	}

	@Override
	public void controls(ScreenController sceneChooser) {
		

		// Takes user back to the main page
		backHome.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				// Clears the screen
				sceneChooser.activate("Main Page"); // Activates home page screen
				return;
			}
		});

		// Takes user to regression page
		regression.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Window controls = sceneChooser.activate("Regression Page");
				controls.controls(sceneChooser);
			}
		});

		// Takes user to knn page
		knn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Window controls = sceneChooser.activate("KNN Screen");
				controls.controls(sceneChooser);
			}
		});
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
	}

}
