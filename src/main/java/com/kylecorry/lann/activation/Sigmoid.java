package com.kylecorry.lann.activation;

public class Sigmoid implements Activation {

	@Override
	public double activate(double x) {
		return 1d / (1 + Math.pow(Math.E, -x));
	}

	@Override
	public double derivative(double x) {
		return activate(x) * (1 - activate(x));
	}

}
