package com.kylecorry.kynet;

public class Linear extends Activation {

	@Override
	public double activate(double x) {

		return x;
	}

	@Override
	public double derivative(double x) {
		// TODO Auto-generated method stub
		return 1;
	}

}
