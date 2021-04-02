package com.natlowis.ai.supervised.knn;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will do regression using KNN.
 * 
 * @author low101043
 *
 */
public class RegressionKNN extends KNearestNeighbour {

	public RegressionKNN() {
		super();
	}

	@Override
	public Double knn(ArrayList<Double> input, int kNeighbours, ArrayList<ArrayList<Double>> trainingData) {

		trainingData.add(input); // Add input to trianing data
		inputs = trainingData;
		addingToList(); // Sort data

		List<double[]> firstKNeighbours = D.subList(0, kNeighbours); // Get the sublist

		// Works out average
		double total = 0;
		for (double[] item : firstKNeighbours) {
			total += item[1];
		}
		double average = total / (double) firstKNeighbours.size();

		return average;
	}

}
