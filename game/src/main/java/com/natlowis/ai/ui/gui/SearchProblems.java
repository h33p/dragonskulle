package com.natlowis.ai.ui.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.natlowis.ai.exceptions.FileException;
import com.natlowis.ai.exceptions.GraphException;
import com.natlowis.ai.exceptions.GraphNodeException;
import com.natlowis.ai.fileHandaling.CSVFiles;
import com.natlowis.ai.graphs.Connection;
import com.natlowis.ai.graphs.Graph;
import com.natlowis.ai.search.SearchAlgorithm;
import com.natlowis.ai.search.informed.AStar;
import com.natlowis.ai.search.uninformed.BreadthFirstSearch;
import com.natlowis.ai.search.uninformed.DepthFirstSearch;

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
 * This Screen will show all the different search algorithms
 * 
 * @author low101043
 *
 */
public class SearchProblems extends Application implements Window {

	// All the variables needed
	private Button backHome;
	private Button dfsButton;
	private Button bfsButton;
	private Button AStarButton;
	private TextField startNode;
	private TextField endNode;
	private Label label;
	private String instructions;

	/**
	 * The Constructor which sets up the screen
	 * 
	 * @param sceneChooser The {@code ScreenController} which holds all the pages
	 *                     being used
	 */
	public SearchProblems(ScreenController sceneChooser) {

		// Sets the root page and the home button
		BorderPane root = new BorderPane();
		backHome = new Button("Go back home");
		root.setLeft(backHome);

		// Adds a label
		root.setTop(new Label("Search Algorithms"));

		// Defining the GridPane used
		GridPane grid = new GridPane();
		grid.setPadding(new Insets(10, 10, 10, 10));
		grid.setVgap(5);
		grid.setHgap(5);

		// Defining the buttons needed
		dfsButton = new Button("Depth First Search");
		bfsButton = new Button("Breadth First Search");
		AStarButton = new Button("A* Algorithm");

		// Defining the StartNode Text Field
		startNode = new TextField();
		startNode.setPromptText("Enter the start node");
		startNode.setPrefColumnCount(10);
		startNode.getText();
		GridPane.setConstraints(startNode, 0, 1);
		grid.getChildren().add(startNode);

		// Defining the endNode TextField
		endNode = new TextField();
		endNode.setPromptText("Enter the end node");
		endNode.setPrefColumnCount(10);
		endNode.getText();
		GridPane.setConstraints(endNode, 0, 2);
		grid.getChildren().add(endNode);

		// Defining the output label
		instructions = "These are the Search Problems.  To use: \n" + "-> Enter the start node as an integer \n"
				+ "-> Enter the end node as an integer \n" + "-> Choose your algorithm.\n"
				+ "If you chose Breadth First Search or Depth First Search \n"
				+ "\t-> Enter a file where each value is seperated by commas and has your graph in it \n\t"
				+ "-> Each row will have one connection in this order: Origin Node, Destination Node, Weight"
				+ "\n If you chose A* Algorithm \n"
				+ "\t-> Enter a file where each value is seperated by commas and has your nodes fro the graph in it \n\t"
				+ "-> This file will have the node and the heuristic to the end node in it \n\t"
				+ "-> The second file will have your connections with each row habing it in this order: Origin Node, Destination Node, Weight";
		label = new Label(instructions);
		GridPane.setConstraints(label, 0, 7);
		GridPane.setColumnSpan(label, 2);
		grid.getChildren().add(label);

		// Adds everything to the scene
		HBox choices = new HBox();
		choices.getChildren().addAll(dfsButton, bfsButton, AStarButton);
		GridPane.setConstraints(choices, 0, 5);
		grid.getChildren().add(choices);
		root.setCenter(grid);

		sceneChooser.addScreen("Search Problems Page", root, this); // Adds it to the screen changer
	}

