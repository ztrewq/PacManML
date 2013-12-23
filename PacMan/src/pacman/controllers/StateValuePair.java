package pacman.controllers;

public class StateValuePair {
	
	private final float[] state;
	private final float value;
	
	public StateValuePair(float[] state, float value) {
		this.state = state;
		this.value = value;
	}
	
	public float[] getState() {
		return state;
	}
	
	public float getValue() {
		return value;
	}
}
