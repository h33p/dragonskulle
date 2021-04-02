package com.natlowis.ai.ui.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.natlowis.ai.exceptions.FileException;
import com.natlowis.ai.exceptions.GraphException;
import com.natlowis.ai.exceptions.GraphNodeException;
import com.natlowis.ai.fileHandaling.CSVFiles;
import com.natlowis.ai.graphs.Graph;
import com.natlowis.ai.optimisation.antcolony.AntColonyOptimisation;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Optimisation extends Application implements Window {

	private Button backHome; // Button to take back home
	private TextField startNode; // The start node
	private TextField epoch; // The iterations
	private TextField pheromoneLevel; // The evaporation rate
	private TextField endState; // The end node
	private TextField numOfAnts; // The number of ants wanted
	private Button submit; // Button to submit
	private Button clear; // Button to clear
	private Label label; // Output label
	private String instructions;

	/**
	 * The Constructor which makes the scene
	 * 
	 * @param sceneChooser The ScreenController being used
	 */
	public Optimisation(ScreenController sceneChooser) {
		// Sets ups the main pane
		BorderPane root = new BorderPane();

		// Sets up the back home button
		backHome = new Button("Go To Home Page");
		root.setLeft(backHome);

		// Creating a GridPane container
		GridPane grid = new GridPane();
		grid.setPadding(new Insets(10, 10, 10, 10));
		grid.setVgap(5);
		grid.setHgap(5);

		// Defining the startNOde text field
		startNode = new TextField();
		startNode.setPromptText("Enter the start node value");
		startNode.setPrefColumnCount(10);
		startNode.getText();
		GridPane.setConstraints(startNode, 0, 0);
		grid.getChildren().add(startNode);

		// Defining the endState text field
		endState = new TextField();
		endState.setPrefColumnCount(15);
		endState.setPromptText("Enter the end state");
		GridPane.setConstraints(endState, 0, 1);
		grid.getChildren().add(endState);

		// Defining the epoch text field
		epoch = new TextField();
		epoch.setPromptText("Enter the number of iterations");
		GridPane.setConstraints(epoch, 0, 2);
		grid.getChildren().add(epoch);

		// Defining the pheromone text field
		pheromoneLevel = new TextField();
		pheromoneLevel.setPrefColumnCount(15);
		pheromoneLevel.setPromptText("Enter the evapouration rate");
		GridPane.setConstraints(pheromoneLevel, 0, 3);
		grid.getChildren().add(pheromoneLevel);

		// Defining the numOfAnts text field
		numOfAnts = new TextField();
		numOfAnts.setPrefColumnCount(15);
		numOfAnts.setPromptText("Enter the number of ants to use");
		GridPane.setConstraints(numOfAnts, 0, 4);
		grid.getChildren().add(numOfAnts);

		// Defining the Submit button
		submit = new Button("Submit");
		GridPane.setConstraints(submit, 1, 0);
		grid.getChildren().add(submit);

		// Defining the Clear button
		clear = new Button("Clear");
		GridPane.setConstraints(clear, 1, 1);
		grid.getChildren().add(clear);

		// Adding a Label 
		instructions = "Please enter these figures: \n " + "-> The start node as an integer \n "
				+ "-> The end node as an integer \n" + "-> The number of iterations as an integer \n"
				+ "-> The evaporation rate as a number between 0 and 1 \n" + "-> The number of ants to use \n"
				+ "-> Press Submit.  This will ask you to enter a file (Please by a csv or txt with values seperated with commas) \n\t"
				+ "-> Each row designates 1 Connection.  Must be in this order: origin node, end node, weight \n"
				+ "This program at the moment assumes all weights are the same.  This will be updated at a later point";
		label = new Label(instructions);
		GridPane.setConstraints(label, 0, 5);
		GridPane.setColumnSpan(label, 2);
		grid.getChildren().add(label);

		// Adding all buttons and label to scene
		root.setCenter(grid);

		sceneChooser.addScreen("Optimisation", root, this); // Adds the screen to the ScreenController
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

	}

	@Override
	public void controls(ScreenController sceneChooser) {

		// Takes the user back to the main page
		backHome.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				// Clears all inputs
				startNode.clear();
				epoch.clear();
				pheromoneLevel.clear();
				numOfAnts.clear();
				endState.clear();
				label.setText(instructions);
				sceneChooser.activate("Main Page"); // activates the main page
				return;
			}
		});

		// Setting an action for the Submit button
		submit.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent e) {

				if ((!startNode.getText().isEmpty() && !epoch.getText().isEmpty() && !pheromoneLevel.getText().isEmpty()
						&& !endState.getText().isEmpty() && !numOfAnts.getText().isEmpty())) { // Checks if there is an
					label.setText("Loading"); // input
					try {
						Double.parseDouble(pheromoneLevel.getText());
						if (Double.parseDouble(pheromoneLevel.getText()) < 1
								&& Double.parseDouble(pheromoneLevel.getText()) > 0) { // If the evaporation rate is
																						// between
																						// 0 and 1

							try {
								ArrayList<ArrayList<String>> data = getData(sceneChooser, 3); 

								if (data != null) {
									Graph graph = null;
									try {
										graph = new Graph(data);
										// Makes a graph

										// Performs ACO
										try {
											Integer.parseInt(startNode.getText());
											Integer.parseInt(endState.getText());
											Integer.parseInt(epoch.getText());
											Double.parseDouble(pheromoneLevel.getText());
											Integer.parseInt(numOfAnts.getText());
											if (graph.inGraph(Integer.parseInt(endState.getText()))
													&& graph.inGraph(Integer.parseInt(startNode.getText()))) {
												AntColonyOptimisation aco = new AntColonyOptimisation(graph);
												aco.AntColonyOptimisationAlgorithm(
														Integer.parseInt(startNode.getText()),
														Integer.parseInt(endState.getText()),
														Integer.parseInt(epoch.getText()),
														Double.parseDouble(pheromoneLevel.getText()),
														Integer.parseInt(numOfAnts.getText()));

												ArrayList<Integer> route = aco.finalRoute();

												String output = "Nodes Visited in order: "; // The final output

												for (int node : route) { // Adds all nodes to visit
													output += node + ", ";
												}

												label.setText(output);
											} else if (graph.inGraph(Integer.parseInt(endState.getText()))
													&& !graph.inGraph(Integer.parseInt(startNode.getText()))) {
												label.setText("The Start Node inputted is not in the graph");
											} else if (!graph.inGraph(Integer.parseInt(endState.getText()))
													&& graph.inGraph(Integer.parseInt(startNode.getText()))) {
												label.setText("The End Node inputted is not in the graph");
											} else {
												label.setText(
														"Both the start node and the end node is not in the graph");
											}

										} catch (NumberFormatException e6) {
											label.setText("The data inputted in above boxes is not numbers");
										}
									}

									catch (GraphException e5) {
										label.setText(
												"Tried adding multiple connections between same node or other problems(Check graph will fit spec) ");
										;
									} catch (GraphNodeException e4) {
										label.setText("Added multiple of the same node");
										;
									} catch (NumberFormatException e6) {
										label.setText("Tried adding something which is not a number");
										;
									}
								} else {
									label.setText("Choose a File");
								}
							} catch (FileException e2) {
								label.setText("One of the rows is not the right length");
							} catch (IOException e3) {
								label.setText("There is an error finding the file or reading the file");
							}
						} else {
							label.setText("The evapouration needs be between 0 and 1");
						}
					} catch (NumberFormatException e1) {
						label.setText("The Evapouration rate must be a number");
					}

				} else {
					label.setText("You have not inputted all data.");
				}
			}
		});

		// Setting an action for the Clear button
		clear.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent e) {
				startNode.clear();
				epoch.clear();
				pheromoneLevel.clear();
				numOfAnts.clear();
				endState.clear();
				label.setText(instructions);
			}
		});

	}

	/**
	 * Gets the data needed to be used
	 * 
	 * @param sceneChooser The ScreenController used
	 * @param number       The number of columns it should have
	 * @return The data in an arraylist of arraylist of strings,
	 * @throws FileException, IOException
	 */
	private ArrayList<ArrayList<String>> getData(ScreenController sceneChooser, int number)
			throws FileException, IOException {
		// Opens the file to use
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Data File");
		Stage stage = sceneChooser.getStage();
		File files = fileChooser.showOpenDialog(stage);

		if (files == null) {
			return null;
		}

		CSVFiles formattor = new CSVFiles(files, number); // makes a formatter to use
		ArrayList<ArrayList<String>> data = null;
		try {
			data = formattor.readCSV();
		} catch (FileException | IOException e) {
			
			throw e;
		} // gets the data

		return data;
	}

}
