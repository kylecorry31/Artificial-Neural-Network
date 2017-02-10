package com.kylecorry.lann.activation;

public class LeakyReLU implements Activation {

	@Override
	public double activate(double x) {
		return x > 0 ? x : 0.01 * x;
	}

	@Override
	public double derivative(double x) {
		return x > 0 ? 1 : 0.01;
	}

}
