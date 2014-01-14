package pacman.gradient;
public class Gradient {

	private float[] values = new float[1];
	public Gradient(int size){
		values = new float[size];
	}
	
	public void change(int i, float value){
		values[i] = value;
	}

	public void print() {
		for(int i = 0;  i < values.length; i++){
			System.out.println(values[i]);
		}
	}
}
