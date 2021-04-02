package com.natlowis.ai.supervised.regression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.natlowis.ai.exceptions.FileException;
import com.natlowis.ai.fileHandaling.CSVFiles;

/**
 * This will implement logistic regression with multiple variables.
 * 
 * @author low101043
 *
 */
public class LogisticRegressionMultivariate extends LogisticRegression implements Regression {

	//private static final Logger logger = Logger.getLogger(LogisticRegressionMultivariate.class);

	private ArrayList<ArrayList<Double>> data; // The data to be used
	private double[] wValues; // The wValues which are being used
	private File file; // The file which holds the training data //TODO Maybe don't pass in file to
						// make better space usage

	/**
	 * The default constructor. This just gets the data for the function
	 */
	public LogisticRegressionMultivariate() {
		super();
		data = new ArrayList<ArrayList<Double>>();
		getData(); // This gets data where I know the values

	}

	/**
	 * This Constructor should be used if you have the file with the data in it and
	 * know how many variables you need.
	 * 
	 * @param files             The file with the data in it
	 * @param multibleVariables The number of variables used
	 */
	public LogisticRegressionMultivariate(File files, int multibleVariables)
			throws FileException, IOException, NumberFormatException {

		// Initialises the variables
		file = files;
		data = new ArrayList<ArrayList<Double>>();
		try {
			getData(multibleVariables);
		} catch (NumberFormatException | FileException | IOException e) {
			
			throw e;
		} // Get the correct user inputs
	}

	@Override
	public void gradientDescent(int iterations, double alpha, int variableSize) {

		wValues = new double[variableSize + 1]; // This will hold the vector of W

		for (int i = 0; i < iterations; i++) { // The gradient descent code for Logistic Regression.

			for (int j = 0; j < data.size(); j++) { // Going through each triplet of values

				double[] xValues = new double[data.get(j).size()]; // This will hold the x values

				// Sets the x values. Will set the bias bit as well
				for (int place = 0; place < xValues.length; place++) {

					if (place == 0) {
						xValues[place] = 1;

					} else {
						xValues[place] = data.get(j).get(place - 1);
					}
				}
				double yData = data.get(j).get(data.get(j).size() - 1); // Sets the y value

				double predicted = 0; // Working out the predicted value of
										// whether it is in a set or not.

				for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // Will work out the current
																							// predicted value with the
																							// current W values

					double wValue = wValues[wValueIndex];

					double xValue = xValues[wValueIndex];

					predicted += wValue * xValue;

				}

				double finalPredicted = activationFunction(predicted); // work out the final predicted value
				double difference = (yData - finalPredicted); // Works out the difference between the actual answer and
																// the
																// predicted answer

				for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // Updates the W values

					double wValue = wValues[wValueIndex];

					double xData = xValues[wValueIndex];

					wValue += alpha * difference * xData;

					wValues[wValueIndex] = wValue;

				}

			}

		}

	}

	@Override
	public void getData() {

		// This is used for testing purposes
		double[][] trainingData = { { 1, 1, 0 }, { 2, 2, 0 }, { 0.3, 1.2, 0 }, { .6, .8, 0 }, { 1.2, 1, 0 },
				{ 1.3, 1, 0 }, { 1.8, 2, 0 }, { 1.5, 1.4, 0 }, { 3, 3, 1 }, { 4, 4, 1 }, { 3.1, 3.3, 1 },
				{ 3.6, 3.8, 1 }, { 3.8, 2.1, 1 }, { 3.5, 2.2, 1 }, { 3.25, 2.8, 1 } };

		for (double[] item : trainingData) {

			ArrayList<Double> dataToAdd = new ArrayList<Double>();
			for (double number : item) {
				dataToAdd.add(number);
			}

			data.add(dataToAdd);

		}

		return;
	}

	@Override
	public void getData(int variableSize) throws FileException, IOException, NumberFormatException {

		CSVFiles formattor = new CSVFiles(file, variableSize); // Makes a new formatter object
		ArrayList<ArrayList<String>> dataToUse = null;
		try {
			dataToUse = formattor.readCSV();
		} catch (FileException | IOException e) {
			
			throw e;
		} // Get all the data

		try {
			data = formattor.convertData(dataToUse);
		} catch (NumberFormatException e) {
			throw e;
		}
	}

	@Override
	public double calculate(double[] inputs) {
		double answer = 0;

		for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) {

			double wValue = wValues[wValueIndex];

			double xValue = inputs[wValueIndex];

			answer += wValue * xValue;

		}
		double finalPredicted = activationFunction(answer);

		if (finalPredicted <= 0.5)
			finalPredicted = 0;
		else
			finalPredicted = 1;
		return finalPredicted;
	}

	@Override
	public void checkFunction() {

		double cost = 0; // Will hold the cost of the current W values

		for (int j = 0; j < data.size(); j++) { // This part of the code will just output the final predicted
												// values against the actual values.

			double[] xValues = new double[data.get(j).size()]; // This will hold the x values

			// Sets the x values. Will set the bias bit as well
			for (int place = 0; place < xValues.length; place++) {

				if (place == 0) {
					xValues[place] = 1;

				} else {
					xValues[place] = data.get(j).get(place - 1);
				}
			}
			double yData = data.get(j).get(data.get(j).size() - 1); // Sets the y value

			double predicted = 0; // Working out the predicted value of
			// whether it is in a set or not.

			for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // Work out the predicted value
																						// with current W values

				double wValue = wValues[wValueIndex];

				double xValue = xValues[wValueIndex];

				predicted += wValue * xValue;

			}

			double finalPredicted = activationFunction(predicted); // Pass it through the activation function

			// Split the data on what the actual yData to correctly work out cost
			if (yData == 1.0) {

				cost += yData * Math.log10(finalPredicted);

			} else {

				cost += (1 - yData) * Math.log10(1 - finalPredicted);
			}

			// System.out.println("Predicted: " + predicted + " Actual: " + yData); 
			
		}

		cost = cost / data.size(); // This will be the final cost.

	}

	@Override
	public double[] answers() {

		return wValues;
	}

	@Override
	public double cost() {

		double cost = 0; // Will hold the cost

		for (int j = 0; j < data.size(); j++) { // This part of the code will just output the final predicted
												// values against the actual values.

			double[] xValues = new double[data.get(j).size()]; // This will hold the x values

			// Sets the x values. Will set the bias bit as well
			for (int place = 0; place < xValues.length; place++) {

				if (place == 0) {
					xValues[place] = 1;

				} else {
					xValues[place] = data.get(j).get(place - 1);
				}
			}
			double yData = data.get(j).get(data.get(j).size() - 1); // Sets the y value

			double predicted = 0; // Working out the predicted value of
			// whether it is in a set or not.

			for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // Work out the predicted value
																						// with current W values

				double wValue = wValues[wValueIndex];

				double xValue = xValues[wValueIndex];

				predicted += wValue * xValue;

			}

			double finalPredicted = activationFunction(predicted); // Pass it through the activation function

			// Split the data on what the actual yData to correctly work out cost
			if (yData == 1.0) {

				cost += yData * Math.log10(finalPredicted);

			} else {

				cost += (1 - yData) * Math.log10(1 - finalPredicted);
			}

		}
		cost = -(cost / data.size()); // This will be the final cost.

		return cost;
	}

}
