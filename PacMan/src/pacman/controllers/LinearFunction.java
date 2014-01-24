package pacman.controllers;

import java.util.Arrays;

public class LinearFunction {

	private float[] coefficients;
	
	public LinearFunction(float[] coefficients) {
		this.coefficients = Arrays.copyOf(coefficients, coefficients.length);
	}
	
	public float getOutput(float[] input) {
		if (input.length != coefficients.length)
			throw new IllegalArgumentException();
		
		float result = 0;
		for (int i = 0; i < coefficients.length; i++) {
			result += input[i] * coefficients[i];
		}
			
		return result;
	}
	
	public void setCoefficients(float[] coefficients) {
		if (coefficients.length != this.coefficients.length)
			throw new IllegalArgumentException();
		
		this.coefficients = Arrays.copyOf(coefficients, coefficients.length);
	}
	
	public float[] getCoefficients() {
		return Arrays.copyOf(coefficients, coefficients.length);
	}
	
}
