package com.natlowis.ai.supervised.knn;

import java.util.ArrayList;

/**
 * An <Code> abstract </code> class which designates how to find the nearest
 * neighbours for the data.
 * 
 * @author low101043
 *
 */
public abstract class KNearestNeighbour {

	protected ArrayList<ArrayList<Double>> inputs; // The inputs
	protected ArrayList<ArrayList<Double>> normalised; // The normalised data
	protected ArrayList<double[]> D; // The data sorted

	/**
	 * The basic constructor
	 */
	public KNearestNeighbour() {
		;
	}

	// TEST!!!
	/**
	 * This will normalise the inputs and the training data
	 */
	private void normalise() {

		normalised = new ArrayList<ArrayList<Double>>();

		int size = inputs.get(0).size(); // Gets the size of the inputs
		ArrayList<double[]> minMaxValues = new ArrayList<double[]>(); // These will hold the min max values

		// For each input set min and max values to infinity and - infinity
		for (int i = 0; i < size - 1; i++) {

			double[] minMaxValuePerInput = { Double.MAX_VALUE, Double.MIN_VALUE };
			minMaxValues.add(minMaxValuePerInput);
		}

		for (int place = 0; place < inputs.size(); place++) { // For each training input

			ArrayList<Double> item = inputs.get(place); // Gets the data

			ArrayList<Double> itemsToAdd = new ArrayList<Double>();

			for (int i = 0; i < item.size() - 1; i++) { // Normalise each data except the final one which is the output

				double min = minMaxValues.get(i)[0]; // Get the min max values
				double max = minMaxValues.get(i)[1];

				double input = item.get(i); // Gets the data to normalise

				if (normalised.size() == 0) { // If it is the first data to be normalised

					double secondInput = inputs.get(1).get(i); // Gets the second input

					int index = 2; // Sets the index to 2
					while (secondInput == input && index < item.size()) { // Until we find data which is different to
																			// the first data

						secondInput = inputs.get(1).get(i); // Get the next data item
						index++; // Increment index
					}

					if (input < secondInput) { // If the input is smaller than the secondInput set the min and max
												// values correctly
						min = input;
						max = secondInput;
					} else if (input > secondInput) { // If the input is larger than the second Input set the min and
														// max values correctly
						max = secondInput;
						min = input;
					} else { // If the min and max values are the same set min to 0 and max to the input
						min = 0;
						max = input;
					}

					double[] minMax = minMaxValues.get(i);

					minMax[0] = min; // Sets the min and max values
					minMax[1] = max;

					minMaxValues.remove(i);
					minMaxValues.add(i, minMax);

				}

				// Renormalise
				else if (input > max || input < min) { // If the input is larger than the current max or if the input is
														// smaller than the current min

					ArrayList<Double> inputsToRenormalise = new ArrayList<Double>(); // Sets these 3 arraylists
					ArrayList<Double> itemsRenormalised = new ArrayList<Double>();

					for (int j = 0; j < place; j++) { // For each item upto the current item add to the
														// inputToRenormalise

						ArrayList<Double> itemInList = inputs.get(j);
						double inputToRenormalise = itemInList.get(i);
						inputsToRenormalise.add(inputToRenormalise);
					}

					// RENORMALISE
					if (input > max) { // If the input is larger than the max
						double oldMax = max; // the current max
						max = input;

						for (double unnormalisedInput : inputsToRenormalise) { // Renormalise each input and add to
																				// itemsRenormalised
							double oldValue = min + (oldMax - min) * unnormalisedInput;
							double normalised = (oldValue - min) / (max - min);
							itemsRenormalised.add(normalised);
						}

						// Reset the minMax value
						double[] minMax = minMaxValues.get(i);
						minMax[1] = max;

						minMaxValues.remove(i);
						minMaxValues.add(i, minMax);

					} else if (input < min) { // If the input is smaller
						double oldMin = min; // the current min
						min = input;

						for (double unnormalisedInput : inputsToRenormalise) { // Renormalise each input and add to
																				// itemsRenormalised
							double oldValue = oldMin + (max - oldMin) * unnormalisedInput;
							double normalised = (oldValue - min) / (max - min);
							itemsRenormalised.add(normalised);
						}
						// Reset the minMax value
						double[] minMax = minMaxValues.get(i);
						minMax[0] = min;

						minMaxValues.remove(i);
						minMaxValues.add(i, minMax);

					}

					for (int placeInNormalised = 0; placeInNormalised < place; placeInNormalised++) { // Adds the
																										// normalised
																										// inputs back
																										// to the
																										// correct
																										// positions

						ArrayList<Double> listToChange = normalised.remove(placeInNormalised);
						listToChange.remove(i);
						double normalisedAgain = itemsRenormalised.get(placeInNormalised);
						listToChange.add(i, normalisedAgain);
						normalised.add(placeInNormalised, listToChange);
					}
				}

				double normalisedInput = (input - min) / (max - min); // Normalises the input
				itemsToAdd.add(normalisedInput);
			}
			itemsToAdd.add(item.get(item.size() - 1));
			normalised.add(itemsToAdd);
		}
	}