	@Override
	public void controls(ScreenController sceneChooser) {

		// Takes user back to the main page
		backHome.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				// Clears the screen
				startNode.clear();
				endNode.clear();
				label.setText(instructions);
				sceneChooser.activate("Main Page"); // Activates home page screen
				return;
			}
		});

		

		// Does Depth First Search
		dfsButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {

				label.setText("Loading");
				if ((!startNode.getText().isEmpty() && !endNode.getText().isEmpty())) { // Checks we have inputs

					try {
						ArrayList<ArrayList<String>> data = getData(sceneChooser, 3);
						Graph graph = null;
						try {
							graph = new Graph(data);
							// Makes a graph
							DepthFirstSearch dfs = new DepthFirstSearch(graph);
							try {

								Integer.parseInt(startNode.getText());
								Integer.parseInt(endNode.getText());
								try {
									if (graph.inGraph(Integer.parseInt(endNode.getText()))
											&& graph.inGraph(Integer.parseInt(startNode.getText()))) {
										dfs.algorithmToImplement(Integer.parseInt(startNode.getText()),
												Integer.parseInt(endNode.getText()));
										// Does DFS then output it

										label.setText(outputAll(dfs));
									} else if (!graph.inGraph(Integer.parseInt(endNode.getText()))
											&& graph.inGraph(Integer.parseInt(startNode.getText()))) {
										label.setText("The end Node is not in the graph");
									} else if (graph.inGraph(Integer.parseInt(endNode.getText()))
											&& !graph.inGraph(Integer.parseInt(startNode.getText()))) {
										label.setText("The start node is not in the graph");
									} else {
										label.setText("Both the start node and the end node is in the graph");
									}
								} catch (GraphNodeException e) {
									label.setText("If you've got here I have no idea.");

								}
							} catch (NumberFormatException e) {
								label.setText("The start node or the end node is not an integer");
								;
							}

						} catch (NumberFormatException e) {
							label.setText("The numbers in your file is not actual numbers");
							;
						} catch (GraphException e) {
							
							label.setText("You've tried adding multiple connections between nodes");
						} catch (GraphNodeException e) {
							
							label.setText("Added multiple of the same node (Somehow!)");
						}
					} catch (FileException e) {
						label.setText("Need 3 data on each row");
					} catch (IOException e) {
						label.setText("Cannot read file");
					}

				} else {
					label.setText("Please enter some data");
				}

			}
		});

		// Does Breadth First Search
		bfsButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {

				label.setText("Loading");
				if ((!startNode.getText().isEmpty() && !endNode.getText().isEmpty())) { // Checks we have inputs
					try {
						ArrayList<ArrayList<String>> data = getData(sceneChooser, 3);
						Graph graph = null;
						try {
							graph = new Graph(data);
							// Makes a graph
							SearchAlgorithm bfs = new BreadthFirstSearch(graph);
							try {

								Integer.parseInt(startNode.getText());
								Integer.parseInt(endNode.getText());
								try {
									if (graph.inGraph(Integer.parseInt(endNode.getText()))
											&& graph.inGraph(Integer.parseInt(startNode.getText()))) {
										bfs.algorithmToImplement(Integer.parseInt(startNode.getText()),
												Integer.parseInt(endNode.getText()));
										// Does DFS then output it

										label.setText(outputAll(bfs));
									} else if (!graph.inGraph(Integer.parseInt(endNode.getText()))
											&& graph.inGraph(Integer.parseInt(startNode.getText()))) {
										label.setText("The end Node is not in the graph");
									} else if (graph.inGraph(Integer.parseInt(endNode.getText()))
											&& !graph.inGraph(Integer.parseInt(startNode.getText()))) {
										label.setText("The start node is not in the graph");
									} else {
										label.setText("Both the start node and the end node is in the graph");
									}
								} catch (GraphNodeException e) {
									label.setText("If you've got here I have no idea.");

								}
							} catch (NumberFormatException e) {
								label.setText("The start node or the end node is not an integer");
								;
							}
						} catch (NumberFormatException e) {
							label.setText("The numbers in your file is not actual numbers");
							;
						} catch (GraphException e) {
							
							label.setText("You've tried adding multiple connections between nodes");
						} catch (GraphNodeException e) {
							
							label.setText("Added multiple of the same node (Somehow!)");
						}
					} catch (FileException e) {
						label.setText("Need 3 data on each row");
					} catch (IOException e) {
						label.setText("Cannot read file");
					}

				} else {
					label.setText("Please enter some data");
				}

			}
		});

		// Defining what do if AStar clicked
		AStarButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {

				if ((!startNode.getText().isEmpty() && !endNode.getText().isEmpty())) { // Checks we have inputs

					label.setText("Loading");
					try {
						ArrayList<ArrayList<String>> dataNodes = getData(sceneChooser, 2);
						try {
							ArrayList<ArrayList<String>> dataConnections = getData(sceneChooser, 3);

							Graph graph = null;
							try {
								graph = new Graph(dataNodes, dataConnections);
								// Makes a graph
								SearchAlgorithm aStar = new AStar(graph);
								try {

									Integer.parseInt(startNode.getText());
									Integer.parseInt(endNode.getText());
									try {
										if (graph.inGraph(Integer.parseInt(endNode.getText()))
												&& graph.inGraph(Integer.parseInt(startNode.getText()))) {
											aStar.algorithmToImplement(Integer.parseInt(startNode.getText()),
													Integer.parseInt(endNode.getText()));
											// Does DFS then output it

											label.setText(outputAll(aStar));
										} else if (!graph.inGraph(Integer.parseInt(endNode.getText()))
												&& graph.inGraph(Integer.parseInt(startNode.getText()))) {
											label.setText("The end Node is not in the graph");
										} else if (graph.inGraph(Integer.parseInt(endNode.getText()))
												&& !graph.inGraph(Integer.parseInt(startNode.getText()))) {
											label.setText("The start node is not in the graph");
										} else {
											label.setText("Both the start node and the end node is in the graph");
										}
									} catch (GraphNodeException e) {
										label.setText("If you've got here I have no idea.");

									}
								} catch (NumberFormatException e) {
									label.setText("The start node or the end node is not an integer");
									;
								}
							} catch (NumberFormatException e) {
								label.setText("The numbers in your file is not actual numbers");
								;
							} catch (GraphException e) {
								
								label.setText("You've tried adding multiple connections between nodes");
							} catch (GraphNodeException e) {
								
								label.setText("Added multiple of the same node (Somehow!)");
							}
						} catch (FileException e) {
							label.setText("Need 3 data on each row");
						} catch (IOException e) {
							label.setText("Cannot read file");
						}
					} catch (FileException e) {
						label.setText("Need 2 data on each row");
					} catch (IOException e) {
						label.setText("Cannot read file");
					}

				} else {
					label.setText("Please enter some text");
				}

			}
		});
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// Needed to be an application

	}

	/**
	 * Will output the solution
	 * 
	 * @param object The SearchAlgorithm which has done its algorithm
	 * @return A string which shows the solution
	 */
	private String outputAll(SearchAlgorithm object) {

		String output = "Nodes Visited in order: "; // The final output
		Integer[] nodes = object.nodesToVisit(); // The nodes in the solution
		Connection[] connections = object.solutionActions(); // The connections in the solution

		for (int node : nodes) { // Adds all nodes to visit
			output += node + ", ";
		}

		output += "\n" + "Connections for the solution: ";

		for (Connection connection : connections) { // Adds each connection which you have to do
			int originNode = connection.getOriginNode();
			int destinationNode = connection.getDestinationNode();
			double weight = connection.getWeight();

			output += "Connection(" + originNode + ", " + destinationNode + ", " + weight + "),  ";

		}

		return output;
	}

	private ArrayList<ArrayList<String>> getData(ScreenController sceneChooser, int number)
			throws FileException, IOException {
		// Opens the file to use
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Data File");
		Stage stage = sceneChooser.getStage();
		File files = fileChooser.showOpenDialog(stage);
		CSVFiles formattor = new CSVFiles(files, number); // makes a formatter to use
		ArrayList<ArrayList<String>> data = null;

		try {
			data = formattor.readCSV();
		} catch (FileException e) {
			
			throw e;
		} catch (IOException e) {
			
			throw e;
		}
		// gets the data

		return data;
	}

}
