package com.natlowis.ai.supervised.regression;

/**
 * This class just has the activation function which is being used
 * 
 * @author low101043
 *
 */
public abstract class LogisticRegression implements Regression {

	/**
	 * This is the activation function for the class. It is the Sigmoid Function
	 * 
	 * @param data The data to be changed
	 * @return the data after is has gone through the sigmoid function
	 */
	protected double activationFunction(double data) {
		double function = 1 / (1 + Math.exp(-data)); // This works out the sigmoid value which is needed.
		return function;
	}

}
