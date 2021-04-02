package com.natlowis.ai.supervised.regression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.natlowis.ai.exceptions.FileException;
import com.natlowis.ai.fileHandaling.CSVFiles;


/**
 * This implements multivariate linear regression
 * 
 * @author low101043
 *
 */
public class LinearRegressionMultivariate implements Regression {

	private ArrayList<ArrayList<Double>> data; // The data to be used
	private double[] wValues; // The wValues which are being used
	private File file; // The file which holds the training data //TODO Maybe don't pass in file to
						// make better space usage

	/**
	 * This will get the data needed for this regression
	 */
	public LinearRegressionMultivariate() {

		super();
		data = new ArrayList<ArrayList<Double>>();
		getData(); // Will get the test data

	}

	/**
	 * This Constructor assumes you have the file
	 * 
	 * @param files             The file which has all the data
	 * @param multibleVariables The number of different variables needed
	 * @throws IOException
	 * @throws FileException
	 */
	public LinearRegressionMultivariate(File files, int multibleVariables)
			throws FileException, IOException, NumberFormatException {

		// Initialises all needed variables
		file = files;
		data = new ArrayList<ArrayList<Double>>();
		getData(multibleVariables + 1); // Gets the data from the file
	}

	@Override
	public double calculate(double[] inputs) {

		double predicted = 0; // This will be the predicted value from the inputs

		for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // For each W value

			double wValue = wValues[wValueIndex]; // Gets the w value

			double xValue = inputs[wValueIndex]; // Gets the corresponding x value

			predicted += wValue * xValue; // Works out the predicted value

		}
		return predicted;
	}

	@Override
	public void checkFunction() {

		double cost = 0; // The cost of the current w values used

		for (int j = 0; j < data.size(); j++) { // For each item in the training data

			double[] xValues = new double[data.get(j).size()]; // This will hold the x values

			// Sets the x values. Will set the bias bit as well
			for (int place = 0; place < xValues.length; place++) {

				if (place == 0) {
					xValues[place] = 1;

				} else {
					xValues[place] = data.get(j).get(place - 1);
				}
			}

			double yData = (double) data.get(j).get(data.get(j).size() - 1); // Gets the correct y value

			double predicted = 0; // Will hold the predicted value

			for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // For each W value and x value
																						// work out what the value
																						// should be

				double wValue = wValues[wValueIndex];

				double xValue = xValues[wValueIndex];

				predicted += wValue * xValue;

			}

			cost = cost + Math.pow((yData - predicted), 2.0); // This is (y- hw(x))**2
			
		}
		cost = cost / data.size(); // This will be the final cost.

	}

	@Override
	public void gradientDescent(int iterations, double alpha, int variableSize) {

		wValues = new double[variableSize + 1]; // This will hold all the W value

		for (int i = 0; i < iterations; i++) { // The gradient descent code for Logistic Regression.

			for (int j = 0; j < data.size(); j++) { // Going through each set of values

				double[] xValues = new double[data.get(j).size()]; // This will hold the x values

				// Sets the x values. Will set the bias bit as well
				for (int place = 0; place < xValues.length; place++) {

					if (place == 0) {
						xValues[place] = 1;

					} else {
						xValues[place] = data.get(j).get(place - 1);
					}
				}

				double yData = (double) data.get(j).get(data.get(j).size() - 1); // Gets the correct y value

				double predicted = 0; // Will hold the predicted value

				for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // Goes through each w value

					double wValue = wValues[wValueIndex]; // Gets the w value

					double xValue = xValues[wValueIndex]; // Gets the corresponding x value

					predicted += wValue * xValue; // Update predicted

				}

				double difference = (yData - predicted); // Works out the difference between the actual answer and
															// the
															// predicted answer

				for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // This updates all the w
																							// values

					double wValue = wValues[wValueIndex];

					double xData = xValues[wValueIndex];

					wValue += alpha * difference * xData;

					wValues[wValueIndex] = wValue;

				}

			}

		}

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
	public double[] answers() {
		return wValues;

	}

	@Override
	public double cost() {

		double cost = 0; // The cost of the current w values used

		for (int j = 0; j < data.size(); j++) { // For each item in the training data

			double[] xValues = new double[data.get(j).size()]; // This will hold the x values

			// Sets the x values. Will set the bias bit as well
			for (int place = 0; place < xValues.length; place++) {

				if (place == 0) {
					xValues[place] = 1;

				} else {
					xValues[place] = data.get(j).get(place - 1);
				}
			}

			double yData = (double) data.get(j).get(data.get(j).size() - 1); // Gets the correct y value

			double predicted = 0; // Will hold the predicted value

			for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // For each W value and x value
																						// work out what the value
																						// should be

				double wValue = wValues[wValueIndex];

				double xValue = xValues[wValueIndex];

				predicted += wValue * xValue;

			}

			cost = cost + Math.pow((yData - predicted), 2.0); // This is (y- hw(x))**2

		}
		cost = cost / data.size(); // This will be the final cost.

		return cost;
	}

	@Override
	public void getData() {
		// TODO Auto-generated method stub WHat to do with this???

	}

}