	/**
	 * This will work out the correct class or numerical output.
	 * 
	 * @param input        The input data which you have
	 * @param kNeighbours  The number of neighbours used
	 * @param trainingData The training data
	 * @return Either the class which the input is in or the average number. Will
	 *         return null if no majority
	 */
	abstract public Double knn(ArrayList<Double> input, int kNeighbours, ArrayList<ArrayList<Double>> trainingData); // NEED
																														// TO
																														// ADD
																														// input
																														// to
																														// end
																														// (ALSO
																														// ADD
																														// ONE
																														// FIGURE
																														// AT
																														// END)

	/**
	 * This will add the inputs to the normalised list
	 */
	protected void addingToList() { // ASSUMENED NORMALISED AND INPUTS HAVE THE INPUT IN AS WELL

		D = new ArrayList<double[]>(inputs.size() - 1);
		normalise(); // Normalise data

		int sizeOfList = normalised.size(); // Finds size of normalised data
		ArrayList<Double> input = normalised.remove(sizeOfList - 1); // Removes the input

		for (ArrayList<Double> item : normalised) { // For each item

			// Performs Pythag to that point with every other point in the normalised list
			double distance = 0;
			for (int i = 0; i < item.size() - 1; i++) {
				double inputMetric = input.get(i);
				double trainingDataInput = item.get(i);

				double difference = inputMetric - trainingDataInput;
				double squaredDifference = Math.pow(difference, 2.0);

				distance += squaredDifference;
			}
			double euclideanDistance = Math.sqrt(distance);

			double[] neighbourAndClass = { euclideanDistance, item.get(item.size() - 1) };

			if (D.size() == 0) { // If no items add at start
				D.add(neighbourAndClass);
			} else { // Adds in the correct position
				int index = binarySearch(euclideanDistance);
				D.add(index, neighbourAndClass);
			}
		}
	}

	/**
	 * Does a binary search on the data to work out where the data should be added
	 * 
	 * @param distance The number which should be added
	 * @return The index where the data should be added
	 */
	private int binarySearch(double distance) {

		int left = 0; // The left most index
		int right = D.size() - 1; // The right most index
		int mid; // The mid index

		if (right == 0) { // If list has 1 item get correct index
			if (distance < D.get(0)[0]) {
				mid = 0;
			} else {
				mid = 1;
			}
		} else {
			mid = -1;
		}
		while (left < right) { // While left is smaller than right

			mid = (left + right) / 2; // Find mid

			if (distance > D.get(mid)[0]) { // If the distance is larger than the mid
				left = mid + 1; // Move left up
			} else {
				right = mid; // Move right down
			}
		}

		return mid;
	}
}
