package com.natlowis.ai.ui.gui;

import java.io.File;
import java.io.IOException;

import com.natlowis.ai.exceptions.FileException;
import com.natlowis.ai.supervised.regression.LinearRegressionMultivariate;
import com.natlowis.ai.supervised.regression.LinearRegressionUnivariate;
import com.natlowis.ai.supervised.regression.LogisticRegressionMultivariate;
import com.natlowis.ai.supervised.regression.LogisticRegressionUnivariate;
import com.natlowis.ai.supervised.regression.Regression;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * The page where you can use either regression or logistic regression
 * 
 * @author low101043
 *
 */
public class RegressionChoice extends Application implements Window {

	// Sets all the variables needed
	private Button backHome;
	private TextField alpha;
	private TextField epoch;
	private TextField polynomialOrVariables;
	private Button clear;
	private Button submitMulti;
	private Button submitUni;
	private Button submitLogisticMulti;
	private Button submitLogisticUni;
	private Label label;
	private String instructions;

	/**
	 * The Constructor which makes the page.
	 * 
	 * @param sceneChooser The {@code ScreenController} where the page should be
	 *                     added to
	 */
	public RegressionChoice(ScreenController sceneChooser) {

		// sets up the back home page and the main pane used
		BorderPane root = new BorderPane();
		backHome = new Button("Go back home");
		root.setLeft(backHome);

		// Creating a GridPane container
		GridPane grid = new GridPane();
		grid.setPadding(new Insets(10, 10, 10, 10));
		grid.setVgap(5);
		grid.setHgap(5);

		// Defining the alpha text field
		alpha = new TextField();
		alpha.setPromptText("Enter your alpha value");
		alpha.setPrefColumnCount(10);
		alpha.getText();
		GridPane.setConstraints(alpha, 0, 0);
		grid.getChildren().add(alpha);

		// Defining the epoch text field
		epoch = new TextField();
		epoch.setPromptText("Enter the number of iterations");
		GridPane.setConstraints(epoch, 0, 1);
		grid.getChildren().add(epoch);

		// Defining the polynomialOrVariables text field
		polynomialOrVariables = new TextField();
		polynomialOrVariables.setPrefColumnCount(15);
		polynomialOrVariables.setPromptText("Enter the highest polynomial or the number of variables");
		GridPane.setConstraints(polynomialOrVariables, 0, 2);
		grid.getChildren().add(polynomialOrVariables);

		HBox choices = new HBox();

		// Defining the Submit button
		submitLogisticMulti = new Button("Multivariable Logistic Regression");
		choices.getChildren().add(submitLogisticMulti);

		// Defining the submitLogisticUni button
		submitLogisticUni = new Button("Univariate Logistic Regression");
		choices.getChildren().add(submitLogisticUni);

		// Defining the submitMulti button
		submitMulti = new Button("Multivariable Regression");
		choices.getChildren().add(submitMulti);

		// Defining the submitUni button
		submitUni = new Button("Univariate Regression");
		choices.getChildren().add(submitUni);
		GridPane.setConstraints(choices, 0, 3);
		grid.getChildren().add(choices);

		// Defining the Clear button
		clear = new Button("Clear");
		GridPane.setConstraints(clear, 1, 1);
		grid.getChildren().add(clear);

		// Adding a Label
		instructions = "Please enter these data: \n" + "-> The alpha value between 0 and 1 \n"
				+ "-> The number of iterations as an integer \n"
				+ "-> The next field will be the highest Power if the data can be modelled by one variable or the number of variables if the data can be modelled by multiple variables \n"
				+ "-> Then choose which regression to use.  Each one expects you to enter a file with the data seperated by commas \n"
				+ " \t-> Each row MUST be the same length and the final data point be either the numerical y number or the class it is in (Can split 2 classes either 0 or 1) \n"
				+ "In future updates I will improve the regression algorithms to handle more cases";
		label = new Label(instructions);
		GridPane.setConstraints(label, 0, 5);
		GridPane.setColumnSpan(label, 2);
		grid.getChildren().add(label);

		root.setCenter(grid); // Adding everything to the screen

		sceneChooser.addScreen("Regression Page", root, this); // Adding it all to the main page
	}

