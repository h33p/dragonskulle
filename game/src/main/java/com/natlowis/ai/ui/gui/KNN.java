package com.natlowis.ai.ui.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.natlowis.ai.exceptions.FileException;
import com.natlowis.ai.fileHandaling.CSVFiles;
import com.natlowis.ai.supervised.knn.ClassificationKNN;
import com.natlowis.ai.supervised.knn.KNearestNeighbour;
import com.natlowis.ai.supervised.knn.RegressionKNN;

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
 * This will output the screen which has KNN on it
 * 
 * @author low101043
 *
 */
public class KNN extends Application implements Window {

	private Button backHome; // Button to back home
	private TextField neighboursWanted; // Neighbours wanted
	private Label label; // Output
	private Button input; // The input button
	private Button regressionKNN; // For regression KNN
	private Button classificationKNN; // For Classification KNN
	private File inputData; // The input file
	private String instructions;

	/**
	 * The Constructor which constructs the screen to be used
	 * 
	 * @param sceneChooser The <Code> ScreenController </Code> being used
	 */
	public KNN(ScreenController sceneChooser) {

		// Sets the root page and the home button
		BorderPane root = new BorderPane();
		backHome = new Button("Go back home");
		root.setLeft(backHome);

		// Adds a label
		root.setTop(new Label("K Nearest Neighbour"));

		// Defining the GridPane used
		GridPane grid = new GridPane();
		grid.setPadding(new Insets(10, 10, 10, 10));
		grid.setVgap(5);
		grid.setHgap(5);

		// Defines the neighbours wanted
		neighboursWanted = new TextField();
		neighboursWanted.setPromptText("Enter the neighbours needed");
		neighboursWanted.setPrefColumnCount(10);
		neighboursWanted.getText();
		GridPane.setConstraints(neighboursWanted, 0, 2);
		grid.getChildren().add(neighboursWanted);

		// Defining the buttons needed
		regressionKNN = new Button("Numerical Data");
		classificationKNN = new Button("Classification Data");

		// Defining the output label
		instructions = "To use: \n->Enter the number of neightbours you want to use. \n->Choose a file which has the data to classify in it"
				+ " \n->Choose whether to do classification or numerical. "
				+ "\nIf Numerical enter a file which has the training data which is seperated by commas with data in this order \n\t"
				+ "Each row must have one more piece of data then the input row given previously with the final data point being the number linked to that point"
				+ "\nIf Classifcation enter a file which has the training data which is seperated by commas with data in this order \n\t"
				+ "Each row must have one more piece of data then the input row given previously with the final data point being the class (A class must be numerical) linked to that point";
		label = new Label(instructions

		);
		GridPane.setConstraints(label, 0, 7);
		GridPane.setColumnSpan(label, 2);
		grid.getChildren().add(label);

		// Defines button for input data
		input = new Button("Get the file with the input data");
		GridPane.setConstraints(input, 0, 3);
		grid.getChildren().add(input);

		// Adds everything to the scene
		HBox choices = new HBox();
		choices.getChildren().addAll(regressionKNN, classificationKNN);
		GridPane.setConstraints(choices, 0, 5);
		grid.getChildren().add(choices);
		root.setCenter(grid);

		sceneChooser.addScreen("KNN Screen", root, this); // Adds it to the screen changer
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

	}

