package com.natlowis.ai.testData;

import java.util.Random;

/**
 * This is used to make random numbers to add.
 * 
 * @author low101043
 *
 */
public class makeLogisticRegressionData {

	public static void main(String[] args) {
		Random rand = new Random();
		final int size = 100;

		double[][] inputs = new double[size][];

		for (int i = 0; i < size; i++) {
			double[] inputsSmall = new double[3];
			double randomNumberX = rand.nextDouble();
			double randomNumberY = rand.nextDouble();
			double actualY = randomNumberX + (5 * randomNumberX) + (2 * Math.pow(randomNumberX, 2.0)); // makes the y
																										// value

			double dis = actualY - randomNumberY;
			double actualOutput = -1;
			if (dis < 0) {
				actualOutput = 0;
			} else {
				actualOutput = 1;
			}

			inputsSmall[0] = randomNumberX;
			inputsSmall[1] = randomNumberY;
			inputsSmall[2] = actualOutput;

			inputs[i] = inputsSmall;
		}

		args = null;
	}

	public double[][] makeValues() {
		Random rand = new Random();
		final int size = 100;

		double[][] inputs = new double[size][];

		for (int i = 0; i < size; i++) {
			double[] inputsSmall = new double[3];
			double randomNumberX = rand.nextDouble();
			double randomNumberY = rand.nextDouble();
			double actualY = randomNumberX + (5 * randomNumberX) + (2 * Math.pow(randomNumberX, 2.0));

			double dis = actualY - randomNumberY;
			double actualOutput = -1;
			if (dis < 0) {
				actualOutput = 0;
			} else {
				actualOutput = 1;
			}

			inputsSmall[0] = randomNumberX;
			inputsSmall[1] = randomNumberY;
			inputsSmall[2] = actualOutput;

			inputs[i] = inputsSmall;
		}
		return inputs;
	}
}
