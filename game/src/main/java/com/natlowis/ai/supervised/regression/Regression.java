package com.natlowis.ai.supervised.regression;

import java.io.IOException;

import com.natlowis.ai.exceptions.FileException;
import com.natlowis.ai.exceptions.RegressionException;

/**
 * This is the interface which specifies all the methods for a Regression
 * algorithm
 * 
 * @author low101043
 *
 */
public interface Regression {

	/**
	 * This will return the vector of W values.
	 * 
	 * @return A 2D array of type double which has the W values.
	 */
	public double[] answers();

	/**
	 * This will implement Gradient Descent for that regression
	 * 
	 * @param iterations     The number of iterations used
	 * @param alpha          The alpha value used
	 * @param polynomialSize The polynomial size to use or the number of independent
	 *                       values to use
	 */
	public void gradientDescent(int iterations, double alpha, int polynomialSize);

	/**
	 * This will get the data whether from a file or another place
	 */
	public void getData();

	/**
	 * This will calculate the what the data is
	 * 
	 * @param inputs The set of inputs for the data
	 * @return The yData for the input
	 * @throws RegressionException
	 */
	public double calculate(double[] inputs) throws RegressionException, RegressionException;

	/**
	 * This will check the function
	 */
	public void checkFunction();

	/**
	 * This will return the cost of the W values
	 * 
	 * @return The cost of W values
	 */
	public double cost();

	/**
	 * This will get data from a File
	 * 
	 * @param varaibleSize the number of variables needed
	 * @throws IOException
	 * @throws FileException
	 */
	public void getData(int varaibleSize) throws FileException, IOException, NumberFormatException; 

}
