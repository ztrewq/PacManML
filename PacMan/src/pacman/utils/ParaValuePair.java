package pacman.utils;

import java.io.Serializable;

public class ParaValuePair implements Serializable{

	private static final long serialVersionUID = 1L;
	private float value;
	private Vector parameter;
	public ParaValuePair(Vector par, float val){
		this.value = val;
		this.parameter = par;
	}
	public float getValue() {
		return value;
	}
	public void setValue(float value) {
		this.value = value;
	}
	public Vector getParameter() {
		return parameter;
	}
	public void setParameter(Vector parameter) {
		this.parameter = parameter;
	}
}
