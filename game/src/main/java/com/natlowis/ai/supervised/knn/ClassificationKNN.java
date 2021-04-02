package com.natlowis.ai.supervised.knn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class which uses KNN to find the class the input data should be in
 * 
 * @author low101043
 *
 */
public class ClassificationKNN extends KNearestNeighbour {

	/**
	 * The basic constructor.
	 */
	public ClassificationKNN() {
		super();
	}

	/**
	 * This will return null if there is no majority vote
	 */
	@Override
	public Double knn(ArrayList<Double> input, int kNeighbours, ArrayList<ArrayList<Double>> trainingData) {

		trainingData.add(input); // Adds the input to the training data and change the name
		inputs = trainingData;
		addingToList(); // Adds data

		List<double[]> firstKNeighbours = D.subList(0, kNeighbours); // Gets the first k neighbours

		HashMap<Double, Integer> findMax = new HashMap<Double, Integer>(); // Will find the max

		for (double[] item : firstKNeighbours) { // For each set of data

			double classIn = item[1]; // Finds the class
			Integer total = findMax.get(classIn); // Gets the class

			if (total == null) { // If the class does not exist

				findMax.put(classIn, 1); // Adds it

			} else { // If the class does exist

				total += 1; // increments the total
				findMax.put(classIn, total); // Adds back in the class
			}
		}

		int max = -1; // The max at the moment
		double classToOutput = -5; // The class to output
		boolean clash = false; // If there is a clash

		for (Map.Entry<Double, Integer> entry : findMax.entrySet()) { // For each data entry in the hash map

			if (entry.getValue() > max) { // If the total is larger than the lastest

				max = entry.getValue(); // Change max to that
				classToOutput = entry.getKey(); // Change class to that key
				clash = false; // Set clash to false

			} else if (entry.getValue() == max) { // If the entry value is the same total
				clash = true; // Set clash to true
			}
		}

		if (clash == true) { // If clash is true return null
			return null;
		} else { // Or return the class to output
			return classToOutput;
		}
	}

}
