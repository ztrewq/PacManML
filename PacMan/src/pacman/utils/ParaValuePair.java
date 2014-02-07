package pacman.utils;

import java.io.Serializable;

public class ParaValuePair implements Serializable{
	private float value;
	private float[] parameter;
	public ParaValuePair(float[] par, float val){
		this.value = val;
		this.parameter = par;
	}
	public float getValue() {
		return value;
	}
	public void setValue(float value) {
		this.value = value;
	}
	public float[] getParameter() {
		return parameter;
	}
	public void setParameter(float[] parameter) {
		this.parameter = parameter;
	}
}
