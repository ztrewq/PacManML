package pacman.controllers;

import pacman.utils.Vector;

public class StateValuePair {
	
	private final Vector state;
	private final double value;
	
	public StateValuePair(Vector state, double value) {
		this.state = state;
		this.value = value;
	}
	
	public Vector getState() {
		return state;
	}
	
	public double getValue() {
		return value;
	}
}
