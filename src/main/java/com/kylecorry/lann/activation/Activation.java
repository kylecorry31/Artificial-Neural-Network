package com.kylecorry.lann.activation;

public interface Activation {
	/**
	 * The activation function.
	 * 
	 * @param x
	 *            The input to function.
	 * @return The result of applying the function to the input.
	 */
	public double activate(double x);

	/**
	 * The derivative of the activation function.
	 * 
	 * @param x
	 *            The input to the function.
	 * @return The result of applying the function to the input.
	 */
	public double derivative(double x);
}
