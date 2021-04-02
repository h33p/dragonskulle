package com.natlowis.ai.supervised.regression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.natlowis.ai.exceptions.FileException;
import com.natlowis.ai.fileHandaling.CSVFiles;
import com.natlowis.ai.testData.makeLogisticRegressionData;

/**
 * This class will implement Logistic Regression which is linear or non linear
 * HOWEVER it does not work
 * 
 * @author low101043
 *
 */
public class LogisticRegressionUnivariate extends LogisticRegression implements Regression {

	private static final Logger logger = Logger.getLogger(LogisticRegressionUnivariate.class);
	private ArrayList<ArrayList<Double>> data; // The data to be used
	private double[] wValues; // The wValues which are being used
	private File file; // The file which holds the training data //TODO Maybe don't pass in file to
						// make better space usage

	/**
	 * Used if have no data but want to see it work
	 */
	public LogisticRegressionUnivariate() {

		super();
		logger.fatal(
				"Stop here this Class does not work at the moment.  IF you want to do Logistic Regression please use the Multivariable one which can do linear seperable stuff.");
		data = new ArrayList<ArrayList<Double>>();
		getData();

	}

	/**
	 * This Constructor assumes you have the file which has all the data.
	 * 
	 * @param files
	 */
	public LogisticRegressionUnivariate(File files) throws FileException, IOException, NumberFormatException {

		// Initialises the variables
		file = files;
		data = new ArrayList<ArrayList<Double>>();
		try {
			getData(3);
		} catch (NumberFormatException | FileException | IOException e) {
			
			throw e;
		}
	}

	@Override
	public void gradientDescent(int iterations, double alpha, int polynomialSize) {

		wValues = new double[polynomialSize + 2]; // This will hold the W values

		for (int i = 0; i < iterations; i++) { // The gradient descent code for Logistic Regression.

			for (int j = 0; j < data.size(); j++) { // Going through each triplet of values

				double xData = data.get(j).get(0); // Get all the values out of the file
				double x2Data = data.get(j).get(1);
				double yData = data.get(j).get(2);

				double predicted = 0; // Working out the predicted value of
										// whether it is in a set or not.

				for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) { // Will go through all the W
																							// values working out what
																							// the W value is

					double wValue = wValues[wValueIndex];

					if (wValueIndex == 1) { // This is the data which is used by the x2Data
						predicted += wValue * x2Data;
					}

					else if (wValueIndex == 0) { // For the bias bit

						double xToPower = Math.pow(xData, 0);

						predicted += wValue * xToPower;
					}

					else { // For all other values

						double xToPower = Math.pow(xData, wValueIndex - 1);

						predicted += wValue * xToPower;
					}

				}

				double finalPredicted = activationFunction(predicted);
				double difference = (yData - finalPredicted); // Works out the difference between the actual answer and
																// the
																// predicted answer

				for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) {

					double wValue = wValues[wValueIndex];

					if (wValueIndex == 1) {
						wValue += alpha * difference * x2Data;
					}

					else if (wValueIndex == 0) {
						double xToPower = Math.pow(xData, 0);

						wValue += alpha * wValue * xToPower;
					}

					else {
						double xToPower = Math.pow(xData, wValueIndex);

						wValue += alpha * difference * xToPower;
					}
					wValues[wValueIndex] = wValue;

				}

			}

		}
	}

	@Override
	public void getData() {

		// Used for testing

		makeLogisticRegressionData a = new makeLogisticRegressionData();

		double[][] trainingData = a.makeValues();
		for (double[] item : trainingData) {

			ArrayList<Double> dataToAdd = new ArrayList<Double>();
			for (double number : item) {
				dataToAdd.add(number);
			}

			data.add(dataToAdd);
		}

	}

	
	@Override
	public double calculate(double inputs[]) {  //TODO Actually write this code
		
		return 0;
	}

	@Override
	public void checkFunction() {
		
		double cost = 0;
		for (int j = 0; j < data.size(); j++) { // This part of the code will just output the final predicted
			// values against the actual values. Used for debugging.
			double xData = (double) data.get(j).get(0); // Assume only 2 inputs
			double x2Data = data.get(j).get(1);
			double yData = (double) data.get(j).get(2);
			double predicted = 0; // Working out the predicted value of
			// whether it is in a set or not. The
			// equation is hW(X) = g(w0 + w1x1 +
			// w2x2)

			for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) {

				double wValue = wValues[wValueIndex];

				if (wValueIndex == 1) {
					predicted += wValue * x2Data;
				} else {
					double xToPower = Math.pow(xData, wValueIndex);

					predicted += wValue * xToPower;
				}

			}
			double finalPredicted = activationFunction(predicted);

			if (yData == 1.0) {
				cost += yData * Math.log10(finalPredicted);
			} else {
				cost += (1 - yData) * Math.log10(1 - finalPredicted);
			}

			System.out.println("Predicted: " + finalPredicted + " Actual: " + yData);
		}
		cost = -(cost / data.size()); // This will be the final cost.

		for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) {

			double wValue = wValues[wValueIndex];
			System.out.println(wValue);

		}
		System.out.println(cost); // This
									// prints
									// the
									// final
									// equation
									// out

	}

	@Override
	public double[] answers() {

		return wValues;
	}

	@Override
	public double cost() {
	
		double cost = 0;
		for (int j = 0; j < data.size(); j++) { // This part of the code will just output the final predicted
			// values against the actual values. Used for debugging.
			double xData = (double) data.get(j).get(0); // Assume only 2 inputs
			double x2Data = data.get(j).get(1);
			double yData = (double) data.get(j).get(2);
			double predicted = 0; // Working out the predicted value of
			// whether it is in a set or not. The
			// equation is hW(X) = g(w0 + w1x1 +
			// w2x2)

			for (int wValueIndex = 0; wValueIndex < wValues.length; wValueIndex++) {

				double wValue = wValues[wValueIndex];

				if (wValueIndex == 1) {
					predicted += wValue * x2Data;
				} else {
					double xToPower = Math.pow(xData, wValueIndex);

					predicted += wValue * xToPower;
				}

			}
			double finalPredicted = activationFunction(predicted);

			if (yData == 1.0) {
				cost += yData * Math.log10(finalPredicted);
			} else {
				cost += (1 - yData) * Math.log10(1 - finalPredicted);
			}

		}
		cost = -(cost / data.size()); // This will be the final cost.
		return cost;
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

}
