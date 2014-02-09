package pacman.utils;

import java.io.Serializable;
import java.util.Arrays;

public class Vector implements Serializable {

	private static final long serialVersionUID = 1L;
	private double[] values;
	
	/**
	 * create a 0 vector with the given dimension
	 */
	public Vector(int dimension) {
		values = new double[dimension];
	}
	
	/**
	 * create a vector with the given dimension and initialized with s for every entry
	 */
	public Vector(int dimension, double s) {
		values = new double[dimension];
		for (int i = 0; i < dimension; i++) {
			values[i] = s;
		}
	}
	
	/**
	 * create a vector initialized with the given values
	 */
	public Vector(double[] values) {
		this.values = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			this.values[i] = values[i];
		}
	}
	
	/**
	 * get the values contained in this vector as an array
	 */
	public double[] getValues() {
		return Arrays.copyOf(values, values.length);
	}
	
	/**
	 * get a random vector of dimension n containing random values between min and max
	 */
	public static Vector getRandomVector(int n, double min, double max) {
		double[] values = new double[n];
		for (int i = 0; i < values.length; i++) {
			values[i] = Math.random() * (max - min) + min;
		}
		
		return new Vector(values);
	}

	/**
	 * get the dimension
	 */
	public int getDimension() {
		return values.length;
	}
	
	/**
	 * get the value at position i
	 */
	public double getAt(int i) {
		return values[i];
	}
	
	/**
	 * get the value at position i
	 */
	public void setAt(int i, double v) {
		values[i] = v;
	}
	
	/**
	 * return a copy of this vector
	 */
	public Vector copy() {
		return new Vector(values);
	}
	
	/**
	 * get the dot product between this vector and vector x
	 */
	public double dot(Vector x) {
		if (x == null || this.values.length != x.values.length)
			throw new IllegalArgumentException();
		
		double dot = 0;
		for (int i = 0; i < values.length; i++) {
			dot += values[i] * x.values[i];
		}
		
		return dot;
	}
	
	/**
	 * add vector x to this vector
	 */
	public Vector add(Vector x) {
		if (x == null || this.values.length != x.values.length)
			throw new IllegalArgumentException();
		
		for (int i = 0; i < values.length; i++) {
			values[i] += x.values[i];
		}
		
		return this;
	}
	
	/**
	 * add the scalar s to every value of this vector
	 */
	public Vector add(Double s) {
		for (int i = 0; i < values.length; i++) {
			values[i] += s;
		}
		
		return this;
	}
	
	/**
	 * get the mean of all values
	 */
	public double getMean() {
		double mean = 0;
		for (double value : values) {
			mean += value;
		}
		
		mean /= values.length;
		return mean;
	}
	
	/**
	 * normalize this vector to unit length
	 */
	public Vector normalize() {
		scale(1 / getLength());
		return this;
	}
	
	/**
	 * get the length of this vector
	 */
	public double getLength() {
		double length = 0;
		for (int i = 0; i < values.length; i++) {
			length += values[i] * values[i];
		}
		
		return Math.sqrt(length);
	}
	
	/**
	 * scale every entry of this vector with the scalar s
	 */
	public Vector scale(double s) {
		for (int i = 0; i < values.length; i++) {
			values[i] *= s;
		}
		
		return this;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(values);
	}
}
