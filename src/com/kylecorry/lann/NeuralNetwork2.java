package com.kylecorry.lann;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import com.kylecorry.lann.activation.Activation;
import com.kylecorry.lann.activation.Softmax;
import com.kylecorry.matrix.Matrix;
import com.kylecorry.matrix.Matrix2;

public class NeuralNetwork2 {

	private ArrayList<Layer> layers;

	/**
	 * A representation of a Feed-Forward neural network.
	 */
	NeuralNetwork2() {
		layers = new ArrayList<Layer>();
	}

	/**
	 * Add a layer to the neural network.
	 * 
	 * @param l
	 *            The layer to add to the neural network.
	 */
	private void addLayer(Layer l) {
		if (layers.size() == 0 || layers.get(layers.size() - 1).getSize()[1] == l.getSize()[0]) {
			layers.add(l);
		} else {
			System.err.println("Layer input did not match the output of the last layer.");
			System.exit(1);
		}
	}

	/**
	 * Calculate the cross entropy error of the neural network.
	 * 
	 * @param y
	 *            The output of the neural network.
	 * @param y_
	 *            The actual expected output.
	 * @return The cross entropy error.
	 */
	public double crossEntropyError(double[] y, double[] y_) {
		double sum = 0;
		for (int i = 0; i < y.length; i++) {
			sum += y_[i] * Math.log(y[i]);
		}
		return -sum;
	}

	/**
	 * Give a prediction based on some input.
	 * 
	 * @param input
	 *            The input to the neural network which is equal in size to the
	 *            number of input neurons.
	 * @return The output of the neural network.
	 */
	public double[] predict(double[] input) {
		if (input.length != layers.get(0).getSize()[0]) {
			System.err.println("Input size did not match the input size of the first layer.");
			System.exit(1);
		}
		double[][] modInput = Matrix.transpose(new double[][] { input });
		for (Layer l : layers) {
			modInput = l.activate(modInput);
		}
		return Matrix.transpose(modInput)[0];
	}

	/**
	 * Get the position of the most probable in an output array.
	 * 
	 * @param output
	 *            The output of the neural network (using Softmax)
	 * @return The position of the most probable class.
	 */
	public static int argMax(double[] output) {
		int pos = 0;
		double max = output[0];
		for (int i = 1; i < output.length; i++) {
			if (max < output[i]) {
				max = output[i];
				pos = i;
			}
		}
		return pos;
	}

	/**
	 * Train the neural network to predict an output given some input.
	 * 
	 * @param input
	 *            The input to the neural network.
	 * @param output
	 *            The target output for the given input.
	 * @param learningRate
	 *            The rate at which the neural network learns. This is normally
	 *            0.01.
	 * @return The error of the network as an mean cross entropy.
	 */
	public double train(double[][] input, double[][] output, double learningRate) {
		double totalError = 0;
		if (input.length == output.length) {
			for (int i = 0; i < input.length; i++) {
				double[] netOutput = this.predict(input[i]);
				double error = this.crossEntropyError(netOutput, output[i]);
				double[][] deltas = Matrix
						.transpose(Matrix.matSubt(new double[][] { output[i] }, new double[][] { netOutput }));
				double[][] derivs = new double[netOutput.length][1];
				for (int d = 0; d < netOutput.length; d++) {
					if (layers.get(layers.size() - 1).function.getClass().equals(Softmax.class)) {
						double sum = 0;
						for (int o = 0; o < netOutput.length; o++) {
							sum += Math.pow(Math.E, netOutput[o]);
						}
						derivs[d][0] = Math.pow(Math.E, netOutput[d]) / sum;
						derivs[d][0] = layers.get(layers.size() - 1).function.activate(derivs[d][0]) - netOutput[d];
					} else {
						derivs[d][0] = layers.get(layers.size() - 1).function.derivative(netOutput[d]);
					}
				}
				layers.get(layers.size() - 1).gradients = Matrix.matElementMult(deltas, derivs);
				for (int l = layers.size() - 2; l >= 0; l--) {
					double[][] deltas2 = Matrix.matMult(Matrix.transpose(layers.get(l + 1).weights),
							layers.get(l + 1).gradients);
					double[][] derivs2 = new double[layers.get(l).getSize()[1]][1];
					for (int d = 0; d < layers.get(l).getSize()[1]; d++) {
						derivs2[d][0] = layers.get(l).function.derivative(layers.get(l).output[d]);
					}
					layers.get(l).gradients = Matrix.matElementMult(deltas2, derivs2);

				}

				for (int l = layers.size() - 1; l > 0; l--) {
					double[][] oldDeltaWeights = layers.get(l).deltaWeights;
					double[][] newDeltaWeights = Matrix.matAdd(
							Matrix.scalarMult(Matrix.matMult(layers.get(l).gradients,
									new double[][] { layers.get(l - 1).output }), learningRate),
							Matrix.scalarMult(oldDeltaWeights, 0.7));
					layers.get(l).deltaWeights = newDeltaWeights;
					layers.get(l).weights = Matrix.matAdd(layers.get(l).weights, newDeltaWeights);

				}
				totalError += error;
			}
		}
		return totalError / input.length;
	}

