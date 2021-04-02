package com.natlowis.ai.supervised.regression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.natlowis.ai.exceptions.FileException;
import com.natlowis.ai.exceptions.RegressionException;
import com.natlowis.ai.fileHandaling.CSVFiles;

/**
 * This implements linear and non linear regression with one variable
 * 
 * @author low101043
 *
 */
public class LinearRegressionUnivariate implements Regression {

	private ArrayList<ArrayList<Double>> data; // The data to be used
	private double[] wValues; // The wValues which are being used
	private File file; // The file which holds the training data //TODO Maybe don't pass in file to
						// make better space usage

	/**
	 * This is the default constructor. It will just get the data needed. It is
	 * basic data
	 */
	public LinearRegressionUnivariate() {

		// Initialises all the data needed
		super();
		data = new ArrayList<ArrayList<Double>>();
		getData(); // SOme practice data to use

	}

	/**
	 * This constructor is used if you have the file with the data.
	 * 
	 * @param files
	 * @throws NumberFormatException, FileException, IOException
	 */
	public LinearRegressionUnivariate(File files) throws NumberFormatException, FileException, IOException {

		// Initialises all the needed variables
		file = files;
		data = new ArrayList<ArrayList<Double>>();
		try {
			getData(2);
		} catch (NumberFormatException | FileException | IOException e) {
			
			throw e;
		} // Gets the correct data from the file
	}

	@Override
	public void gradientDescent(int iterations, double alpha, int polynomialSize) {

		wValues = new double[polynomialSize + 1]; // Will hold the vector of W values to use

		for (int i = 0; i < iterations; i++) { // This part of the code is the code for gradient descent.

			for (int j = 0; j < data.size(); j++) { // This goes over each pair of values in the training data

				double xData = (double) data.get(j).get(0); // Gets the x and y inputs

				double yData = (double) data.get(j).get(1);

				double predicted = 0; // This uses the formula h(x) parameterised by W
										// to work out the predicted y values given x
										// and the vector W.

				for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // This will work out with the
																							// current W values what the
																							// model would predict

					double wValue = wValues[wValueIndex];

					double xToPower = Math.pow(xData, wValueIndex);

					predicted += wValue * xToPower;

				}

				double difference = (yData - predicted); // The difference between the predicted value and the actual y
															// value

				for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // This updates all the w
																							// values.

					double wValue = wValues[wValueIndex];

					double xToPower = Math.pow(xData, wValueIndex);

					wValue += alpha * difference * xToPower;

					wValues[wValueIndex] = wValue;

				}
			}

		}

	}

	@Override
	public void getData() {

		// This is training data which I know the answer to (1,1)
		double[][] trainingData = { { -10, 91 }, { -3, 7 }, { 0, 1 }, { 1, 3 }, { 2, 7 }, { 3, 13 }, { 4, 21 },
				{ 10, 111 }, { -100, 9901 }, { 100, 10101 } };

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

		CSVFiles formattor = new CSVFiles(file, variableSize); // Makes a CSVFiles Object which can get data from a CSV
																// file
		ArrayList<ArrayList<String>> dataToUse = null;
		try {
			dataToUse = formattor.readCSV();
		} catch (IOException | FileException e) {
			
			throw e;
		} // Read the file

		try {
			data = formattor.convertData(dataToUse);
		} catch (NumberFormatException e) {
			throw e;
		}
	}

	@Override
	public double calculate(double inputs[]) throws RegressionException {


		if (inputs.length != 1) {
			throw new RegressionException();
		} else {

			double predicted = 0; // Will hold the predicted value
			for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // For every W value will work out
																						// wi *
																						// x^i and add to predicted

				double wValue = wValues[wValueIndex];

				double xToPower = Math.pow(inputs[wValueIndex], wValueIndex);

				predicted += wValue * xToPower;

			}

			return predicted;
		}

	}

	@Override
	public void checkFunction() {

		double cost = 0; // This will hold the current cost with the current W values

		for (int j = 0; j < data.size(); j++) { // This part of the code will just output the final predicted
												// values against the actual values.
			double xData = (double) data.get(j).get(0); // Get the two inputs

			double yData = (double) data.get(j).get(1);

			double predicted = 0; // This uses the formula h(x) parameterised by W
			// to work out the predicted y values given x
			// and the vector W.

			for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // This will work out with the
																						// current W values what the
																						// model would predict

				double wValue = wValues[wValueIndex];

				double xToPower = Math.pow(xData, wValueIndex);

				predicted += wValue * xToPower;

			}

			cost = cost + Math.pow((yData - predicted), 2.0); // This is (y- hw(x))**2
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

		double cost = 0;
		for (int j = 0; j < data.size(); j++) { // This part of the code will just output the final predicted
												// values against the actual values.
			double xData = (double) data.get(j).get(0); // Get the two inputs

			double yData = (double) data.get(j).get(1);

			double predicted = 0; // This uses the formula h(x) parameterised by W
			// to work out the predicted y values given x
			// and the vector W.

			for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // Works out the predicted value
																						// for the current W values

				double wValue = wValues[wValueIndex];

				double xToPower = Math.pow(xData, wValueIndex);

				predicted += wValue * xToPower;

			}

			cost = cost + Math.pow((yData - predicted), 2.0); // This is (y- hw(x))**2

		}
		cost = cost / data.size(); // This will be the final cost.

		return cost;
	}

}