	@Override
	public void controls(ScreenController sceneChooser) {

		// Takes user back to the main page
		backHome.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				// Clears the screen
				neighboursWanted.clear();
				inputData = null;
				label.setText(instructions);
				sceneChooser.activate("Main Page"); // Activates home page screen
				return;
			}
		});

		// What to do to get file
		input.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				// Opens the file to use
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Open Data File");
				Stage stage = sceneChooser.getStage();
				inputData = fileChooser.showOpenDialog(stage);
			}
		});

		// What to do if regression pressed
		regressionKNN.setOnAction(new EventHandler<ActionEvent>() { 
			@Override
			public void handle(ActionEvent t) {
				if (inputData != null && !neighboursWanted.getText().isEmpty()) {
					label.setText("Computing");
					ArrayList<Double> input = null;
					try {
						input = openFile();
						FileChooser fileChooser = new FileChooser();
						fileChooser.setTitle("Open Training Data");
						Stage stage = sceneChooser.getStage();
						File files = fileChooser.showOpenDialog(stage); // allow user to open file
						CSVFiles formattor = new CSVFiles(files, input.size() + 1); // makes a formatter to use
						ArrayList<ArrayList<String>> data = null;
						try {
							data = formattor.readCSV();
							ArrayList<ArrayList<Double>> trainingData = formattor.convertData(data);
							try {
								Integer.parseInt(neighboursWanted.getText());
								KNearestNeighbour knn = new RegressionKNN();
								Double output = knn.knn(input, Integer.parseInt(neighboursWanted.getText()),
										trainingData);

								if (output == null) {
									label.setText("There was no majority.  Choose another amount of neighbours");
									;
								} else {
									label.setText("The value for the input given and the training data is: " + output);
								}
							} catch (NumberFormatException e2) {
								label.setText("The neighbour input needs to be an integer");
								;
							}

						} catch (NumberFormatException e) {
							
							label.setText("The data is not all double");
						} // gets the data

					} catch (IOException e1) {
						
						label.setText("Error in opening file.  Try again and check if it is in a CSV format");
					} catch (FileException e2) {
						label.setText("One of the row lengths is not the correct length");
					} catch (Exception e1) {
						
						label.setText("The input file is not correct.  Make sure it only has numbers");
					}

				} else {
					label.setText("Not added a file for training data or not added number of neighbours needed");
				}

			}
		});

		classificationKNN.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {

				if (inputData != null && !neighboursWanted.getText().isEmpty()) {
					label.setText("Computing");
					ArrayList<Double> input = null;
					try {
						input = openFile();
						FileChooser fileChooser = new FileChooser();
						fileChooser.setTitle("Open Training Data");
						Stage stage = sceneChooser.getStage();
						File files = fileChooser.showOpenDialog(stage); // allow user to open file
						CSVFiles formattor = new CSVFiles(files, input.size() + 1); // makes a formatter to use
						ArrayList<ArrayList<String>> data = null;
						try {
							data = formattor.readCSV();
							ArrayList<ArrayList<Double>> trainingData = formattor.convertData(data);
							try {
								Integer.parseInt(neighboursWanted.getText());
								KNearestNeighbour knn = new ClassificationKNN();
								Double output = knn.knn(input, Integer.parseInt(neighboursWanted.getText()),
										trainingData);

								if (output == null) {
									label.setText("There was no majority.  Choose another amount of neighbours");
									;
								} else {
									label.setText("The value for the input given and the training data is: " + output);
								}
							} catch (NumberFormatException e2) {
								label.setText("The neighbour input needs to be an integer");
								;
							}

						} catch (NumberFormatException e) {
							
							label.setText("The data is not all double");
						} // gets the data

					} catch (IOException e1) {
						
						label.setText("Error in opening file.  Try again and check if it is in a CSV format");
					} catch (FileException e2) {
						label.setText("One of the row lengths is not the correct length");
					} catch (Exception e1) {
						
						label.setText("Not added a file for training data or not added number of neighbours needed");
					}

				} else {
					label.setText("Not added a file for training data or not added number of neighbours needed");
				}
			}
		});
	}

	/**
	 * This will open the file and take the first line and convert to a double
	 * 
	 * @return The input line
	 * @throws Exception
	 */
	private ArrayList<Double> openFile() throws Exception { // TODO this can be melded with CSVFiles convert data

		CSVFiles reader = new CSVFiles(inputData);

		ArrayList<ArrayList<String>> data = null;
		try {
			data = reader.readCSV();
		} catch (IOException e) {
			
			throw e;
		} catch (FileException e) {
			throw e;
		}
		ArrayList<String> inputAsString = data.get(0);

		ArrayList<Double> actualInput = new ArrayList<Double>();

		for (String dataIn : inputAsString) {

			actualInput.add(Double.parseDouble(dataIn));
		}

		return actualInput;

	}
}