	@Override
	public void controls(ScreenController sceneChooser) {

		// Takes user back to main page
		backHome.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				// Clears all inputs
				alpha.clear();
				epoch.clear();
				polynomialOrVariables.clear();
				label.setText(instructions);
				sceneChooser.activate("Main Page"); // activates the main page screen
				return;
			}
		});

		// Does linear regression on multiple variables
		submitMulti.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				label.setText("Loading Values");
				if ((!alpha.getText().isEmpty() && !epoch.getText().isEmpty()
						&& !polynomialOrVariables.getText().isEmpty())) { // checks there are inputs

					// Opens the file to use
					FileChooser fileChooser = new FileChooser();
					fileChooser.setTitle("Open Data File");
					Stage stage = sceneChooser.getStage();
					File files = fileChooser.showOpenDialog(stage);
					if (files != null) {
						try {
							Integer.parseInt(polynomialOrVariables.getText());
							Integer.parseInt(epoch.getText());
							Double.parseDouble(alpha.getText());
							Integer.parseInt(polynomialOrVariables.getText());

							try {
								Regression regressionObject = new LinearRegressionMultivariate(files,
										Integer.parseInt(polynomialOrVariables.getText()));
								regressionObject.gradientDescent(Integer.parseInt(epoch.getText()),
										Double.parseDouble(alpha.getText()),
										Integer.parseInt(polynomialOrVariables.getText())); // Does
																							// gradient
																							// descent
								label.setText(output(regressionObject)); // Output answer
							} catch (FileException e) {
								label.setText("Not the correct file length");

							} catch (IOException e) {
								label.setText("The file cannot be read");
							} // Makes a regression object

						} catch (NumberFormatException e1) {
							label.setText("PLease make sure you have entered numbers in the boxes");
						}
					} else {
						label.setText("Please choose a file");
					}
				} else {
					label.setText("You have not filled out all values.");
				}
			}
		});

		// Does linear and non linear univariate regression
		submitUni.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				label.setText("Loading Values");
				if ((!alpha.getText().isEmpty() && !epoch.getText().isEmpty()
						&& !polynomialOrVariables.getText().isEmpty())) { // checks there are inputs

					// Opens the file to use
					FileChooser fileChooser = new FileChooser();
					fileChooser.setTitle("Open Data File");
					Stage stage = sceneChooser.getStage();
					File files = fileChooser.showOpenDialog(stage);
					if (files != null) {
						try {
							Integer.parseInt(polynomialOrVariables.getText());
							Integer.parseInt(epoch.getText());
							Double.parseDouble(alpha.getText());
							Integer.parseInt(polynomialOrVariables.getText());

							try {
								Regression regressionObject = new LinearRegressionUnivariate(files);
								regressionObject.gradientDescent(Integer.parseInt(epoch.getText()),
										Double.parseDouble(alpha.getText()),
										Integer.parseInt(polynomialOrVariables.getText())); // Does
																							// gradient
																							// descent
								label.setText(output(regressionObject)); // Output answer
							} catch (FileException e) {
								label.setText("Not the correct file length");

							} catch (IOException e) {
								label.setText("The file cannot be read");
							} // Makes a regression object

						} catch (NumberFormatException e1) {
							label.setText("PLease make sure you have entered numbers in the boxes");
						}
					} else {
						label.setText("Please choose a file");
					}
				} else {
					label.setText("You have not filled out all values.");
				}
			}
		});

		// Does linear logistic regression on multiple objects
		submitLogisticMulti.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				label.setText("Loading Values");
				if ((!alpha.getText().isEmpty() && !epoch.getText().isEmpty()
						&& !polynomialOrVariables.getText().isEmpty())) { // checks there are inputs

					// Opens the file to use
					FileChooser fileChooser = new FileChooser();
					fileChooser.setTitle("Open Data File");
					Stage stage = sceneChooser.getStage();
					File files = fileChooser.showOpenDialog(stage);
					if (files != null) {
						try {
							Integer.parseInt(polynomialOrVariables.getText());
							Integer.parseInt(epoch.getText());
							Double.parseDouble(alpha.getText());
							Integer.parseInt(polynomialOrVariables.getText());

							try {
								Regression regressionObject = new LogisticRegressionMultivariate(files,
										Integer.parseInt(polynomialOrVariables.getText()));
								regressionObject.gradientDescent(Integer.parseInt(epoch.getText()),
										Double.parseDouble(alpha.getText()),
										Integer.parseInt(polynomialOrVariables.getText())); // Does
																							// gradient
																							// descent
								label.setText(output(regressionObject)); // Output answer
							} catch (FileException e) {
								label.setText("Not the correct file length");

							} catch (IOException e) {
								label.setText("The file cannot be read");
							} // Makes a regression object

						} catch (NumberFormatException e1) {
							label.setText("PLease make sure you have entered numbers in the boxes");
						}
					} else {
						label.setText("Please choose a file");
					}
				} else {
					label.setText("You have not filled out all values.");
				}
			}
		});

		// Does non linear and linear (kinda) regression on one variable
		submitLogisticUni.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				label.setText("Loading Values");
				if ((!alpha.getText().isEmpty() && !epoch.getText().isEmpty()
						&& !polynomialOrVariables.getText().isEmpty())) { // checks there are inputs

					// Opens the file to use
					FileChooser fileChooser = new FileChooser();
					fileChooser.setTitle("Open Data File");
					Stage stage = sceneChooser.getStage();
					File files = fileChooser.showOpenDialog(stage);
					if (files != null) {
						try {
							Integer.parseInt(polynomialOrVariables.getText());
							Integer.parseInt(epoch.getText());
							Double.parseDouble(alpha.getText());
							Integer.parseInt(polynomialOrVariables.getText());

							try {
								Regression regressionObject = new LogisticRegressionUnivariate(files);
								regressionObject.gradientDescent(Integer.parseInt(epoch.getText()),
										Double.parseDouble(alpha.getText()),
										Integer.parseInt(polynomialOrVariables.getText())); // Does
																							// gradient
																							// descent
								label.setText(output(regressionObject)); // Output answer
							} catch (FileException e) {
								label.setText("Not the correct file length");

							} catch (IOException e) {
								label.setText("The file cannot be read");
							} // Makes a regression object

						} catch (NumberFormatException e1) {
							label.setText("PLease make sure you have entered numbers in the boxes");
						}
					} else {
						label.setText("Please choose a file");
					}
				} else {
					label.setText("You have not filled out all values.");
				}
			}
		});

		// Setting an action for the Clear button
		clear.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent e) {
				// Clears everything
				alpha.clear();
				epoch.clear();
				polynomialOrVariables.clear();
				label.setText(instructions);
			}
		});

	}

	// TODO Sort out making new value! Do it from file!

	@Override
	public void start(Stage primaryStage) throws Exception {
		// Not needed but needed for Application

	}

	/**
	 * This gets all the W values for the regression object
	 * 
	 * @param object A regression object where gradient descent has been done
	 * @return A String which gives an output
	 */
	private String output(Regression object) {

		String answer = ""; // The answer

		double[] wValues = object.answers(); // Gets all the wValues
		for (int i = 0; i < wValues.length; i++) {

			answer += "w" + i + ": " + wValues[i] + "\n"; // Adds each wValue

		}
		answer += "Cost: " + object.cost(); // Adds the cost
		return answer;
	}

}