	/**
	 * Saves the neural network to a file (CSV).
	 * 
	 * @param filename
	 *            The filename in which to save the weights to.
	 */
	public void save(String filename) {
		PrintWriter printWriter;
		try {
			printWriter = new PrintWriter(filename, "UTF-8");
			for (Layer l : layers) {
				for (int i = 0; i < l.weights.length; i++) {
					printWriter.print(Arrays.toString(l.weights[i]));
					if (i != l.weights.length - 1)
						printWriter.print(",");
				}
				printWriter.println();
			}
			printWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads a neural network from a file.
	 * 
	 * @param filename
	 *            The name of the file to retrieve the weights from.
	 */
	public void load(String filename) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));

			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			br.close();
			String everything = sb.toString();
			String[] strLayers = everything.split("\n");

			for (int i = 0; i < strLayers.length; i++) {
				String[] rows = strLayers[i].split("\\],\\[");
				for (int r = 0; r < rows.length; r++) {
					String[] cols = rows[r].replace("[", "").replace("]", "").split(", ");
					for (int c = 0; c < cols.length; c++) {
						layers.get(i).weights[r][c] = Double.parseDouble(cols[c]);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builder for creating neural network instances.
	 */
	public static class Builder {
		private NeuralNetwork2 net;

		public Builder() {
			net = new NeuralNetwork2();
		}

		/**
		 * Adds a layer to the neural network.
		 * 
		 * @param size
		 *            The size of the layer in this format: [input, output]
		 * @param function
		 *            The activation function of the layer.
		 */
		public NeuralNetwork2.Builder addLayer(int[] size, Activation function) {
			Layer l = new Layer(size, function);
			net.addLayer(l);
			return this;
		}

		/**
		 * Builds a neural network instance.
		 */
		public NeuralNetwork2 build() {
			return net;
		}

	}

	class LayerSize {
		private int input, output;

		public LayerSize(int input, int output) {
			this.input = input;
			this.output = output;
		}

		public int getInputSize() {
			return input;
		}

		public int getOutputSize() {
			return output;
		}
	}

	static class Layer {
		private Matrix2 weightMatrix, dWeightsMatrix, biasMatrix, gradientsMatrix, outputMatrix;
		private double[][] weights, deltaWeights;
		private Activation function;
		private double[][] bias, gradients;
		private int[] size;
		private LayerSize layerSize;
		private double[] output;

		/**
		 * Represents a layer in a neural network.
		 * 
		 * @param size
		 *            The size of the layer in this format: [input, output]
		 * @param function
		 *            The activation function for the neurons in this layer.
		 */
		public Layer(int[] size, Activation fn) {
			weights = new double[size[1]][size[0]];
			bias = new double[size[1]][1];
			gradients = new double[size[1]][1];
			output = new double[size[1]];
			deltaWeights = new double[size[1]][size[0]];
			function = fn;
			this.size = size;
			initializeWeights();
			initializeBias();
		}

		public Layer(LayerSize size, Activation fn) {
			weightMatrix = new Matrix2(size.getOutputSize(), size.getInputSize());
			biasMatrix = new Matrix2(size.getOutputSize(), 1);
			gradientsMatrix = new Matrix2(size.getOutputSize(), 1);
			outputMatrix = new Matrix2(size.getOutputSize(), 1);
			dWeightsMatrix = new Matrix2(size.getOutputSize(), size.getInputSize());
		}

		/**
		 * Initializes the layer's weights to random double values between 0 and
		 * 1.
		 */
		private void initializeWeights() {
			for (int i = 0; i < weights.length; i++) {
				for (int j = 0; j < weights[i].length; j++) {
					weights[i][j] = Math.random();
				}
			}
		}

		/**
		 * Initializes the layer's bias neurons to random double values between
		 * 0 and 1.
		 */
		private void initializeBias() {
			for (int i = 0; i < bias.length; i++) {
				for (int j = 0; j < bias[i].length; j++) {
					bias[i][j] = 0.1;
				}
			}
		}

		/**
		 * Processes the input to the layer.
		 * 
		 * @param input
		 *            The input to the layer.
		 * @return The output of the layer.
		 */
		public double[][] activate(double[][] input) {
			double[][] y = applyFunction(Matrix.matAdd(Matrix.matMult(weights, input), bias));
			output = Matrix.transpose(y)[0];
			return y;
		}

		/**
		 * Applies the activation function to the processed input.
		 * 
		 * @param input
		 *            The input to the activation function.
		 * @return The output of the activation function.
		 */
		private double[][] applyFunction(double[][] input) {
			double[][] activated = input.clone();
			double sum = 0;
			for (int i = 0; i < input.length; i++) {
				for (int j = 0; j < input[i].length; j++) {
					activated[i][j] = function.activate(input[i][j]);
					if (function.getClass().equals(Softmax.class)) {
						sum += activated[i][j];
					}
				}
			}
			if (function.getClass().equals(Softmax.class)) {
				for (int i = 0; i < input.length; i++) {
					for (int j = 0; j < input[i].length; j++) {
						activated[i][j] /= sum;
					}
				}
			}
			return activated;
		}

		/**
		 * Get the input and output size of the layer.
		 * 
		 * @return An int array in this format: [input, output] representing the
		 *         size of the layer.
		 */
		public int[] getSize() {
			return size;
		}

	}

}
